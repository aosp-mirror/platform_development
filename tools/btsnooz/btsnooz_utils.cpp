/******************************************************************************
 *
 *  Copyright (C) 2015 Google, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

#include <iostream>
#include <iomanip>
#include <fstream>
#include <sstream>
#include <string>
#include <string.h> // for memcpy
#include <vector>
#include <resolv.h>
#include <zlib.h>

extern "C" {
#include "btif/include/btif_debug_btsnoop.h"
#include "hci/include/bt_hci_bdroid.h"
#include "stack/include/bt_types.h"
#include "stack/include/hcidefs.h"
}

// Epoch in microseconds since 01/01/0000.
#define BTSNOOP_EPOCH_DELTA 0x00dcddb30f2f8000ULL

#define INITIAL_BUFFER_SIZE 131072
#define INFLATE_BUFFER  16384

#define LOG_PREFIX  "--- BEGIN:BTSNOOP_LOG_SUMMARY"
#define LOG_POSTFIX  "--- END:BTSNOOP_LOG_SUMMARY"

#define H4_DIRECTION_SENT  0
#define H4_DIRECTION_RECEIVED  1

static uint8_t packetTypeToFlags(const uint8_t type) {
  switch (type << 8) {
    case MSG_HC_TO_STACK_HCI_ERR:
    case MSG_HC_TO_STACK_HCI_ACL:
    case MSG_HC_TO_STACK_HCI_SCO:
    case MSG_HC_TO_STACK_HCI_EVT:
    case MSG_HC_TO_STACK_L2C_SEG_XMIT:
      return H4_DIRECTION_RECEIVED;

    case MSG_STACK_TO_HC_HCI_ACL:
    case MSG_STACK_TO_HC_HCI_SCO:
    case MSG_STACK_TO_HC_HCI_CMD:
      return H4_DIRECTION_SENT;

    default:
      break;
  }
  return 0;
}

static uint8_t packetTypeToHciType(const uint8_t type) {
  switch (type << 8 & 0xFF00) {
    case MSG_STACK_TO_HC_HCI_CMD:
      return HCIT_TYPE_COMMAND;

    case MSG_HC_TO_STACK_HCI_EVT:
      return HCIT_TYPE_EVENT;

    case MSG_STACK_TO_HC_HCI_ACL:
    case MSG_HC_TO_STACK_HCI_ACL:
      return HCIT_TYPE_ACL_DATA;

    case MSG_STACK_TO_HC_HCI_SCO:
    case MSG_HC_TO_STACK_HCI_SCO:
      return HCIT_TYPE_SCO_DATA;

    default:
      break;
  }
  return 0;
}

size_t writeBtSnoop(std::ostream &out, std::vector<uint8_t> &in) {
  if (in.size() < sizeof(btsnooz_preamble_t))
    return 0;

  // Get preamble

  uint8_t *p = in.data();
  btsnooz_preamble_t *preamble = reinterpret_cast<btsnooz_preamble_t*>(p);
  if (preamble->version != BTSNOOZ_CURRENT_VERSION)
    return 0;

  // Write header

  const uint8_t header[] = {
    0x62, 0x74, 0x73, 0x6e, 0x6f, 0x6f, 0x70, 0x00,
    0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x03, 0xea
  };

  out.write(reinterpret_cast<const char*>(header), sizeof(header));

  // Calculate first timestamp

  uint64_t first_ts = preamble->last_timestamp_ms + BTSNOOP_EPOCH_DELTA;
  size_t left = in.size() - sizeof(btsnooz_preamble_t);
  p = in.data() + sizeof(btsnooz_preamble_t);

  while (left > sizeof(btsnooz_header_t)) {
    btsnooz_header_t *p_hdr = reinterpret_cast<btsnooz_header_t*>(p);
    p += sizeof(btsnooz_header_t) + (p_hdr->length - 1);
    left -= sizeof(btsnooz_header_t) + (p_hdr->length - 1);

    first_ts -= p_hdr->delta_time_ms;
  }

  // Process packets

  size_t packets = 0;
  left = in.size() - sizeof(btsnooz_preamble_t);
  p = in.data() + sizeof(btsnooz_preamble_t);

  while (left > sizeof(btsnooz_header_t)) {
    btsnooz_header_t *p_hdr = reinterpret_cast<btsnooz_header_t*>(p);
    p += sizeof(btsnooz_header_t);
    left -= sizeof(btsnooz_header_t);

    const uint32_t h_length = htonl(p_hdr->length);
    out.write(reinterpret_cast<const char*>(&h_length), 4);
    out.write(reinterpret_cast<const char*>(&h_length), 4);

    const uint32_t h_flags = htonl(packetTypeToFlags(p_hdr->type));
    out.write(reinterpret_cast<const char*>(&h_flags), 4);

    const uint32_t h_dropped = 0;
    out.write(reinterpret_cast<const char*>(&h_dropped), 4);

    first_ts += p_hdr->delta_time_ms;
    const uint32_t h_time_hi = htonl(first_ts >> 32);
    const uint32_t h_time_lo = htonl(first_ts & 0xFFFFFFFF);
    out.write(reinterpret_cast<const char*>(&h_time_hi), 4);
    out.write(reinterpret_cast<const char*>(&h_time_lo), 4);

    const uint8_t type = packetTypeToHciType(p_hdr->type);
    out.write(reinterpret_cast<const char*>(&type), 1);

    out.write(reinterpret_cast<const char*>(p), p_hdr->length - 1);

    p += p_hdr->length - 1;
    left -= p_hdr->length - 1;

    ++packets;
  }

  return packets;
}

int readLog(std::istream &in, std::vector<char> &buffer) {
  buffer.reserve(INITIAL_BUFFER_SIZE);

  std::string line;

  const std::string log_prefix(LOG_PREFIX);
  const std::string log_postfix(LOG_POSTFIX);

  bool in_block = false;

  while (std::getline(in, line)) {
    // Ensure line endings aren't wonky...

    if (!line.empty() && line[line.size() - 1] == '\r')
      line.erase(line.end() - 1);

    // Detect block

    if (!in_block) {
      if (line.compare(0, log_prefix.length(), log_prefix) == 0)
        in_block = true;
      continue;
    }

    if (line.compare(0, log_postfix.length(), log_postfix) == 0)
      break;

    // Process data

    buffer.insert(buffer.end(), line.begin(), line.end());
  }

  if (buffer.size() != 0)
    buffer.push_back(0);

  return buffer.size();
}

int base64Decode(std::vector<char> &buffer) {
  char *p = buffer.data();
  return b64_pton(p, reinterpret_cast<uint8_t*>(p), buffer.size());
}

int inflate(std::vector<char> &in, std::vector<uint8_t> &out) {
  out.reserve(in.size());

  uint8_t buffer[INFLATE_BUFFER];
  z_stream zs;

  int ret = inflateInit(&zs);
  if (Z_OK != ret)
    return -1;

  // Copy preamble as-is

  for (size_t i = 0; i != sizeof(btsnooz_preamble_t); ++i) {
    out.push_back(in[i]);
  }

  // De-compress data

  zs.avail_in = in.size() - sizeof(btsnooz_preamble_t);;
  zs.next_in = reinterpret_cast<uint8_t*>(in.data()) + sizeof(btsnooz_preamble_t);

  do {
    zs.avail_out = INFLATE_BUFFER;
    zs.next_out = buffer;

    ret = inflate(&zs, Z_NO_FLUSH);

    size_t read = INFLATE_BUFFER - zs.avail_out;
    uint8_t *p = buffer;
    while (read--)
      out.push_back(*p++);
  } while (zs.avail_out == 0);

  inflateEnd(&zs);

  return out.size();
}

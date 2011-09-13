/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Contains implementation of classes that encapsulate connection to camera
 * services in the emulator via qemu pipe.
 */

#define LOG_NDEBUG 0
#define LOG_TAG "EmulatedCamera_QemuClient"
#include <cutils/log.h>
#include "emulated_camera.h"
#include "QemuClient.h"

namespace android {

/****************************************************************************
 * Qemu query
 ***************************************************************************/

QemuQuery::QemuQuery()
    : query_(query_prealloc_),
      query_status_(NO_ERROR),
      reply_buffer_(NULL),
      reply_data_(NULL),
      reply_size_(0),
      reply_data_size_(0),
      reply_status_(0)
{
    *query_ = '\0';
}

QemuQuery::QemuQuery(const char* query_string)
    : query_(query_prealloc_),
      query_status_(NO_ERROR),
      reply_buffer_(NULL),
      reply_data_(NULL),
      reply_size_(0),
      reply_data_size_(0),
      reply_status_(0)
{
    query_status_ = QemuQuery::Create(query_string, NULL);
}

QemuQuery::QemuQuery(const char* query_name, const char* query_param)
    : query_(query_prealloc_),
      query_status_(NO_ERROR),
      reply_buffer_(NULL),
      reply_data_(NULL),
      reply_size_(0),
      reply_data_size_(0),
      reply_status_(0)
{
    query_status_ = QemuQuery::Create(query_name, query_param);
}

QemuQuery::~QemuQuery()
{
    QemuQuery::Reset();
}

status_t QemuQuery::Create(const char* name, const char* param)
{
    /* Reset from the previous use. */
    Reset();

    /* Query name cannot be NULL or an empty string. */
    if (name == NULL || *name == '\0') {
        LOGE("%s: NULL or an empty string is passed as query name.",
             __FUNCTION__);
        return EINVAL;
    }

    const size_t name_len = strlen(name);
    const size_t param_len = (param != NULL) ? strlen(param) : 0;
    const size_t required = strlen(name) + (param_len ? (param_len + 2) : 1);

    if (required > sizeof(query_prealloc_)) {
        /* Preallocated buffer was too small. Allocate a bigger query buffer. */
        query_ = new char[required];
        if (query_ == NULL) {
            LOGE("%s: Unable to allocate %d bytes for query buffer",
                 __FUNCTION__, required);
            query_status_ = ENOMEM;
            return ENOMEM;
        }
    }

    /* At this point query_ buffer is big enough for the query. */
    if (param_len) {
        sprintf(query_, "%s %s", name, param);
    } else {
        memcpy(query_, name, name_len + 1);
    }

    return NO_ERROR;
}

status_t QemuQuery::Completed(status_t status)
{
    /* Save query completion status. */
    query_status_ = status;
    if (query_status_ != NO_ERROR) {
        return query_status_;
    }

    /* Make sure reply buffer contains at least 'ok', or 'ko'.
     * Note that 'ok', or 'ko' prefixes are always 3 characters long: in case
     * there are more data in the reply, that data will be separated from 'ok'/'ko'
     * with a ':'. If there is no more data in the reply, the prefix will be
     * zero-terminated, and the terminator will be inculded in the reply. */
    if (reply_buffer_ == NULL || reply_size_ < 3) {
        LOGE("%s: Invalid reply to the query", __FUNCTION__);
        query_status_ = EINVAL;
        return EINVAL;
    }

    /* Lets see the reply status. */
    if (!memcmp(reply_buffer_, "ok", 2)) {
        reply_status_ = 1;
    } else if (!memcmp(reply_buffer_, "ko", 2)) {
        reply_status_ = 0;
    } else {
        LOGE("%s: Invalid query reply: '%s'", __FUNCTION__, reply_buffer_);
        query_status_ = EINVAL;
        return EINVAL;
    }

    /* Lets see if there are reply data that follow. */
    if (reply_size_ > 3) {
        /* There are extra data. Make sure they are separated from the status
         * with a ':' */
        if (reply_buffer_[2] != ':') {
            LOGE("%s: Invalid query reply: '%s'", __FUNCTION__, reply_buffer_);
            query_status_ = EINVAL;
            return EINVAL;
        }
        reply_data_ = reply_buffer_ + 3;
        reply_data_size_ = reply_size_ - 3;
    } else {
        /* Make sure reply buffer containing just 'ok'/'ko' ends with
         * zero-terminator. */
        if (reply_buffer_[2] != '\0') {
            LOGE("%s: Invalid query reply: '%s'", __FUNCTION__, reply_buffer_);
            query_status_ = EINVAL;
            return EINVAL;
        }
    }

    return NO_ERROR;
}

void QemuQuery::Reset()
{
    if (query_ != NULL && query_ != query_prealloc_) {
        delete[] query_;
    }
    query_ = query_prealloc_;
    query_status_ = NO_ERROR;
    if (reply_buffer_ != NULL) {
        free(reply_buffer_);
        reply_buffer_ = NULL;
    }
    reply_data_ = NULL;
    reply_size_ = 0;
    reply_data_size_ = 0;
    reply_status_ = 0;
}

/****************************************************************************
 * Qemu client base
 ***************************************************************************/

/* Camera service name. */
const char QemuClient::camera_service_name_[]   = "camera";

QemuClient::QemuClient()
    : fd_(-1)
{
}

QemuClient::~QemuClient()
{
    if (fd_ >= 0) {
        close(fd_);
    }
}

/****************************************************************************
 * Qemu client API
 ***************************************************************************/

status_t QemuClient::Connect(const char* param)
{
    LOGV("%s: '%s'", __FUNCTION__, param ? param : "");

    /* Make sure that client is not connected already. */
    if (fd_ >= 0) {
        LOGE("%s: Qemu client is already connected", __FUNCTION__);
        return EINVAL;
    }

    /* Select one of the two: 'factory', or 'emulated camera' service */
    if (param == NULL || *param == '\0') {
        /* No parameters: connect to the factory service. */
        char pipe_name[512];
        snprintf(pipe_name, sizeof(pipe_name), "qemud:%s", camera_service_name_);
        fd_ = qemu_pipe_open(pipe_name);
    } else {
        /* One extra char ':' that separates service name and parameters + six
         * characters for 'qemud:'. This is required by qemu pipe protocol. */
        char* connection_str = new char[strlen(camera_service_name_) +
                                        strlen(param) + 8];
        sprintf(connection_str, "qemud:%s:%s", camera_service_name_, param);

        fd_ = qemu_pipe_open(connection_str);
        delete[] connection_str;
    }
    if (fd_ < 0) {
        LOGE("%s: Unable to connect to the camera service '%s': %s",
             __FUNCTION__, param ? param : "Factory", strerror(errno));
        return errno ? errno : EINVAL;
    }

    return NO_ERROR;
}

void QemuClient::Disconnect()
{
    if (fd_ >= 0) {
        close(fd_);
        fd_ = -1;
    }
}

status_t QemuClient::Send(const void* data, size_t data_size)
{
    if (fd_ < 0) {
        LOGE("%s: Qemu client is not connected", __FUNCTION__);
        return EINVAL;
    }

    /* Note that we don't use here qemud_client_send, since with qemu pipes we
     * don't need to provide payload size prior to payload when we're writing to
     * the pipe. So, we can use simple write, and qemu pipe will take care of the
     * rest, calling the receiving end with the number of bytes transferred. */
    const size_t written = qemud_fd_write(fd_, data, data_size);
    if (written == data_size) {
        return NO_ERROR;
    } else {
        LOGE("%s: Error sending data via qemu pipe: %s",
             __FUNCTION__, strerror(errno));
        return errno != NO_ERROR ? errno : EIO;
    }
}

status_t QemuClient::Receive(void** data, size_t* data_size)
{
    *data = NULL;
    *data_size = 0;

    if (fd_ < 0) {
        LOGE("%s: Qemu client is not connected", __FUNCTION__);
        return EINVAL;
    }

    /* The way the service replies to a query, it sends payload size first, and
     * then it sends the payload itself. Note that payload size is sent as a
     * string, containing 8 characters representing a hexadecimal payload size
     * value. Note also, that the string doesn't contain zero-terminator. */
    size_t payload_size;
    char payload_size_str[9];
    int rd_res = qemud_fd_read(fd_, payload_size_str, 8);
    if (rd_res != 8) {
        LOGE("%s: Unable to obtain payload size: %s",
             __FUNCTION__, strerror(errno));
        return errno ? errno : EIO;
    }

    /* Convert payload size. */
    errno = 0;
    payload_size_str[8] = '\0';
    payload_size = strtol(payload_size_str, NULL, 16);
    if (errno) {
        LOGE("%s: Invalid payload size '%s'", __FUNCTION__, payload_size_str);
        return EIO;
    }

    /* Allocate payload data buffer, and read the payload there. */
    *data = malloc(payload_size);
    if (*data == NULL) {
        LOGE("%s: Unable to allocate %d bytes payload buffer",
             __FUNCTION__, payload_size);
        return ENOMEM;
    }
    rd_res = qemud_fd_read(fd_, *data, payload_size);
    if (static_cast<size_t>(rd_res) == payload_size) {
        *data_size = payload_size;
        return NO_ERROR;
    } else {
        LOGE("%s: Read size %d doesnt match expected payload size %d: %s",
             __FUNCTION__, rd_res, payload_size, strerror(errno));
        free(*data);
        *data = NULL;
        return errno ? errno : EIO;
    }
}

status_t QemuClient::Query(QemuQuery* query)
{
    /* Make sure that query has been successfuly constructed. */
    if (query->query_status_ != NO_ERROR) {
        LOGE("%s: Query is invalid", __FUNCTION__);
        return query->query_status_;
    }

    /* Send the query. */
    status_t res = Send(query->query_, strlen(query->query_) + 1);
    if (res == NO_ERROR) {
        /* Read the response. */
        res = Receive(reinterpret_cast<void**>(&query->reply_buffer_),
                      &query->reply_size_);
        if (res != NO_ERROR) {
            LOGE("%s Response to query '%s' has failed: %s",
                 __FUNCTION__, query->query_, strerror(res));
        }
    } else {
        LOGE("%s: Send query '%s' failed: %s",
             __FUNCTION__, query->query_, strerror(res));
    }

    /* Complete the query, and return its completion handling status. */
    return query->Completed(res);
}

/****************************************************************************
 * Qemu client for the 'factory' service.
 ***************************************************************************/

/*
 * Factory service queries.
 */

/* Queries list of cameras connected to the host. */
const char FactoryQemuClient::query_list_[] = "list";

FactoryQemuClient::FactoryQemuClient()
    : QemuClient()
{
}

FactoryQemuClient::~FactoryQemuClient()
{
}

status_t FactoryQemuClient::ListCameras(char** list)
{
    QemuQuery query(query_list_);
    Query(&query);
    if (!query.IsSucceeded()) {
        return query.GetCompletionStatus();
    }

    /* Make sure there is a list returned. */
    if (query.reply_data_size_ == 0) {
        LOGE("%s: No camera list is returned.", __FUNCTION__);
        return EINVAL;
    }

    /* Copy the list over. */
    *list = (char*)malloc(query.reply_data_size_);
    if (*list != NULL) {
        memcpy(*list, query.reply_data_, query.reply_data_size_);
        LOGD("Emulated camera list: %s", *list);
        return NO_ERROR;
    } else {
        LOGE("%s: Unable to allocate %d bytes",
             __FUNCTION__, query.reply_data_size_);
        return ENOMEM;
    }
}

/****************************************************************************
 * Qemu client for an 'emulated camera' service.
 ***************************************************************************/

/*
 * Emulated camera queries
 */

/* Connect to the camera device. */
const char CameraQemuClient::query_connect_[]    = "connect";
/* Disconect from the camera device. */
const char CameraQemuClient::query_disconnect_[] = "disconnect";
/* Start capturing video from the camera device. */
const char CameraQemuClient::query_start_[]      = "start";
/* Stop capturing video from the camera device. */
const char CameraQemuClient::query_stop_[]       = "stop";
/* Get next video frame from the camera device. */
const char CameraQemuClient::query_frame_[]      = "frame";

CameraQemuClient::CameraQemuClient()
    : QemuClient()
{
}

CameraQemuClient::~CameraQemuClient()
{

}

status_t CameraQemuClient::QueryConnect()
{
    QemuQuery query(query_connect_);
    Query(&query);
    const status_t res = query.GetCompletionStatus();
    LOGE_IF(res != NO_ERROR, "%s failed: %s",
            __FUNCTION__, query.reply_data_ ? query.reply_data_ :
                                              "No error message");
    return res;
}

status_t CameraQemuClient::QueryDisconnect()
{
    QemuQuery query(query_disconnect_);
    Query(&query);
    const status_t res = query.GetCompletionStatus();
    LOGE_IF(res != NO_ERROR, "%s failed: %s",
            __FUNCTION__, query.reply_data_ ? query.reply_data_ :
                                              "No error message");
    return res;
}

status_t CameraQemuClient::QueryStart(uint32_t pixel_format,
                                      int width,
                                      int height)
{
    char query_str[256];
    snprintf(query_str, sizeof(query_str), "%s dim=%dx%d pix=%d",
             query_start_, width, height, pixel_format);
    QemuQuery query(query_str);
    Query(&query);
    const status_t res = query.GetCompletionStatus();
    LOGE_IF(res != NO_ERROR, "%s failed: %s",
            __FUNCTION__, query.reply_data_ ? query.reply_data_ :
                                              "No error message");
    return res;
}

status_t CameraQemuClient::QueryStop()
{
    QemuQuery query(query_stop_);
    Query(&query);
    const status_t res = query.GetCompletionStatus();
    LOGE_IF(res != NO_ERROR, "%s failed: %s",
            __FUNCTION__, query.reply_data_ ? query.reply_data_ :
                                              "No error message");
    return res;
}

status_t CameraQemuClient::QueryFrame(void* vframe,
                                      void* pframe,
                                      size_t vframe_size,
                                      size_t pframe_size)
{
    char query_str[256];
    snprintf(query_str, sizeof(query_str), "%s video=%d preview=%d",
             query_frame_, (vframe && vframe_size) ? vframe_size : 0,
                           (pframe && pframe_size) ? pframe_size : 0);
    QemuQuery query(query_str);
    Query(&query);
    const status_t res = query.GetCompletionStatus();
    LOGE_IF(res != NO_ERROR, "%s failed: %s",
            __FUNCTION__, query.reply_data_ ? query.reply_data_ :
                                              "No error message");
    if (res == NO_ERROR) {
        /* Copy requested frames. */
        size_t cur_offset = 0;
        const uint8_t* frame = reinterpret_cast<const uint8_t*>(query.reply_data_);
        /* Video frame is always first. */
        if (vframe != NULL && vframe_size != 0) {
            /* Make sure that video frame is in. */
            if ((query.reply_data_size_ - cur_offset) >= vframe_size) {
                memcpy(vframe, frame, vframe_size);
                cur_offset += vframe_size;
            } else {
                LOGE("%s: Reply (%d bytes) is to small to contain video frame (%d bytes)",
                     __FUNCTION__, query.reply_data_size_ - cur_offset, vframe_size);
                return EINVAL;
            }
        }
        if (pframe != NULL && pframe_size != 0) {
            /* Make sure that preview frame is in. */
            if ((query.reply_data_size_ - cur_offset) >= pframe_size) {
                memcpy(pframe, frame + cur_offset, pframe_size);
                cur_offset += pframe_size;
            } else {
                LOGE("%s: Reply (%d bytes) is to small to contain preview frame (%d bytes)",
                     __FUNCTION__, query.reply_data_size_ - cur_offset, pframe_size);
                return EINVAL;
            }
        }
    }

    return res;
}

}; /* namespace android */

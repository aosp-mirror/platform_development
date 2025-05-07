/*
 * Copyright (C) 2025 The Android Open Source Project
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

import Long from 'long';
import {perfetto} from 'protos/perfetto/trace/static';
import configJson32 from '../../../../configs/services.core.protolog32.json'; // eslint-disable-line no-restricted-imports
import configJson64 from '../../../../configs/services.core.protolog64.json'; // eslint-disable-line no-restricted-imports

interface LegacyConfig {
  groups: {[key: string]: {tag: string}};
  messages: {
    [key: string]: {
      message: string;
      level: string;
      group: string;
      at: string;
    };
  };
}

function makeProtologViewerConfig(
  configJson: LegacyConfig,
): perfetto.protos.ProtoLogViewerConfig {
  const groupNameToId = new Map<string, number>();

  const groups: perfetto.protos.ProtoLogViewerConfig.Group[] = Object.entries(
    configJson.groups,
  ).map(([name, {tag}], index) => {
    const group = perfetto.protos.ProtoLogViewerConfig.Group.fromObject({
      id: index + 1,
      name,
      tag,
    });
    groupNameToId.set(group.name, group.id);
    return group;
  });

  const messages: perfetto.protos.ProtoLogViewerConfig.MessageData[] =
    Object.entries(configJson.messages).map(
      ([id, {message, level, group, at}]) => {
        let protologLevel: perfetto.protos.ProtoLogLevel;
        switch (level) {
          case 'DEBUG':
            protologLevel = perfetto.protos.ProtoLogLevel.PROTOLOG_LEVEL_DEBUG;
            break;
          case 'VERBOSE':
            protologLevel =
              perfetto.protos.ProtoLogLevel.PROTOLOG_LEVEL_VERBOSE;
            break;
          case 'INFO':
            protologLevel = perfetto.protos.ProtoLogLevel.PROTOLOG_LEVEL_INFO;
            break;
          case 'WARN':
            protologLevel = perfetto.protos.ProtoLogLevel.PROTOLOG_LEVEL_WARN;
            break;
          case 'ERROR':
            protologLevel = perfetto.protos.ProtoLogLevel.PROTOLOG_LEVEL_ERROR;
            break;
          case 'WTF':
            protologLevel = perfetto.protos.ProtoLogLevel.PROTOLOG_LEVEL_WTF;
            break;
          default:
            protologLevel =
              perfetto.protos.ProtoLogLevel.PROTOLOG_LEVEL_UNDEFINED;
        }
        return perfetto.protos.ProtoLogViewerConfig.MessageData.fromObject({
          messageId: Long.fromString(id),
          message,
          level: protologLevel,
          groupId: groupNameToId.get(group),
          location: at,
        });
      },
    );
  return perfetto.protos.ProtoLogViewerConfig.fromObject({
    messages,
    groups,
  });
}

export const CONFIG_32 = makeProtologViewerConfig(configJson32);
export const CONFIG_64 = makeProtologViewerConfig(configJson64);

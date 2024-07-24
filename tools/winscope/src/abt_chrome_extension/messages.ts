/*
 * Copyright (C) 2022 The Android Open Source Project
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

// TODO (b/262667224):
//  deduplicate all the type definitions in this file when we move Winscope to google3.
//  The definitions were duplicated from these source files:
//  - google3/wireless/android/tools/android_bug_tool/app/platform/web/interface/command.ts
//  - google3/wireless/android/tools/android_bug_tool/app/platform/web/interface/attachment_metadata.ts
//  - google3/wireless/android/tools/android_bug_tool/app/platform/web/interface/bug_report_metadata.ts

/** Describes the type of message enclosed in a {@code WebCommandMessage}. */
export enum MessageType {
  UNKNOWN,
  OPEN_REQUEST,
  OPEN_BUGANIZER_RESPONSE,
  OPEN_WEB_RESPONSE,
  OPEN_URL_REQUEST,
  OPEN_URL_RESPONSE,
  CHECK_ISSUE_METADATA_REQUEST,
  CHECK_ISSUE_METADATA_RESPONSE,
  OPEN_TOOL_WEB_REQUEST,
}

/** Base of all messages sent between the web and extension. */
export declare interface WebCommandMessage {
  action: MessageType;
}

/** Request from web to background to download the file. */
export interface OpenRequest extends WebCommandMessage {
  action: MessageType.OPEN_REQUEST;
}

/** Response of download the issue's attachment from background. */
export declare interface OpenBuganizerResponse extends WebCommandMessage {
  action: MessageType.OPEN_BUGANIZER_RESPONSE;

  /** issue id */
  issueId: string;

  /** issue title */
  issueTitle: string | undefined;

  /** issue access level */
  issueAccessLevel: IssueAccessLimit | undefined;

  /** Attachment list. */
  attachments: AttachmentMetadata[];
}

/** Attachment metadata. */
export interface AttachmentMetadata {
  bugReportMetadata?: BugReportMetadata;
  author: string;
  name: string;
  objectUrl: string;
  restrictionSeverity: RestrictionSeverity;
  resourceName: string;
  entityStatus: EntityStatus;
  attachmentId: string;
  fileSize: number;
  commentTimestamp?: DateTime;
}

/**
 * Incorporates all of the metadata that can be retrieved from a bugreport
 * file name.
 */
export interface BugReportMetadata {
  uuid?: string;
  hasWinscope: boolean;
  hasTrace: boolean;
  isRedacted: boolean;
  device: string;
  build: string;
  // The date parsed from the bug report filename is only used for
  // grouping common files together.  It is not used for display purposes.
  timestamp: DateTime;
}

/**
 * Defines of the issue access limit. See:
 * http://go/buganizer/concepts/access-control#accesslimit
 */
export enum IssueAccessLimit {
  INTERNAL = '',
  VISIBLE_TO_PARTNERS = 'Visible to Partners',
  VISIBLE_TO_PUBLIC = 'Visible to Public',
}

/**
 * Types of issue content restriction verdicts. See:
 * http://google3/google/devtools/issuetracker/v1/issuetracker.proto?l=1858&rcl=278024740
 */
export enum RestrictionSeverity {
  /** Unspecified restricted content severity */
  RESTRICTION_SEVERITY_UNSPECIFIED = 0,
  /** No restricted content was detected/flagged in the content */
  NONE_DETECTED = 1,
  /** Restricted content was detected/flagged in the content */
  RESTRICTED = 2,
  /** RESTRICTED_PLUS content was detected/flagged in the content */
  RESTRICTED_PLUS = 3,
}

/**
 * Types of entity statuses for issue tracker attachments. See:
 * https:google3/google/devtools/issuetracker/v1/issuetracker.proto;rcl=448855448;l=58
 */
export enum EntityStatus {
  // Default value. Entity exists and is available for use.
  ACTIVE = 0,
  // Entity is invisible except for administrative actions, i.e. undelete.
  DELETED = 1,
  // Entity is irretrievably wiped.
  PURGED = 2,
}

// Actual definition is in google3/third_party/javascript/closure/date/date
export type DateTime = object;

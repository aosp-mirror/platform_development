/**
 * Should be kept in sync with ENUM is in Google3 under:
 * google3/wireless/android/tools/android_bug_tool/extension/common/actions
 */
const WebContentScriptMessageType = {
  UNKNOWN: 0,
  CONVERT_OBJECT_URL: 1,
  CONVERT_OBJECT_URL_RESPONSE: 2,
};

const NAVIGATION_STYLE = {
  GLOBAL: 'Global',
  FOCUSED: 'Focused',
  CUSTOM: 'Custom',
  TARGETED: 'Targeted',
};

export { WebContentScriptMessageType, NAVIGATION_STYLE };

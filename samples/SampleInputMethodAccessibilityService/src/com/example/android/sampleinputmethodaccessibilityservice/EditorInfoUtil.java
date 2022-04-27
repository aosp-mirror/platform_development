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

package com.example.android.sampleinputmethodaccessibilityservice;

import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class EditorInfoUtil {
    /**
     * Not intended to be instantiated.
     */
    private EditorInfoUtil() {
    }

    static String dump(@Nullable EditorInfo editorInfo) {
        if (editorInfo == null) {
            return "null";
        }
        final StringBuilder sb = new StringBuilder();
        dump(sb, editorInfo);
        return sb.toString();
    }

    static void dump(@NonNull StringBuilder sb, @NonNull EditorInfo editorInfo) {
        sb.append("packageName=").append(editorInfo.packageName).append("\n")
                .append("inputType=");
        dumpInputType(sb, editorInfo.inputType);
        sb.append("\n");
        sb.append("imeOptions=");
        dumpImeOptions(sb, editorInfo.imeOptions);
        sb.append("\n");
        sb.append("initialSelection=(").append(editorInfo.initialSelStart)
                .append(",").append(editorInfo.initialSelEnd).append(")");
        sb.append("\n");
        sb.append("initialCapsMode=");
        dumpCapsMode(sb, editorInfo.initialCapsMode);
        sb.append("\n");
    }

    static void dumpInputType(@NonNull StringBuilder sb, int inputType) {
        final int inputClass = inputType & EditorInfo.TYPE_MASK_CLASS;
        final int inputVariation = inputType & EditorInfo.TYPE_MASK_VARIATION;
        final int inputFlags = inputType & EditorInfo.TYPE_MASK_FLAGS;
        switch (inputClass) {
            case EditorInfo.TYPE_NULL:
                sb.append("Null");
                break;
            case EditorInfo.TYPE_CLASS_TEXT: {
                sb.append("Text");
                switch (inputVariation) {
                    case EditorInfo.TYPE_TEXT_VARIATION_NORMAL:
                        break;
                    case EditorInfo.TYPE_TEXT_VARIATION_URI:
                        sb.append(":URI");
                        break;
                    case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS:
                        sb.append(":EMAIL_ADDRESS");
                        break;
                    case EditorInfo.TYPE_TEXT_VARIATION_EMAIL_SUBJECT:
                        sb.append(":EMAIL_SUBJECT");
                        break;
                    case EditorInfo.TYPE_TEXT_VARIATION_SHORT_MESSAGE:
                        sb.append(":SHORT_MESSAGE");
                        break;
                    case EditorInfo.TYPE_TEXT_VARIATION_LONG_MESSAGE:
                        sb.append(":LONG_MESSAGE");
                        break;
                    case EditorInfo.TYPE_TEXT_VARIATION_PERSON_NAME:
                        sb.append(":PERSON_NAME");
                        break;
                    case EditorInfo.TYPE_TEXT_VARIATION_POSTAL_ADDRESS:
                        sb.append(":POSTAL_ADDRESS");
                        break;
                    case EditorInfo.TYPE_TEXT_VARIATION_PASSWORD:
                        sb.append(":PASSWORD");
                        break;
                    case EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
                        sb.append(":VISIBLE_PASSWORD");
                        break;
                    case EditorInfo.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT:
                        sb.append(":WEB_EDIT_TEXT");
                        break;
                    case EditorInfo.TYPE_TEXT_VARIATION_FILTER:
                        sb.append(":FILTER");
                        break;
                    case EditorInfo.TYPE_TEXT_VARIATION_PHONETIC:
                        sb.append(":PHONETIC");
                        break;
                    case EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS:
                        sb.append(":WEB_EMAIL_ADDRESS");
                        break;
                    case EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD:
                        sb.append(":WEB_PASSWORD");
                        break;
                    default:
                        sb.append(":UNKNOWN=").append(inputVariation);
                        break;
                }
                if ((inputFlags & EditorInfo.TYPE_TEXT_FLAG_CAP_CHARACTERS) != 0) {
                    sb.append("|CAP_CHARACTERS");
                }
                if ((inputFlags & EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS) != 0) {
                    sb.append("|CAP_WORDS");
                }
                if ((inputFlags & EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0) {
                    sb.append("|CAP_SENTENCES");
                }
                if ((inputFlags & EditorInfo.TYPE_TEXT_FLAG_AUTO_CORRECT) != 0) {
                    sb.append("|AUTO_CORRECT");
                }
                if ((inputFlags & EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    sb.append("|AUTO_COMPLETE");
                }
                if ((inputFlags & EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE) != 0) {
                    sb.append("|MULTI_LINE");
                }
                if ((inputFlags & EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) {
                    sb.append("|NO_SUGGESTIONS");
                }
                if ((inputFlags & EditorInfo.TYPE_TEXT_FLAG_ENABLE_TEXT_CONVERSION_SUGGESTIONS)
                        != 0) {
                    sb.append("|ENABLE_TEXT_CONVERSION_SUGGESTIONS");
                }
                break;
            }
            case EditorInfo.TYPE_CLASS_NUMBER: {
                sb.append("Number");
                switch (inputVariation) {
                    case EditorInfo.TYPE_NUMBER_VARIATION_NORMAL:
                        sb.append(":NORMAL");
                        break;
                    case EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD:
                        sb.append(":PASSWORD");
                        break;
                    default:
                        sb.append(":UNKNOWN=").append(inputVariation);
                        break;
                }
                if ((inputFlags & EditorInfo.TYPE_NUMBER_FLAG_SIGNED) != 0) {
                    sb.append("|SIGNED");
                }
                if ((inputFlags & EditorInfo.TYPE_NUMBER_FLAG_DECIMAL) != 0) {
                    sb.append("|DECIMAL");
                }
                break;
            }
            case EditorInfo.TYPE_CLASS_PHONE:
                sb.append("Phone");
                break;
            case EditorInfo.TYPE_CLASS_DATETIME: {
                sb.append("DateTime");
                switch (inputVariation) {
                    case EditorInfo.TYPE_DATETIME_VARIATION_NORMAL:
                        sb.append(":NORMAL");
                        break;
                    case EditorInfo.TYPE_DATETIME_VARIATION_DATE:
                        sb.append(":DATE");
                        break;
                    case EditorInfo.TYPE_DATETIME_VARIATION_TIME:
                        sb.append(":TIME");
                        break;
                    default:
                        sb.append(":UNKNOWN=").append(inputVariation);
                        break;
                }
                break;
            }
            default:
                sb.append("UnknownClass=").append(inputClass);
                if (inputVariation != 0) {
                    sb.append(":variation=").append(inputVariation);
                }
                if (inputFlags != 0) {
                    sb.append("|flags=0x").append(Integer.toHexString(inputFlags));
                }
                break;
        }
    }

    static void dumpImeOptions(@NonNull StringBuilder sb, int imeOptions) {
        final int action = imeOptions & EditorInfo.IME_MASK_ACTION;
        final int flags = imeOptions & ~EditorInfo.IME_MASK_ACTION;
        sb.append("Action:");
        switch (action) {
            case EditorInfo.IME_ACTION_UNSPECIFIED:
                sb.append("UNSPECIFIED");
                break;
            case EditorInfo.IME_ACTION_NONE:
                sb.append("NONE");
                break;
            case EditorInfo.IME_ACTION_GO:
                sb.append("GO");
                break;
            case EditorInfo.IME_ACTION_SEARCH:
                sb.append("SEARCH");
                break;
            case EditorInfo.IME_ACTION_SEND:
                sb.append("SEND");
                break;
            case EditorInfo.IME_ACTION_NEXT:
                sb.append("NEXT");
                break;
            case EditorInfo.IME_ACTION_DONE:
                sb.append("DONE");
                break;
            case EditorInfo.IME_ACTION_PREVIOUS:
                sb.append("PREVIOUS");
                break;
            default:
                sb.append("UNKNOWN=").append(action);
                break;
        }
        if ((flags & EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0) {
            sb.append("|NO_PERSONALIZED_LEARNING");
        }
        if ((flags & EditorInfo.IME_FLAG_NO_FULLSCREEN) != 0) {
            sb.append("|NO_FULLSCREEN");
        }
        if ((flags & EditorInfo.IME_FLAG_NAVIGATE_PREVIOUS) != 0) {
            sb.append("|NAVIGATE_PREVIOUS");
        }
        if ((flags & EditorInfo.IME_FLAG_NAVIGATE_NEXT) != 0) {
            sb.append("|NAVIGATE_NEXT");
        }
        if ((flags & EditorInfo.IME_FLAG_NO_EXTRACT_UI) != 0) {
            sb.append("|NO_EXTRACT_UI");
        }
        if ((flags & EditorInfo.IME_FLAG_NO_ACCESSORY_ACTION) != 0) {
            sb.append("|NO_ACCESSORY_ACTION");
        }
        if ((flags & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
            sb.append("|NO_ENTER_ACTION");
        }
        if ((flags & EditorInfo.IME_FLAG_FORCE_ASCII) != 0) {
            sb.append("|FORCE_ASCII");
        }
    }

    static void dumpCapsMode(@NonNull StringBuilder sb, int capsMode) {
        if (capsMode == 0) {
            sb.append("none");
            return;
        }
        boolean addSeparator = false;
        if ((capsMode & EditorInfo.TYPE_TEXT_FLAG_CAP_CHARACTERS) != 0) {
            sb.append("CHARACTERS");
            addSeparator = true;
        }
        if ((capsMode & EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS) != 0) {
            if (addSeparator) {
                sb.append('|');
            }
            sb.append("WORDS");
            addSeparator = true;
        }
        if ((capsMode & EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0) {
            if (addSeparator) {
                sb.append('|');
            }
            sb.append("SENTENCES");
        }
    }
}

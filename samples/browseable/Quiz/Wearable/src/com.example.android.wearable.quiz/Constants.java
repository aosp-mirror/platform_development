/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.wearable.quiz;

/** Constants used in the wearable app. */
public final class Constants {
    private Constants() {
    }

    public static final String ANSWERS = "answers";
    public static final String CHOSEN_ANSWER_CORRECT = "chosen_answer_correct";
    public static final String CORRECT_ANSWER_INDEX = "correct_answer_index";
    public static final String QUESTION = "question";
    public static final String QUESTION_INDEX = "question_index";
    public static final String QUESTION_WAS_ANSWERED = "question_was_answered";
    public static final String QUESTION_WAS_DELETED = "question_was_deleted";

    public static final String NUM_CORRECT = "num_correct";
    public static final String NUM_INCORRECT = "num_incorrect";
    public static final String NUM_SKIPPED = "num_skipped";

    public static final String QUIZ_ENDED_PATH = "/quiz_ended";
    public static final String QUIZ_EXITED_PATH = "/quiz_exited";
    public static final String RESET_QUIZ_PATH = "/reset_quiz";

    public static final int CONNECT_TIMEOUT_MS = 100;
    public static final int GET_CAPABILITIES_TIMEOUT_MS = 5000;
}

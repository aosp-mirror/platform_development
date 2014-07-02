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

package com.example.android.wearable.elizachat;

import android.text.TextUtils;

/**
 * A basic chat bot based on ELIZA, one of the earliest programs to employ primitive natural
 * language processing. Like ELIZA, this bot can simulate a Rogerian psychotherapist using a
 * collection of vague and generic responses.
 */
public class ElizaResponder {

    private static final String[] CONVERSATION_KEYWORDS = {
            "CAN YOU", "CAN I", "YOU ARE", "YOURE", "I DONT", "I FEEL", "WHY DONT YOU", "WHY CANT I",
            "ARE YOU", "I CANT", "I AM", " IM ", "YOU", "I WANT", "WHAT", "HOW", "WHO", "WHERE",
            "WHEN", "WHY", "NAME", "CAUSE", "SORRY", "DREAM", "HELLO", "HI", "MAYBE", "NO", "YOUR",
            "ALWAYS", "THINK", "ALIKE", "YES", "FRIEND", "COMPUTER", "NOKEYFOUND" };

    private static String[] WORDS_TO_REPLACE = {"ARE","AM","WERE","WAS","YOU","I","YOUR","MY",
            "IVE","YOUVE","IM","YOURE", "YOU", "ME"};

    private static String[] QUESTIONS = {
            "DON'T YOU BELIEVE THAT I CAN.", "PERHAPS YOU WOULD LIKE TO BE ABLE TO.",
            "YOU WANT ME TO BE ABLE TO*", "PERHAPS YOU DON'T WANT TO*",
            "DO YOU WANT TO BE ABLE TO*", "WHAT MAKES YOU THINK I AM*",
            "DOES IT PLEASE YOU TO BELIEVE I AM*", "PERHAPS YOU WOULD LIKE TO BE*",
            "DO YOU SOMETIMES WISH YOU WERE*", "DON'T YOU REALLY*", "WHY DON'T YOU*",
            "DO YOU WISH TO BE ABLE TO*", "DOES THAT TROUBLE YOU?",
            "TELL ME MORE ABOUT SUCH FEELINGS*", "DO YOU OFTEN FEEL*",
            "DO YOU ENJOY FEELING*", "DO YOU REALLY BELIEVE I DON'T*",
            "PERHAPS IN TIME I WILL*", "DO YOU WANT ME TO*",
            "DO YOU THINK YOU SHOULD BE ABLE TO*", "WHY CAN'T YOU*",
            "WHAT MAKES YOU WONDER WHETHER OR NOT I AM*",
            "WOULD YOU PREFER IF I WERE NOT*", "PERHAPS IN YOUR FANTASIES I AM*",
            "HOW DO YOU KNOW YOU CAN'T*", "HAVE YOU TRIED?", "PERHAPS YOU CAN NOW*",
            "DID YOU COME TO ME BECAUSE YOU ARE*", "HOW LONG HAVE YOU BEEN*",
            "DO YOU BELIEVE IT IS NORMAL TO BE*", "DO YOU ENJOY BEING*",
            "I AM MORE INTERESTED IN TALKING ABOUT YOU.", "OH, I*",
            "ARE YOU REALLY SO INTERESTED IN SOMEONE LIKE ME?",
            "WHAT WOULD IT MEAN TO YOU IF YOU GOT*", "WHY DO YOU WANT*",
            "SUPPOSE YOU SOON GOT*", "WHAT IF YOU NEVER GOT*", "I SOMETIMES ALSO WANT*",
            "WHY DO YOU ASK?", "DOES THAT QUESTION INTEREST YOU?",
            "WHAT ANSWER WOULD PLEASE YOU THE MOST?", "WHAT DO YOU THINK?",
            "ARE SUCH QUESTIONS ON YOUR MIND OFTEN?",
            "WHAT IS IT THAT YOU REALLY WANT TO KNOW?", "HAVE YOU ASKED ANYONE ELSE?",
            "HAVE YOU ASKED SUCH QUESTIONS BEFORE?",
            "WHAT ELSE COMES TO MIND WHEN YOU ASK THAT?", "WE CAN KEEP THIS ANONYMOUS.",
            "NO NEED TO SPECIFY ANY NAMES-- PLEASE GO ON.", "IS THAT THE REAL REASON?",
            "DON'T ANY OTHER REASONS COME TO MIND?",
            "DOES THAT REASON EXPLAIN ANYTHING ELSE?", "WHAT OTHER REASONS MIGHT THERE BE?",
            "PLEASE DON'T APOLOGIZE.", "APOLOGIES ARE NOT NECESSARY.",
            "WHAT FEELINGS DO YOU HAVE WHEN YOU APOLOGIZE?", "NO NEED TO BE DEFENSIVE!",
            "WHAT DOES THAT DREAM SUGGEST TO YOU?", "DO YOU DREAM OFTEN?",
            "WHAT PERSONS APPEAR IN YOUR DREAMS?", "DO YOU HAVE PLEASANT DREAMS?",
            "HOW DO YOU DO ... PLEASE STATE YOUR PROBLEM.", "YOU DON'T SEEM QUITE CERTAIN.",
            "WHY THE UNCERTAIN TONE?", "LET'S TRY TO KEEP THIS POSITIVE.", "YOU AREN'T SURE?",
            "DON'T YOU KNOW?", "IS THAT A DEFINITE NO OR MIGHT YOU CHANGE YOUR MIND?",
            "I AM SENSING SOME NEGATIVITY.", "WHY NOT?", "ARE YOU SURE?", "WHY NO?",
            "WHY ARE YOU CONCERNED ABOUT MY*", "WHAT ABOUT YOUR OWN*",
            "CAN'T YOU THINK OF A SPECIFIC EXAMPLE?", "WHEN?", "WHAT ARE YOU THINKING OF?",
            "REALLY. ALWAYS?", "DO YOU REALLY THINK SO?", "BUT YOU ARE NOT SURE YOU.",
            "BELIEVE IN YOURSELF.", "IN WHAT WAY?", "WHAT RESEMBLANCE DO YOU SEE?",
            "WHAT DOES THE SIMILARITY SUGGEST TO YOU?",
            "WHAT OTHER CONNECTIONS DO YOU SEE?", "COULD THERE REALLY BE SOME CONNECTION?",
            "HOW?", "YOU SEEM QUITE POSITIVE.", "ARE YOU SURE?", "I SEE.", "I UNDERSTAND.",
            "TELL ME ABOUT YOUR FRIENDS.", "ARE YOU WORRIED ABOUT YOUR FRIENDS?",
            "DO YOUR FRIENDS EVER GIVE YOU A HARD TIME?", "WHAT DO YOU LIKE ABOUT YOUR FRIENDS?",
            "DO YOU LOVE YOUR FRIENDS?", "PERHAPS YOUR LOVE FOR FRIENDS WORRIES YOU.",
            "DO COMPUTERS EXCITE YOU?", "ARE YOU TALKING ABOUT ME IN PARTICULAR?",
            "HOW DO YOU LIKE YOUR WATCH?", "WHY DO YOU MENTION COMPUTERS?",
            "DO YOU FIND MACHINES AS FASCINATING AS I DO?",
            "DON'T YOU THINK COMPUTERS CAN HELP PEOPLE?",
            "WHAT ABOUT MACHINES EXCITES YOU THE MOST?",
            "HEY THERE, HOW CAN I HELP YOU?",
            "WHAT DOES THAT SUGGEST TO YOU?", "I SEE.",
            "I'M NOT SURE I UNDERSTAND YOU FULLY.", "COME COME ELUCIDATE YOUR THOUGHTS.",
            "CAN YOU ELABORATE ON THAT?", "THAT IS QUITE INTERESTING."};

    private static char[] CONVERSATION_TO_RESPONSES_MAP = {
            1,3,4,2,6,4,6,4,10,4,14,3,17,3,20,2,22,3,25,3,
            28,4,28,4,32,3,35,5,40,9,40,9,40,9,40,9,40,9,40,9,
            49,2,51,4,55,4,59,4,63,1,63,1,64,5,69,5,74,2,76,4,
            80,3,83,7,90,3,93,6,99,7,106,6};

    private int[] responseStarts = new int[36];
    private int[] responseCurrentIndices = new int[36];
    private int[] responseEnds = new int[36];
    private String previousInput = null;

    public ElizaResponder() {
        for (int i = 0; i < CONVERSATION_TO_RESPONSES_MAP.length / 2; i++) {
            responseStarts[i] = CONVERSATION_TO_RESPONSES_MAP[2 * i];
            responseCurrentIndices[i] = CONVERSATION_TO_RESPONSES_MAP[2 * i];
            responseEnds[i] = responseStarts[i] + CONVERSATION_TO_RESPONSES_MAP[2 * i  + 1];
        }
    }

    public String elzTalk(String input) {
        if (null == input) {
            input = "";
        }
        String result = "";

        input = " " + input.toUpperCase().replace("\'", "") + " ";

        if (previousInput != null && input.equals(previousInput)) {
            return "DIDN'T YOU JUST SAY THAT?\n";
        }
        previousInput = input;

        int keywordIndex = 0;
        for (; keywordIndex < CONVERSATION_KEYWORDS.length; ++keywordIndex) {
            int index = input.indexOf(CONVERSATION_KEYWORDS[keywordIndex]);
            if (index != -1) {
                break;
            }
        }

        String afterKeyword = "";
        if (keywordIndex == CONVERSATION_KEYWORDS.length) {
            keywordIndex = 35;
        } else {
            int index = input.indexOf(CONVERSATION_KEYWORDS[keywordIndex]);
            afterKeyword = input.substring(index + CONVERSATION_KEYWORDS[keywordIndex].length());
            String[] parts = afterKeyword.split("\\s+");
            for (int i = 0; i < WORDS_TO_REPLACE.length / 2; i++) {
                String first = WORDS_TO_REPLACE[i * 2];
                String second = WORDS_TO_REPLACE[i * 2 + 1];
                for (int j = 0; j < parts.length; ++j) {
                    if (parts[j].equals(first)) {
                        parts[j] = second;
                    } else if (parts[j].equals(second)) {
                        parts[j] = first;
                    }
                }
            }
            afterKeyword = TextUtils.join(" ", parts);
        }

        String question = QUESTIONS[responseCurrentIndices[keywordIndex] - 1];
        responseCurrentIndices[keywordIndex] = responseCurrentIndices[keywordIndex] + 1;
        if (responseCurrentIndices[keywordIndex] > responseEnds[keywordIndex]) {
            responseCurrentIndices[keywordIndex] = responseStarts[keywordIndex];
        }
        result += question;
        if (result.endsWith("*")) {
            result = result.substring(0, result.length() - 1);
            result += " " + afterKeyword;
        }

        return result;
    }
}

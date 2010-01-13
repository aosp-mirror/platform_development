/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.commands.monkey;

/**
 * event source interface
 */
public interface MonkeyEventSource {
    /**
     * @return the next monkey event from the source
     */
    public MonkeyEvent getNextEvent();

    /**
     * set verbose to allow different level of log
     *
     * @param verbose output mode? 1= verbose, 2=very verbose
     */
    public void setVerbose(int verbose);

    /**
     * check whether precondition is satisfied
     *
     * @return false if something fails, e.g. factor failure in random source or
     *         file can not open from script source etc
     */
    public boolean validate();
}

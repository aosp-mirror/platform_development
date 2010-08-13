/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.android.samplesync.client;

import com.example.android.samplesync.client.User;

import junit.framework.TestCase;

import org.json.JSONObject;

public class UserTest extends TestCase {

    @SmallTest
    public void testConstructor() throws Exception {
        User user =
            new User("mjoshi", "Megha", "Joshi", "1-650-335-5681", "1-650-111-5681",
                "1-650-222-5681", "test@google.com", false, 1);
        assertEquals("Megha", user.getFirstName());
        assertEquals("Joshi", user.getLastName());
        assertEquals("mjoshi", user.getUserName());
        assertEquals(1, user.getUserId());
        assertEquals("1-650-335-5681", user.getCellPhone());
        assertEquals(false, user.isDeleted());
    }

    @SmallTest
    public void testValueOf() throws Exception {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("u", "mjoshi");
        jsonObj.put("f", "Megha");
        jsonObj.put("l", "Joshi");
        jsonObj.put("i", 1);
        User user = User.valueOf(jsonObj);
        assertEquals("Megha", user.getFirstName());
        assertEquals("Joshi", user.getLastName());
        assertEquals("mjoshi", user.getUserName());
        assertEquals(1, user.getUserId());
    }
}

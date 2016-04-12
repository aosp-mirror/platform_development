/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.directshare;

import android.content.ComponentName;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides the Direct Share items to the system.
 */
public class SampleChooserTargetService extends ChooserTargetService {

    @Override
    public List<ChooserTarget> onGetChooserTargets(ComponentName targetActivityName,
                                                   IntentFilter matchedFilter) {
        ComponentName componentName = new ComponentName(getPackageName(),
                SendMessageActivity.class.getCanonicalName());
        // The list of Direct Share items. The system will show the items the way they are sorted
        // in this list.
        ArrayList<ChooserTarget> targets = new ArrayList<>();
        for (int i = 0; i < Contact.CONTACTS.length; ++i) {
            Contact contact = Contact.byId(i);
            Bundle extras = new Bundle();
            extras.putInt(Contact.ID, i);
            targets.add(new ChooserTarget(
                    // The name of this target.
                    contact.getName(),
                    // The icon to represent this target.
                    Icon.createWithResource(this, contact.getIcon()),
                    // The ranking score for this target (0.0-1.0); the system will omit items with
                    // low scores when there are too many Direct Share items.
                    0.5f,
                    // The name of the component to be launched if this target is chosen.
                    componentName,
                    // The extra values here will be merged into the Intent when this target is
                    // chosen.
                    extras));
        }
        return targets;
    }

}

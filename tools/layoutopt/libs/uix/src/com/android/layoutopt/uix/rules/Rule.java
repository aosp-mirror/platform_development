/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.layoutopt.uix.rules;

import com.android.layoutopt.uix.LayoutAnalysis;
import org.w3c.dom.Node;

/**
 * Interface that define an analysis rule.
 */
public interface Rule {
    /**
     * Returns the name of the rule.
     *
     * @return A non-null String.
     */
    String getName();

    /**
     * Runs the rule for the specified node. The rule must add any detected
     * issue to the analysis.
     *
     * @param analysis The resulting analysis.
     * @param node The original XML node.
     */
    void run(LayoutAnalysis analysis, Node node);
}

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

import groovy.lang.Script;
import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.xml.dom.DOMCategory;
import com.android.layoutopt.uix.LayoutAnalysis;
import com.android.layoutopt.uix.groovy.LayoutAnalysisCategory;
import org.w3c.dom.Node;
import org.codehaus.groovy.runtime.GroovyCategorySupport;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Implementation of a rule using a Groovy script.
 */
public class GroovyRule implements Rule {
    private final String mName;
    private final Script mScript;
    private final Binding mBinding;
    private final Closure mClosure;
    private final List<Class> mCategories;

    public GroovyRule(String name, Script script) {
        mName = name;
        mScript = script;
        mBinding = new Binding();
        mScript.setBinding(mBinding);
        mClosure = new Closure(this) {
            @Override
            public Object call() {
                return mScript.run();
            }
        };
        mCategories = new ArrayList<Class>();
        Collections.addAll(mCategories, DOMCategory.class, LayoutAnalysisCategory.class);
    }

    public String getName() {
        return mName;
    }

    public void run(LayoutAnalysis analysis, Node node) {
        mBinding.setVariable("analysis", analysis);
        mBinding.setVariable("node", node);

        GroovyCategorySupport.use(mCategories, mClosure);
    }
}

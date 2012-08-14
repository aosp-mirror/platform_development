/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.idegen;

import java.nio.charset.Charset;

/**
 * Constants
 */
public class Constants {

    public static final Charset CHARSET = Charset.forName("UTF-8");

    public static final String REL_TEMPLATE_DIR = "development/tools/idegen/templates";
    public static final String REL_MODULES_TEMPLATE = REL_TEMPLATE_DIR + "/idea/modules.xml";
    public static final String REL_VCS_TEMPLATE = REL_TEMPLATE_DIR + "/idea/vcs.xml";
    public static final String REL_IML_TEMPLATE = REL_TEMPLATE_DIR + "/module-template.iml";

    public static final String REL_OUT_APP_DIR = "out/target/common/obj/APPS";

    public static final String FRAMEWORK_MODULE = "framework";
    public static final String[] AUTO_DEPENDENCIES = new String[]{
            FRAMEWORK_MODULE, "libcore"
    };
    public static final String[] DIRS_WITH_AUTO_DEPENDENCIES = new String[] {
      "packages", "vendor", "frameworks/ex", "frameworks/opt", "frameworks/support"
    };

    // Framework needs a special constant for it's intermediates because it does not follow
    // normal conventions.
    public static final String FRAMEWORK_INTERMEDIATES = "framework-res_intermediates";
}

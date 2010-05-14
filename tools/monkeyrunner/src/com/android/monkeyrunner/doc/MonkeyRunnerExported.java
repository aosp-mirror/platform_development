/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.monkeyrunner.doc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method is a public API to expose to the
 * scripting interface.  Can be used to generate documentation of what
 * methods are exposed and also can be used to enforce visibility of
 * these methods in the scripting environment.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE })
public @interface MonkeyRunnerExported {
    /**
     * A documentation string for this method.
     */
    String doc();

    /**
     * The list of names for the keywords in this method in their proper positional order.
     *
     * For example:
     *
     * @MonkeyRunnerExported(args={"one", "two"})
     * public void foo();
     *
     * would allow calls like this:
     *   foo(one=1, two=2)
     *   foo(1, 2)
     */
    String[] args() default {};

    /**
     * The list of documentation for the arguments.
     */
    String[] argDocs() default {};

    /**
     * The documentation for the return type of this method.
     */
    String returns() default "returns nothing.";
}
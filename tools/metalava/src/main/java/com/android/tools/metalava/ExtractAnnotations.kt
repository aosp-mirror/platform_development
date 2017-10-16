/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.tools.metalava

import com.android.tools.lint.annotations.Extractor
import com.intellij.psi.PsiClassOwner

class ExtractAnnotations {
    fun extractAnnotations(units: List<PsiClassOwner>) {
        val rmTypeDefs = if (options.rmTypeDefs != null) listOf(options.rmTypeDefs) else emptyList()
        val typedefFile = options.typedefFile
        val filter = options.apiFilter

        val verbose = !options.quiet
        val skipClassRetention = options.skipClassRetention
        val extractor = Extractor(filter, rmTypeDefs, verbose, !skipClassRetention, true)
        extractor.isListIgnored = !options.hideFiltered
        extractor.extractFromProjectSource(units)
        for (jar in options.mergeAnnotations) {
            extractor.mergeExisting(jar)
        }

        extractor.export(options.externalAnnotations, null)

        if (typedefFile != null) {
            extractor.writeTypedefFile(typedefFile)
        }

        if (rmTypeDefs.isNotEmpty()) {
            if (typedefFile != null) {
                Extractor.removeTypedefClasses(rmTypeDefs, typedefFile)
            } else {
                extractor.removeTypedefClasses()
            }
        }
    }
}

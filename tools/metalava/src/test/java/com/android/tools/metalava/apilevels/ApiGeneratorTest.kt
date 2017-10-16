/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.tools.metalava.apilevels

import com.android.tools.metalava.DriverTest
import com.android.utils.XmlUtils
import com.google.common.truth.Truth
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.text.Charsets.UTF_8

class ApiGeneratorTest : DriverTest() {
    @Test
    fun `Extract API levels`() {
        val oldSdkJars = File("prebuilts/tools/common/api-versions")
        if (!oldSdkJars.isDirectory) {
            println("Ignoring ${ApiGeneratorTest::class.java}: prebuilts not found - is \$PWD set to an Android source tree?")
            return
        }

        val platformJars = File("prebuilts/sdk")
        if (!platformJars.isDirectory) {
            println("Ignoring ${ApiGeneratorTest::class.java}: prebuilts not found: $platformJars")
            return
        }

        val output = File.createTempFile("api-info", "xml")
        output.deleteOnExit()
        val outputPath = output.path

        check(
            extraArguments = arrayOf(
                "--generate-api-levels",
                outputPath,
                "--android-jar-pattern",
                "${oldSdkJars.path}/android-%/android.jar",
                "--android-jar-pattern",
                "${platformJars.path}/%/android.jar"
            ),
            checkDoclava1 = false,
            signatureSource = """
                package test.pkg {
                  public class MyTest {
                    ctor public MyTest();
                    method public int clamp(int);
                    method public java.lang.Double convert(java.lang.Float);
                    field public java.lang.Number myNumber;
                  }
                }
                """
        )

        assertTrue(output.isFile)

        val xml = output.readText(UTF_8)
        Truth.assertThat(xml).contains("<class name=\"android/Manifest\$permission\" since=\"1\">")
        Truth.assertThat(xml).contains("<field name=\"BIND_CARRIER_MESSAGING_SERVICE\" since=\"22\" deprecated=\"23\"/>")

        val document = XmlUtils.parseDocumentSilently(xml, false)
        assertNotNull(document)

    }

}
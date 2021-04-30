/*
 * Copyright 2021, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export default class ObjectFormatter {
    static format(obj: any): {} {
        const entries = Object.entries(obj)
        const sortedEntries = entries.sort()

        const result: any = {}
        sortedEntries.forEach(entry => {
            const key = entry[0]
            const value = entry[1]
            if (value && typeof(value) == `object`) {
                result[key] = this.format(value)
            } else {
                result[key] = value
            }
        })

        // Reassign prototype to ensure formatters work
        result.__proto__ = obj.__proto__
        return result
    }
}
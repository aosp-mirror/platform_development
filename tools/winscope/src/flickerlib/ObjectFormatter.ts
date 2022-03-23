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

import {toBounds, toBuffer, toColor, toPoint, toRect,
    toRectF, toRegion, toTransform} from './common';
import intDefMapping from
    '../../../../../prebuilts/misc/common/winscope/intDefMapping.json';

export default class ObjectFormatter {
    private static INVALID_ELEMENT_PROPERTIES = ['length', 'name', 'prototype', 'children',
    'childrenWindows', 'ref', 'root', 'layers', 'resolvedChildren']

    private static FLICKER_INTDEF_MAP = new Map([
        [`WindowLayoutParams.type`, `android.view.WindowManager.LayoutParams.WindowType`],
        [`WindowLayoutParams.flags`, `android.view.WindowManager.LayoutParams.Flags`],
        [`WindowLayoutParams.privateFlags`, `android.view.WindowManager.LayoutParams.PrivateFlags`],
        [`WindowLayoutParams.gravity`, `android.view.Gravity.GravityFlags`],
        [`WindowLayoutParams.softInputMode`, `android.view.WindowManager.LayoutParams.WindowType`],
        [`WindowLayoutParams.systemUiVisibilityFlags`, `android.view.WindowManager.LayoutParams.SystemUiVisibilityFlags`],
        [`WindowLayoutParams.subtreeSystemUiVisibilityFlags`, `android.view.WindowManager.LayoutParams.SystemUiVisibilityFlags`],
        [`WindowLayoutParams.behavior`, `android.view.WindowInsetsController.Behavior`],
        [`WindowLayoutParams.fitInsetsSides`, `android.view.WindowInsets.Side.InsetsSide`],

        [`Configuration.windowingMode`, `android.app.WindowConfiguration.WindowingMode`],
        [`WindowConfiguration.windowingMode`, `android.app.WindowConfiguration.WindowingMode`],
        [`Configuration.orientation`, `android.content.pm.ActivityInfo.ScreenOrientation`],
        [`WindowConfiguration.orientation`, `android.content.pm.ActivityInfo.ScreenOrientation`],
        [`WindowState.orientation`, `android.content.pm.ActivityInfo.ScreenOrientation`],
    ])

    static format(obj: any): {} {
        const entries = Object.entries(obj)
            .filter(it => !it[0].includes(`$`))
            .filter(it => !this.INVALID_ELEMENT_PROPERTIES.includes(it[0]))
        const sortedEntries = entries.sort()

        const result: any = {}
        sortedEntries.forEach(entry => {
            const key = entry[0]
            const value: any = entry[1]

            if (value) {
                // flicker obj
                if (value.prettyPrint) {
                    result[key] = value.prettyPrint()
                } else {
                    // converted proto to flicker
                    const translatedObject = this.translateObject(value)
                    if (translatedObject) {
                        result[key] = translatedObject.prettyPrint()
                    // objects - recursive call
                    } else if (value && typeof(value) == `object`) {
                        result[key] = this.format(value)
                    } else {
                    // values
                        result[key] = this.translateIntDef(obj, key, value)
                    }
                }

            }
        })

        return Object.freeze(result)
    }

    /**
     * Translate some predetermined proto objects into their flicker equivalent
     *
     * Returns null if the object cannot be translated
     *
     * @param obj Object to translate
     */
    private static translateObject(obj) {
        const type = obj?.$type?.name
        switch(type) {
            case `SizeProto`: return toBounds(obj)
            case `ActiveBufferProto`: return toBuffer(obj)
            case `ColorProto`: return toColor(obj)
            case `PointProto`: return toPoint(obj)
            case `RectProto`: return toRect(obj)
            case `FloatRectProto`: return toRectF(obj)
            case `RegionProto`: return toRegion(obj)
            case `TransformProto`: return toTransform(obj)
            case 'ColorTransformProto': {
                const formatted = this.formatColorTransform(obj.val);
                return `${formatted}`;
            }
        }

        return null
    }

    private static formatColorTransform(vals) {
        const fixedVals = vals.map((v) => v.toFixed(1));
        let formatted = ``;
        for (let i = 0; i < fixedVals.length; i += 4) {
            formatted += `[`;
            formatted += fixedVals.slice(i, i + 4).join(', ');
            formatted += `] `;
        }
        return formatted;
    }

    /**
     * Obtains from the proto field, the metadata related to the typedef type (if any)
     *
     * @param obj Proto object
     * @param propertyName Property to search
     */
    private static getTypeDefSpec(obj: any, propertyName: string): string {
        const fields = obj?.$type?.fields
        if (!fields) {
            return null
        }

        const options = fields[propertyName]?.options
        if (!options) {
            return null
        }

        return options["(.android.typedef)"]
    }

    /**
     * Translate intdef properties into their string representation
     *
     * For proto objects check the
     *
     * @param parentObj Object containing the value to parse
     * @param propertyName Property to search
     * @param value Property value
     */
    private static translateIntDef(parentObj: any, propertyName: string, value: any): string {
        const parentClassName = parentObj.constructor.name
        const propertyPath = `${parentClassName}.${propertyName}`

        let translatedValue = value
        // Parse Flicker objects (no intdef annotation supported)
        if (this.FLICKER_INTDEF_MAP.has(propertyPath)) {
            translatedValue = this.getIntFlagsAsStrings(value,
                this.FLICKER_INTDEF_MAP.get(propertyPath))
        } else {
            // If it's a proto, search on the proto definition for the intdef type
            const typeDefSpec = this.getTypeDefSpec(parentObj, propertyName)
            if (typeDefSpec) {
                translatedValue = this.getIntFlagsAsStrings(value, typeDefSpec)
            }
        }

        return translatedValue
    }

    /**
     * Translate a property from its numerical value into its string representation
     *
     * @param intFlags Property value
     * @param annotationType IntDef type to use
     */
    private static getIntFlagsAsStrings(intFlags: any, annotationType: string) {
        const flags = [];

        const mapping = intDefMapping[annotationType].values;
        const knownFlagValues = Object.keys(mapping).reverse().map(x => parseInt(x));

        if (mapping.length == 0) {
            console.warn("No mapping for type", annotationType)
            return intFlags + ""
        }

        // Will only contain bits that have not been associated with a flag.
        const parsedIntFlags = parseInt(intFlags);
        let leftOver = parsedIntFlags;

        for (const flagValue of knownFlagValues) {
          if (((leftOver & flagValue) && ((intFlags & flagValue) === flagValue))
                || (parsedIntFlags === 0 && flagValue === 0)) {
            flags.push(mapping[flagValue]);

            leftOver = leftOver & ~flagValue;
          }
        }

        if (flags.length === 0) {
          console.error('No valid flag mappings found for ',
              intFlags, 'of type', annotationType);
        }

        if (leftOver) {
          // If 0 is a valid flag value that isn't in the intDefMapping
          // it will be ignored
          flags.push(leftOver);
        }

        return flags.join(' | ');
      }
}
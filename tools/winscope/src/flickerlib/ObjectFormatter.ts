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

import {toSize, toActiveBuffer, toColor, toColor3, toPoint, toRect,
    toRectF, toRegion, toMatrix22, toTransform} from './common';
import intDefMapping from
    '../../../../../prebuilts/misc/common/winscope/intDefMapping.json';
import config from '../config/Configuration.json'

function readIntdefMap(): Map<string, string> {
    const map = new Map<string, string>();
    const keys = Object.keys(config.intDefColumn);

    keys.forEach(key => {
        const value = config.intDefColumn[key];
        map.set(key, value);
    });

    return map;
}
export default class ObjectFormatter {
    static displayDefaults: boolean = false
    private static INVALID_ELEMENT_PROPERTIES = config.invalidProperties;

    private static FLICKER_INTDEF_MAP = readIntdefMap();

    static cloneObject(entry: any): any {
        let obj: any = {}
        const properties = ObjectFormatter.getProperties(entry);
        properties.forEach(prop => obj[prop] = entry[prop]);
        return obj;
    }

    /**
     * Get the true properties of an entry excluding functions, kotlin gernerated
     * variables, explicitly excluded properties, and flicker objects already in
     * the hierarchy that shouldn't be traversed when formatting the entry
     * @param entry The entry for which we want to get the properties for
     * @return The "true" properties of the entry as described above
     */
    static getProperties(entry: any): string[] {
        var props = [];
        let obj = entry;

        do {
            const properties = Object.getOwnPropertyNames(obj).filter(it => {
                // filter out functions
                if (typeof(entry[it]) === 'function') return false;
                // internal propertires from kotlinJs
                if (it.includes(`$`)) return false;
                // private kotlin variables from kotlin
                if (it.startsWith(`_`)) return false;
                // some predefined properties used only internally (e.g., children, ref, diff)
                if (this.INVALID_ELEMENT_PROPERTIES.includes(it)) return false;

                const value = entry[it];
                // only non-empty arrays of non-flicker objects (otherwise they are in hierarchy)
                if (Array.isArray(value) && value.length > 0) return !value[0].stableId;
                // non-flicker object
                return !(value?.stableId);
            });
            properties.forEach(function (prop) {
                if (typeof(entry[prop]) !== 'function' && props.indexOf(prop) === -1) {
                    props.push(prop);
                }
            });
        } while (obj = Object.getPrototypeOf(obj));

        return props;
    }

    /**
     * Format a Winscope entry to be displayed in the UI
     * Accounts for different user display settings (e.g. hiding empty/default values)
     * @param obj The raw object to format
     * @return The formatted object
     */
    static format(obj: any): {} {
        const properties = this.getProperties(obj);
        const sortedProperties = properties.sort()

        const result: any = {}
        sortedProperties.forEach(entry => {
            const key = entry;
            const value: any = obj[key];

            if (value === null || value === undefined) {
                if (this.displayDefaults) {
                    result[key] = value;
                }
                return
            }

            if (value || this.displayDefaults) {
                // raw values (e.g., false or 0)
                if (!value) {
                    result[key] = value
                // flicker obj
                } else if (value.prettyPrint) {
                    const isEmpty = value.isEmpty === true;
                    if (!isEmpty || this.displayDefaults) {
                        result[key] = value.prettyPrint();
                    }
                } else {
                    // converted proto to flicker
                    const translatedObject = this.translateObject(value);
                    if (translatedObject) {
                        if (translatedObject.prettyPrint) {
                          result[key] = translatedObject.prettyPrint();
                        }
                        else {
                          result[key] = translatedObject;
                        }
                    // objects - recursive call
                    } else if (value && typeof(value) == `object`) {
                        const childObj = this.format(value) as any;
                        const isEmpty = Object.entries(childObj).length == 0 || childObj.isEmpty;
                        if (!isEmpty || this.displayDefaults) {
                            result[key] = childObj;
                        }
                    } else {
                    // values
                        result[key] = this.translateIntDef(obj, key, value);
                    }
                }

            }
        })

        return result;
    }

    /**
     * Translate some predetermined proto objects into their flicker equivalent
     *
     * Returns null if the object cannot be translated
     *
     * @param obj Object to translate
     */
    private static translateObject(obj) {
        const type = obj?.$type?.name;
        switch(type) {
            case `SizeProto`: return toSize(obj);
            case `ActiveBufferProto`: return toActiveBuffer(obj);
            case `Color3`: return toColor3(obj);
            case `ColorProto`: return toColor(obj);
            case `PointProto`: return toPoint(obj);
            case `RectProto`: return toRect(obj);
            case `Matrix22`: return toMatrix22(obj);
            case `FloatRectProto`: return toRectF(obj);
            case `RegionProto`: return toRegion(obj);
            case `TransformProto`: return toTransform(obj);
            case 'ColorTransformProto': {
                const formatted = this.formatColorTransform(obj.val);
                return `${formatted}`;
            }
        }

        return null;
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
        const fields = obj?.$type?.fields;
        if (!fields) {
            return null;
        }

        const options = fields[propertyName]?.options;
        if (!options) {
            return null;
        }

        return options["(.android.typedef)"];
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
        const parentClassName = parentObj.constructor.name;
        const propertyPath = `${parentClassName}.${propertyName}`;

        let translatedValue = value;
        // Parse Flicker objects (no intdef annotation supported)
        if (this.FLICKER_INTDEF_MAP.has(propertyPath)) {
            translatedValue = this.getIntFlagsAsStrings(value,
                this.FLICKER_INTDEF_MAP.get(propertyPath));
        } else {
            // If it's a proto, search on the proto definition for the intdef type
            const typeDefSpec = this.getTypeDefSpec(parentObj, propertyName);
            if (typeDefSpec) {
                translatedValue = this.getIntFlagsAsStrings(value, typeDefSpec);
            }
        }

        return translatedValue;
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

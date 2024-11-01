/*
 * Copyright (C) 2024 The Android Open Source Project
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

import intDefMapping from 'common/intDefMapping.json';
import {TamperedProtoField} from 'parsers/tampered_message_type';
import {FixedStringFormatter} from 'trace/tree_node/formatters';
import {Operation} from 'trace/tree_node/operations/operation';
import {PropertyTreeNode} from 'trace/tree_node/property_tree_node';

export class TranslateIntDef implements Operation<PropertyTreeNode> {
  constructor(private readonly rootField: TamperedProtoField) {}

  apply(value: PropertyTreeNode, parentField = this.rootField): void {
    const protoType = parentField.tamperedMessageType;

    if (protoType === undefined) {
      return;
    }

    let field = parentField;
    if (field.name !== value.name) {
      field = protoType.fields[value.name] ?? parentField;
    }

    if (value.getAllChildren().length > 0) {
      value.getAllChildren().forEach((value) => {
        this.apply(value, field);
      });
    } else {
      const propertyValue = Number(value.getValue());
      if (!Number.isNaN(propertyValue) && propertyValue !== -1) {
        const translation = this.translateIntDefToStringIfNeeded(
          propertyValue,
          field,
        );
        if (typeof translation === 'string') {
          value.setFormatter(new FixedStringFormatter(translation));
        }
      }
    }
  }

  private translateIntDefToStringIfNeeded(
    value: number,
    field: TamperedProtoField,
  ): string | number {
    const typeDefSpec = this.getTypeDefSpecFromField(field);

    if (typeDefSpec) {
      return this.getIntFlagsAsStrings(value, typeDefSpec);
    } else {
      const propertyPath = `${field.parent?.name}.${field.name}`;
      if (this.intDefColumn[propertyPath]) {
        return this.getIntFlagsAsStrings(
          value,
          this.intDefColumn[propertyPath] as string,
        );
      }
    }

    return value;
  }

  private getTypeDefSpecFromField(
    field: TamperedProtoField,
  ): string | undefined {
    if (field.options === undefined) {
      return undefined;
    } else if (field.options['(.android.typedef)'] !== undefined) {
      return field.options['(.android.typedef)'];
    } else if (field.options['(.perfetto.protos.typedef)'] !== undefined) {
      return field.options['(.perfetto.protos.typedef)'];
    }
    return undefined;
  }

  private getIntFlagsAsStrings(
    intFlags: number,
    annotationType: string,
  ): string {
    let flags = '';

    const mapping =
      intDefMapping[annotationType as keyof typeof intDefMapping].values;

    const knownFlagValues = Object.keys(mapping)
      .reverse()
      .map((x) => Math.floor(Number(x)));

    if (knownFlagValues.length === 0) {
      console.warn('No mapping for type', annotationType);
      return intFlags + '';
    }

    // Will only contain bits that have not been associated with a flag.
    const parsedIntFlags = Math.floor(Number(intFlags));
    let leftOver = parsedIntFlags;

    for (const flagValue of knownFlagValues) {
      if (
        (leftOver & flagValue && (intFlags & flagValue) === flagValue) ||
        (parsedIntFlags === 0 && flagValue === 0)
      ) {
        if (flags.length > 0) flags += ' | ';
        flags += mapping[flagValue as keyof typeof mapping];

        leftOver = leftOver & ~flagValue;
      }
    }

    if (flags.length === 0) {
      return `${intFlags}`;
    }

    if (leftOver) {
      // If 0 is a valid flag value that isn't in the intDefMapping it will be ignored
      flags += ' | ' + leftOver;
    }

    return flags;
  }

  private readonly intDefColumn: {[key: string]: string} = {
    'WindowLayoutParams.type':
      'android.view.WindowManager.LayoutParams.WindowType',
    'WindowLayoutParams.flags': 'android.view.WindowManager.LayoutParams.Flags',
    'WindowLayoutParams.privateFlags':
      'android.view.WindowManager.LayoutParams.PrivateFlags',
    'WindowLayoutParams.gravity': 'android.view.Gravity.GravityFlags',
    'WindowLayoutParams.softInputMode':
      'android.view.WindowManager.LayoutParams.WindowType',
    'WindowLayoutParams.systemUiVisibilityFlags':
      'android.view.WindowManager.LayoutParams.SystemUiVisibilityFlags',
    'WindowLayoutParams.subtreeSystemUiVisibilityFlags':
      'android.view.WindowManager.LayoutParams.SystemUiVisibilityFlags',
    'WindowLayoutParams.behavior':
      'android.view.WindowInsetsController.Behavior',
    'WindowLayoutParams.fitInsetsSides':
      'android.view.WindowInsets.Side.InsetsSide',
    'InputWindowInfoProto.layoutParamsFlags':
      'android.view.WindowManager.LayoutParams.Flags',
    'InputWindowInfoProto.inputConfig':
      'android.view.InputWindowHandle.InputConfigFlags',
    'Configuration.windowingMode':
      'android.app.WindowConfiguration.WindowingMode',
    'WindowConfiguration.windowingMode':
      'android.app.WindowConfiguration.WindowingMode',
    'Configuration.orientation':
      'android.content.pm.ActivityInfo.ScreenOrientation',
    'WindowConfiguration.orientation':
      'android.content.pm.ActivityInfo.ScreenOrientation',
    'WindowState.orientation':
      'android.content.pm.ActivityInfo.ScreenOrientation',
    'InsetsSourceControlProto.typeNumber':
      'android.view.WindowInsets.Type.InsetsType',
    'InsetsSourceConsumerProto.typeNumber':
      'android.view.WindowInsets.Type.InsetsType',
    'WindowStateProto.requestedVisibleTypes':
      'android.view.WindowInsets.Type.InsetsType',
    'Target.flags': 'android.window.TransitionInfo.ChangeFlags',
    'Transition.flags': 'android.view.WindowManager.TransitionFlags',
  };
}

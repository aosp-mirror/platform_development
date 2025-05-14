/*
 * Copyright (C) 2025 The Android Open Source Project
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

import {
  getTimestampConverter,
  TimestampConverterUtils,
} from 'common/time/test_utils';
import Long from 'long';
import {TransitionType} from 'parsers/transitions/transition_type';
import {perfetto} from 'protos/perfetto/trace/static';
import {PropertyTreeBuilder} from 'test/unit/property_tree_builder';
import {
  DEFAULT_PROPERTY_FORMATTER,
  EnumFormatter,
  FixedStringFormatter,
  TIMESTAMP_NODE_FORMATTER,
} from 'trace/tree_node/formatters';
import {PropertySource} from 'trace/tree_node/property_tree_node';
import {EntryPropertiesTreeFactory} from './entry_properties_tree_factory';

describe('EntryPropertiesTreeFactory', () => {
  const transitionProto: perfetto.protos.IShellTransition = {
    id: 5,
    createTimeNs: Long.fromInt(10),
    sendTimeNs: Long.fromInt(15),
    dispatchTimeNs: Long.fromInt(20),
    mergeTimeNs: Long.fromInt(25),
    shellAbortTimeNs: Long.fromInt(35),
    wmAbortTimeNs: Long.fromInt(40),
    finishTimeNs: Long.fromInt(45),
    startTransactionId: Long.fromInt(100),
    handler: 2,
    mergeTarget: 4,
    flags: 16,
  };

  // adds formatters, updates abort time node, adds defaults for only type
  // and targets, translates flags intdef
  const expectedWmTree = new PropertyTreeBuilder()
    .setRootId('TransitionTraceEntry')
    .setIsRoot(true)
    .setName('Selected Transition')
    .setChildren([
      {
        name: 'wmData',
        children: [
          {name: 'id', value: 5, formatter: DEFAULT_PROPERTY_FORMATTER},
          {
            name: 'createTimeNs',
            value: TimestampConverterUtils.makeElapsedTimestamp(10n),
            formatter: TIMESTAMP_NODE_FORMATTER,
          },
          {
            name: 'sendTimeNs',
            value: TimestampConverterUtils.makeElapsedTimestamp(15n),
            formatter: TIMESTAMP_NODE_FORMATTER,
          },
          {
            name: 'finishTimeNs',
            value: TimestampConverterUtils.makeElapsedTimestamp(45n),
            formatter: TIMESTAMP_NODE_FORMATTER,
          },
          {
            name: 'startTransactionId',
            value: transitionProto.startTransactionId,
            formatter: DEFAULT_PROPERTY_FORMATTER,
          },
          {
            name: 'flags',
            value: 16,
            formatter: new FixedStringFormatter('TRANSIT_FLAG_APP_CRASHED'),
          },
          {
            name: 'abortTimeNs',
            value: TimestampConverterUtils.makeElapsedTimestamp(40n),
            formatter: TIMESTAMP_NODE_FORMATTER,
          },
          {
            name: 'type',
            value: 0,
            source: PropertySource.DEFAULT,
            formatter: new EnumFormatter(TransitionType),
          },
          {
            name: 'targets',
            value: [],
            source: PropertySource.DEFAULT,
            formatter: DEFAULT_PROPERTY_FORMATTER,
          },
        ],
      },
    ])
    .build();
  // adds formatters, updates abort time node, does not add any defaults,
  // transforms timestamps, translates handler
  const expectedShellTree = new PropertyTreeBuilder()
    .setRootId('TransitionTraceEntry')
    .setIsRoot(true)
    .setName('Selected Transition')
    .setChildren([
      {
        name: 'shellData',
        children: [
          {name: 'id', value: 5, formatter: DEFAULT_PROPERTY_FORMATTER},
          {
            name: 'dispatchTimeNs',
            value: TimestampConverterUtils.makeElapsedTimestamp(20n),
            formatter: TIMESTAMP_NODE_FORMATTER,
          },
          {
            name: 'mergeTimeNs',
            value: TimestampConverterUtils.makeElapsedTimestamp(25n),
            formatter: TIMESTAMP_NODE_FORMATTER,
          },
          {
            name: 'handler',
            value: 2,
            formatter: new EnumFormatter({2: 'HANDLER1'}),
          },
          {
            name: 'mergeTarget',
            value: 4,
            formatter: DEFAULT_PROPERTY_FORMATTER,
          },
          {
            name: 'abortTimeNs',
            value: TimestampConverterUtils.makeElapsedTimestamp(35n),
            formatter: TIMESTAMP_NODE_FORMATTER,
          },
        ],
      },
    ])
    .build();

  it('makeTransitionPropertiesTree', () => {
    const tree = EntryPropertiesTreeFactory.makeTransitionPropertiesTree(
      expectedShellTree,
      expectedWmTree,
    );
    expect(tree.getChildByName('shellData')).toEqual(
      expectedShellTree.getChildByName('shellData'),
    );
    expect(tree.getChildByName('wmData')).toEqual(
      expectedWmTree.getChildByName('wmData'),
    );
    expect(tree.getChildByName('id')).toBeDefined();
    expect(tree.getChildByName('duration')).toBeDefined();
    expect(tree.getChildByName('aborted')).toBeDefined();
    expect(tree.getChildByName('merged')).toBeDefined();
    expect(tree.getChildByName('played')).toBeDefined();
  });

  describe('makeWmPropertiesTree', () => {
    it('robust to no info', () => {
      const expectedTree = new PropertyTreeBuilder()
        .setRootId('TransitionTraceEntry')
        .setName('Selected Transition')
        .setIsRoot(true)
        .setSource(PropertySource.PROTO)
        .setChildren([
          {
            name: 'wmData',
            value: null,
            formatter: DEFAULT_PROPERTY_FORMATTER,
          },
        ])
        .build();
      expect(EntryPropertiesTreeFactory.makeWmPropertiesTree()).toEqual(
        expectedTree,
      );
    });

    it('makes tree', () => {
      const timestampConverter = getTimestampConverter();
      const tree = EntryPropertiesTreeFactory.makeWmPropertiesTree({
        entry: transitionProto,
        timestampConverter,
      });
      expect(tree).toEqual(expectedWmTree);
    });
  });

  describe('makeShellPropertiesTree', () => {
    it('robust to no info', () => {
      const expectedTree = new PropertyTreeBuilder()
        .setRootId('TransitionTraceEntry')
        .setName('Selected Transition')
        .setIsRoot(true)
        .setSource(PropertySource.PROTO)
        .setChildren([
          {
            name: 'shellData',
            value: null,
            formatter: DEFAULT_PROPERTY_FORMATTER,
          },
        ])
        .build();
      expect(EntryPropertiesTreeFactory.makeShellPropertiesTree()).toEqual(
        expectedTree,
      );
    });

    it('makes tree', () => {
      const timestampConverter = getTimestampConverter();
      const tree = EntryPropertiesTreeFactory.makeShellPropertiesTree({
        entry: transitionProto,
        timestampConverter,
        handlerMapping: {2: 'HANDLER1'},
      });
      expect(tree).toEqual(expectedShellTree);
    });

    it('robust to no handler mapping', () => {
      const timestampConverter = getTimestampConverter();
      const tree = EntryPropertiesTreeFactory.makeShellPropertiesTree({
        entry: transitionProto,
        timestampConverter,
      });
      expect(
        tree
          .getChildByName('shellData')
          ?.getChildByName('handler')
          ?.formattedValue(),
      ).toEqual('2');
    });
  });
});

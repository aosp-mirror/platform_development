/*
 * Copyright (C) 2022 The Android Open Source Project
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
import {assertDefined} from 'common/assert_utils';
import {TimestampType} from 'common/time';
import {NO_TIMEZONE_OFFSET_FACTORY} from 'common/timestamp_factory';
import {TraceBuilder} from 'test/unit/trace_builder';
import {UnitTestUtils} from 'test/unit/utils';
import {CustomQueryType} from 'trace/custom_query';
import {Parser} from 'trace/parser';
import {Trace} from 'trace/trace';
import {TraceType} from 'trace/trace_type';
import {HierarchyTreeNode} from 'trace/tree_node/hierarchy_tree_node';
import {UiTreeUtils} from 'viewers/common/ui_tree_utils';

describe('ParserSurfaceFlinger', () => {
  describe('trace with elapsed + real timestamp', () => {
    let parser: Parser<HierarchyTreeNode>;
    let trace: Trace<HierarchyTreeNode>;

    beforeAll(async () => {
      jasmine.addCustomEqualityTester(UnitTestUtils.timestampEqualityTester);
      parser = (await UnitTestUtils.getParser(
        'traces/elapsed_and_real_timestamp/SurfaceFlinger.pb',
      )) as Parser<HierarchyTreeNode>;
      trace = new TraceBuilder<HierarchyTreeNode>()
        .setType(TraceType.SURFACE_FLINGER)
        .setParser(parser)
        .build();
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.SURFACE_FLINGER);
    });

    it('provides elapsed timestamps', () => {
      const expected = [
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(14500282843n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(14631249355n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(15403446377n),
      ];
      expect(
        assertDefined(parser.getTimestamps(TimestampType.ELAPSED)).slice(0, 3),
      ).toEqual(expected);
    });

    it('provides real timestamps', () => {
      const expected = [
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659107089102062832n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659107089233029344n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659107090005226366n),
      ];
      expect(
        assertDefined(parser.getTimestamps(TimestampType.REAL)).slice(0, 3),
      ).toEqual(expected);
    });

    it('applies timezone info to real timestamps only', async () => {
      const parserWithTimezoneInfo = (await UnitTestUtils.getParser(
        'traces/elapsed_and_real_timestamp/SurfaceFlinger.pb',
        true,
      )) as Parser<HierarchyTreeNode>;
      expect(parserWithTimezoneInfo.getTraceType()).toEqual(
        TraceType.SURFACE_FLINGER,
      );

      const expectedElapsed = [
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(14500282843n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(14631249355n),
        NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(15403446377n),
      ];
      expect(
        assertDefined(
          parserWithTimezoneInfo.getTimestamps(TimestampType.ELAPSED),
        ).slice(0, 3),
      ).toEqual(expectedElapsed);

      const expectedReal = [
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659126889102062832n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659126889233029344n),
        NO_TIMEZONE_OFFSET_FACTORY.makeRealTimestamp(1659126890005226366n),
      ];
      expect(
        assertDefined(
          parserWithTimezoneInfo.getTimestamps(TimestampType.REAL),
        ).slice(0, 3),
      ).toEqual(expectedReal);
    });

    it('provides correct root entry node', async () => {
      const entry = await parser.getEntry(1, TimestampType.REAL);
      expect(entry.id).toEqual('LayerTraceEntry root');
      expect(entry.name).toEqual('root');
    });

    it('decodes layer state flags', async () => {
      const entry = await parser.getEntry(0, TimestampType.REAL);
      {
        const layer = assertDefined(
          entry.findDfs(UiTreeUtils.makeIdMatchFilter('27 Leaf:24:25#27')),
        );
        expect(layer.name).toEqual('Leaf:24:25#27');

        expect(
          assertDefined(layer.getEagerPropertyByName('flags')).formattedValue(),
        ).toEqual('0');
        expect(
          assertDefined(
            layer.getEagerPropertyByName('verboseFlags'),
          ).formattedValue(),
        ).toEqual('');
      }
      {
        const layer = assertDefined(
          entry.findDfs(UiTreeUtils.makeIdMatchFilter('48 Task=4#48')),
        );
        expect(layer.name).toEqual('Task=4#48');

        expect(
          assertDefined(layer.getEagerPropertyByName('flags')).formattedValue(),
        ).toEqual('1');
        expect(
          assertDefined(
            layer.getEagerPropertyByName('verboseFlags'),
          ).formattedValue(),
        ).toEqual('HIDDEN (0x1)');
      }
      {
        const layer = assertDefined(
          entry.findDfs(
            UiTreeUtils.makeIdMatchFilter('77 Wallpaper BBQ wrapper#77'),
          ),
        );
        expect(layer.name).toEqual('Wallpaper BBQ wrapper#77');

        expect(
          assertDefined(layer.getEagerPropertyByName('flags')).formattedValue(),
        ).toEqual('256');
        expect(
          assertDefined(
            layer.getEagerPropertyByName('verboseFlags'),
          ).formattedValue(),
        ).toEqual('ENABLE_BACKPRESSURE (0x100)');
      }
    });

    it('supports VSYNCID custom query', async () => {
      const entries = await trace
        .sliceEntries(0, 3)
        .customQuery(CustomQueryType.VSYNCID);
      const values = entries.map((entry) => entry.getValue());
      expect(values).toEqual([4891n, 5235n, 5748n]);
    });

    it('supports SF_LAYERS_ID_AND_NAME custom query', async () => {
      const idAndNames = await trace
        .sliceEntries(0, 1)
        .customQuery(CustomQueryType.SF_LAYERS_ID_AND_NAME);
      expect(idAndNames).toContain({
        id: 4,
        name: 'WindowedMagnification:0:31#4',
      });
      expect(idAndNames).toContain({id: 5, name: 'HideDisplayCutout:0:14#5'});
    });

    it('is robust to duplicated layer ids', async () => {
      const parser = (await UnitTestUtils.getParser(
        'traces/elapsed_and_real_timestamp/SurfaceFlinger_with_duplicated_ids.pb',
      )) as Parser<HierarchyTreeNode>;
      const entry = await parser.getEntry(0, TimestampType.REAL);
      expect(entry).toBeTruthy();

      const layer = assertDefined(
        entry.findDfs(
          UiTreeUtils.makeIdMatchFilter(
            '-2147483595 Input Consumer recents_animation_input_consumer#408(Mirror)',
          ),
        ),
      );
      expect(layer.name).toEqual(
        'Input Consumer recents_animation_input_consumer#408(Mirror)',
      );
      expect(layer.getAllChildren().length).toEqual(0);

      const dupLayer = assertDefined(
        entry.findDfs(
          UiTreeUtils.makeIdMatchFilter(
            '-2147483595 Input Consumer recents_animation_input_consumer#408(Mirror) duplicate(1)',
          ),
        ),
      );

      expect(dupLayer.name).toEqual(
        'Input Consumer recents_animation_input_consumer#408(Mirror) duplicate(1)',
      );
      expect(dupLayer.getAllChildren().length).toEqual(0);
    });
  });

  describe('trace with elapsed (only) timestamp', () => {
    let parser: Parser<HierarchyTreeNode>;

    beforeAll(async () => {
      parser = (await UnitTestUtils.getParser(
        'traces/elapsed_timestamp/SurfaceFlinger.pb',
      )) as Parser<HierarchyTreeNode>;
    });

    it('has expected trace type', () => {
      expect(parser.getTraceType()).toEqual(TraceType.SURFACE_FLINGER);
    });

    it('provides elapsed timestamps', () => {
      expect(
        assertDefined(parser.getTimestamps(TimestampType.ELAPSED))[0],
      ).toEqual(NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(850335483446n));
    });

    it("doesn't provide real timestamps", () => {
      expect(parser.getTimestamps(TimestampType.REAL)).toEqual(undefined);
    });

    it('does not apply timezone info', async () => {
      const parserWithTimezoneInfo = (await UnitTestUtils.getParser(
        'traces/elapsed_timestamp/SurfaceFlinger.pb',
        true,
      )) as Parser<HierarchyTreeNode>;
      expect(parserWithTimezoneInfo.getTraceType()).toEqual(
        TraceType.SURFACE_FLINGER,
      );

      expect(
        assertDefined(
          parserWithTimezoneInfo.getTimestamps(TimestampType.ELAPSED),
        )[0],
      ).toEqual(NO_TIMEZONE_OFFSET_FACTORY.makeElapsedTimestamp(850335483446n));
    });

    it('provides correct root entry node', async () => {
      const entry = await parser.getEntry(0, TimestampType.ELAPSED);
      expect(entry.id).toEqual('LayerTraceEntry root');
      expect(entry.name).toEqual('root');
    });
  });
});

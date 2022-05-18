import { DiffType } from "../src/utils/diff.js";
import { ObjectTransformer } from "../src/transform.js";
import { NodeBuilder, toPlainObject } from "./utils/tree.js";

describe("ObjectTransformer", () => {
    it("can transform a simple object", () => {
        const obj = {
            obj: {
                string: 'string',
                number: 3,
            },
            array: [
                {
                    nested: "item",
                },
                "two",
            ],
        };

        const expectedTransformedObj = toPlainObject(
            new NodeBuilder().setTransformed().setName('root')
                .setStableId('root').setChildren([
                new NodeBuilder().setTransformed().setName('obj')
                    .setStableId('root.obj').setChildren([
                    new NodeBuilder().setTransformed().setName('string: string')
                        .setStableId('root.obj.string').setCombined().build(),
                    new NodeBuilder().setTransformed().setName('number: 3')
                        .setStableId('root.obj.number').setCombined().build(),
                ]).build(),
                new NodeBuilder().setTransformed().setName('array')
                    .setStableId('root.array').setChildren([
                    new NodeBuilder().setTransformed().setName('0')
                        .setStableId('root.array.0').setChildren([
                        new NodeBuilder().setTransformed().setName('nested: item')
                            .setStableId('root.array.0.nested').setCombined().build(),
                    ]).build(),
                    new NodeBuilder().setTransformed().setName("1: two")
                        .setStableId('root.array.1').setCombined().build(),
                ]).build()
            ]).build()
        );

        const transformedObj = new ObjectTransformer(obj, 'root', 'root')
            .setOptions({ formatter: () => { } })
            .transform();

        expect(transformedObj).toEqual(expectedTransformedObj);
    });

    it("handles null as expected", () => {
        const obj = {
            obj: {
                null: null,
            },
        }

        const expectedTransformedObj = toPlainObject(
            new NodeBuilder().setTransformed().setName('root')
                .setStableId('root').setChildren([
                new NodeBuilder().setTransformed().setName('obj')
                    .setStableId('root.obj').setChildren([
                    new NodeBuilder().setTransformed().setName('null: null')
                        .setStableId('root.obj.null').setCombined().build(),
                ]).build(),
            ]).build()
        );

        const transformedObj = new ObjectTransformer(obj, 'root', 'root')
            .setOptions({ formatter: () => { } })
            .transform();

        expect(transformedObj).toEqual(expectedTransformedObj);
    });

    it("can generate a simple add diff", () => {
        const oldObj = {
            a: {
                b: 1,
            },
            c: 2,
        };

        const newObj = {
            a: {
                b: 1,
                d: 3,
            },
            c: 2,
        };

        const expectedTransformedObj = toPlainObject(
            new NodeBuilder().setTransformed().setName('root')
                .setStableId('root').setDiffType(DiffType.NONE).setChildren([
                new NodeBuilder().setTransformed().setName('a')
                    .setStableId('root.a').setDiffType(DiffType.NONE).setChildren([
                    new NodeBuilder().setTransformed().setName('b: 1')
                        .setStableId('root.a.b').setDiffType(DiffType.NONE).setCombined().build(),
                    new NodeBuilder().setTransformed().setName('d: 3')
                        .setStableId('root.a.d').setDiffType(DiffType.ADDED).setCombined().build(),
                ]).build(),
                new NodeBuilder().setTransformed().setName('c: 2').setStableId('root.c').setDiffType(DiffType.NONE).setCombined().build(),
            ]).build()
        );

        const transformedObj = new ObjectTransformer(newObj, 'root', 'root')
            .setOptions({ formatter: () => { } })
            .withDiff(oldObj)
            .transform();

        expect(transformedObj).toEqual(expectedTransformedObj);
    });

    it("can handle null", () => {
        const oldObj = {
            a: null,
        };

        const newObj = {
            a: 1,
        };

        const expectedTransformedObj = toPlainObject(
          new NodeBuilder().setTransformed().setName('root')
              .setStableId('root').setDiffType(DiffType.NONE).setChildren([
              new NodeBuilder().setTransformed().setName('a')
                  .setStableId('root.a').setDiffType(DiffType.NONE).setChildren([
                  new NodeBuilder().setTransformed().setName('1')
                      .setStableId('root.a.1').setDiffType(DiffType.ADDED).build(),
                  new NodeBuilder().setTransformed().setName('null')
                      .setStableId('root.a.null').setDiffType(DiffType.DELETED).build(),
                ]).build(),
            ]).build()
        );

        const transformedObj = new ObjectTransformer(newObj, 'root', 'root')
            .setOptions({ formatter: () => { } })
            .withDiff(oldObj)
            .transform();

        expect(transformedObj).toEqual(expectedTransformedObj);
    });

    it("can handle nested null", () => {
        const oldObj = {
            a: {
                b: null,
            },
            c: 2,
        };

        const newObj = {
            a: {
                b: 1,
            },
            c: 2,
        };

        const expectedTransformedObj = toPlainObject(
            new NodeBuilder().setTransformed().setName('root')
                .setStableId('root').setDiffType(DiffType.NONE).setChildren([
                new NodeBuilder().setTransformed().setName('a')
                    .setStableId('root.a').setDiffType(DiffType.NONE).setChildren([
                    new NodeBuilder().setTransformed().setName('b')
                        .setStableId('root.a.b').setDiffType(DiffType.NONE).setChildren([
                        new NodeBuilder().setTransformed().setName('1')
                            .setStableId('root.a.b.1').setDiffType(DiffType.ADDED).build(),
                        new NodeBuilder().setTransformed().setName('null')
                            .setStableId('root.a.b.null').setDiffType(DiffType.DELETED).build(),
                    ]).build(),
                ]).build(),
                new NodeBuilder().setTransformed().setName('c: 2')
                    .setStableId('root.c').setDiffType(DiffType.NONE).setCombined().build(),
            ]).build()
        );

        const transformedObj = new ObjectTransformer(newObj, 'root', 'root')
            .setOptions({ formatter: () => { } })
            .withDiff(oldObj)
            .transform();

        expect(transformedObj).toEqual(expectedTransformedObj);
    });
});
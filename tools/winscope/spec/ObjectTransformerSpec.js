import { DiffType } from "../src/utils/diff.js";
import { ObjectTransformer } from "../src/transform.js";
import { ObjNode, ObjDiffNode, toPlainObject } from "./utils/tree.js";

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
            new ObjNode('root', [
                new ObjNode('obj', [
                    new ObjNode('string: string', [], true, 'root.obj.string'),
                    new ObjNode('number: 3', [], true, 'root.obj.number'),
                ], undefined, 'root.obj'),
                new ObjNode('array', [
                    new ObjNode('0', [
                        new ObjNode('nested: item', [], true, 'root.array.0.nested'),
                    ], undefined, 'root.array.0'),
                    new ObjNode("1: two", [], true, 'root.array.1'),
                ], undefined, 'root.array'),
            ], undefined, 'root')
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
            new ObjNode('root', [
                new ObjNode('obj', [
                    new ObjNode('null: null', [], true, 'root.obj.null'),
                ], undefined, 'root.obj'),
            ], undefined, 'root')
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
            new ObjDiffNode('root', DiffType.NONE, [
                new ObjDiffNode('a', DiffType.NONE, [
                    new ObjDiffNode('b: 1', DiffType.NONE, [], true, 'root.a.b'),
                    new ObjDiffNode('d: 3', DiffType.ADDED, [], true, 'root.a.d'),
                ], false, 'root.a'),
                new ObjDiffNode('c: 2', DiffType.NONE, [], true, 'root.c'),
            ], false, 'root')
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
            new ObjDiffNode('root', DiffType.NONE, [
                new ObjDiffNode('a', DiffType.NONE, [
                    new ObjDiffNode('1', DiffType.ADDED, [], false, 'root.a.1'),
                    new ObjDiffNode('null', DiffType.DELETED, [], false, 'root.a.null'),
                ], false, 'root.a'),
            ], false, 'root')
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
            new ObjDiffNode('root', DiffType.NONE, [
                new ObjDiffNode('a', DiffType.NONE, [
                    new ObjDiffNode('b', DiffType.NONE, [
                        new ObjDiffNode('1', DiffType.ADDED, [], false, 'root.a.b.1'),
                        new ObjDiffNode('null', DiffType.DELETED, [],  false, 'root.a.b.null'),
                    ], false, 'root.a.b'),
                ], false, 'root.a'),
                new ObjDiffNode('c: 2', DiffType.NONE, [], true, 'root.c'),
            ],  false, 'root')
        );

        const transformedObj = new ObjectTransformer(newObj, 'root', 'root')
            .setOptions({ formatter: () => { } })
            .withDiff(oldObj)
            .transform();

        expect(transformedObj).toEqual(expectedTransformedObj);
    });
});
import { DiffType } from "../src/utils/diff.js";
import { ObjectTransformer } from "../src/transform.js";
import { Node, DiffNode, toPlainObject } from "./utils/tree.js";

class ObjNode extends Node {
    constructor(name, children, combined) {
        const nodeDef = {
            kind: '',
            name: name,
        };
        if (combined) {
            nodeDef.combined = true;
        }
        super(nodeDef, children);
    }
}

class ObjDiffNode extends DiffNode {
    constructor(name, diffType, children, combined) {
        const nodeDef = {
            kind: '',
            name: name,
        };
        if (combined) {
            nodeDef.combined = true;
        }
        super(nodeDef, diffType, children);
    }
}

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
                    new ObjNode('string: string', [], true),
                    new ObjNode('number: 3', [], true),
                ]),
                new ObjNode('array', [
                    new ObjNode('0', [
                        new ObjNode('nested: item', [], true),
                    ]),
                    new ObjNode("1: two", [], true),
                ]),
            ])
        );

        const transformedObj = new ObjectTransformer(obj, 'root')
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
                    new ObjNode('null: null', [], true),
                ]),
            ])
        );

        const transformedObj = new ObjectTransformer(obj, 'root')
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
                    new ObjDiffNode('b: 1', DiffType.NONE, [], true),
                    new ObjDiffNode('d: 3', DiffType.ADDED, [], true),
                ]),
                new ObjDiffNode('c: 2', DiffType.NONE, [], true),
            ])
        );

        const transformedObj = new ObjectTransformer(newObj, 'root')
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
                    new ObjDiffNode('1', DiffType.ADDED, []),
                    new ObjDiffNode('null', DiffType.DELETED, []),
                ]),
            ])
        );

        const transformedObj = new ObjectTransformer(newObj, 'root')
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
                        new ObjDiffNode('1', DiffType.ADDED, []),
                        new ObjDiffNode('null', DiffType.DELETED, []),
                    ]),
                ]),
                new ObjDiffNode('c: 2', DiffType.NONE, [], true),
            ])
        );

        const transformedObj = new ObjectTransformer(newObj, 'root')
            .setOptions({ formatter: () => { } })
            .withDiff(oldObj)
            .transform();

        expect(transformedObj).toEqual(expectedTransformedObj);
    });
});
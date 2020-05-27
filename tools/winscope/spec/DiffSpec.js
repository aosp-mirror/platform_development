import { DiffGenerator, DiffType } from "../src/utils/diff.js";

class Node {
    constructor(nodeDef, children) {
        this.children = children;
        const { id, data } = nodeDef;
        this.id = id;
        if (data) {
            this.data = data;
        }
    }
}

class DiffNode extends Node {
    constructor(nodeDef, diffType, children) {
        super(nodeDef, children);
        this.diff = { type: diffType };
    }
}

function checkDiffTreeWithNoModifiedCheck(oldTree, newTree, expectedDiffTree) {
    const diffTree = new DiffGenerator(newTree)
        .compareWith(oldTree)
        .withUniqueNodeId(node => node.id)
        .generateDiffTree();

    expect(diffTree).toEqual(expectedDiffTree);
}

function isPrimitive(test) {
    return test !== Object(test);
};

function toPlainObject(theClass) {
    if (isPrimitive(theClass)) {
        return theClass;
    } else if (Array.isArray(theClass)) {
        return theClass.map(item => toPlainObject(item));
    } else {
        const keys = Object.getOwnPropertyNames(Object.assign({}, theClass));
        return keys.reduce((classAsObj, key) => {
            classAsObj[key] = toPlainObject(theClass[key]);
            return classAsObj;
        }, {});
    }
}

describe("DiffGenerator", () => {
    it("can generate a simple add diff", () => {
        const oldTree = new Node({ id: 1 }, [
            new Node({ id: 2 }, []),
            new Node({ id: 3 }, []),
            new Node({ id: 4 }, []),
        ]);

        const newTree = new Node({ id: 1 }, [
            new Node({ id: 2 }, []),
            new Node({ id: 3 }, [
                new Node({ id: 5 }, []),
            ]),
            new Node({ id: 4 }, []),
        ]);

        const diffTree = new DiffGenerator(newTree)
            .compareWith(oldTree)
            .withUniqueNodeId(node => node.id)
            .withModifiedCheck(() => false)
            .generateDiffTree();

        const expectedDiffTree = toPlainObject(
            new DiffNode({ id: 1 }, DiffType.NONE, [
                new DiffNode({ id: 2 }, DiffType.NONE, []),
                new DiffNode({ id: 3 }, DiffType.NONE, [
                    new DiffNode({ id: 5 }, DiffType.ADDED, []),
                ]),
                new DiffNode({ id: 4 }, DiffType.NONE, []),
            ])
        );

        checkDiffTreeWithNoModifiedCheck(oldTree, newTree, expectedDiffTree);
    });

    it("can generate a simple delete diff", () => {
        const oldTree = new Node({ id: 1 }, [
            new Node({ id: 2 }, []),
            new Node({ id: 3 }, [
                new Node({ id: 5 }, []),
            ]),
            new Node({ id: 4 }, []),
        ]);

        const newTree = new Node({ id: 1 }, [
            new Node({ id: 2 }, []),
            new Node({ id: 3 }, []),
            new Node({ id: 4 }, []),
        ]);

        const expectedDiffTree = toPlainObject(
            new DiffNode({ id: 1 }, DiffType.NONE, [
                new DiffNode({ id: 2 }, DiffType.NONE, []),
                new DiffNode({ id: 3 }, DiffType.NONE, [
                    new DiffNode({ id: 5 }, DiffType.DELETED, []),
                ]),
                new DiffNode({ id: 4 }, DiffType.NONE, []),
            ])
        );

        checkDiffTreeWithNoModifiedCheck(oldTree, newTree, expectedDiffTree);
    });

    it("can generate a simple move diff", () => {
        const oldTree = new Node({ id: 1 }, [
            new Node({ id: 2 }, []),
            new Node({ id: 3 }, [
                new Node({ id: 5 }, []),
            ]),
            new Node({ id: 4 }, []),
        ]);

        const newTree = new Node({ id: 1 }, [
            new Node({ id: 2 }, []),
            new Node({ id: 3 }, []),
            new Node({ id: 4 }, [
                new Node({ id: 5 }, []),
            ]),
        ]);

        const expectedDiffTree = toPlainObject(
            new DiffNode({ id: 1 }, DiffType.NONE, [
                new DiffNode({ id: 2 }, DiffType.NONE, []),
                new DiffNode({ id: 3 }, DiffType.NONE, [
                    new DiffNode({ id: 5 }, DiffType.DELETED_MOVE, []),
                ]),
                new DiffNode({ id: 4 }, DiffType.NONE, [
                    new DiffNode({ id: 5 }, DiffType.ADDED_MOVE, []),
                ]),
            ])
        );

        checkDiffTreeWithNoModifiedCheck(oldTree, newTree, expectedDiffTree);
    });

    it("can generate a simple modified diff", () => {
        const oldTree = new Node({ id: 1, data: "xyz" }, [
            new Node({ id: 2, data: "abc" }, []),
            new Node({ id: 3, data: "123" }, []),
        ]);

        const newTree = new Node({ id: 1, data: "xyz" }, [
            new Node({ id: 2, data: "def" }, []),
            new Node({ id: 3, data: "123" }, []),
        ]);

        const expectedDiffTree = toPlainObject(
            new DiffNode({ id: 1, data: "xyz" }, DiffType.NONE, [
                new DiffNode({ id: 2, data: "def" }, DiffType.MODIFIED, []),
                new DiffNode({ id: 3, data: "123" }, DiffType.NONE, []),
            ])
        );

        const diffTree = new DiffGenerator(newTree)
            .compareWith(oldTree)
            .withUniqueNodeId(node => node.id)
            .withModifiedCheck(
                (newNode, oldNode) => newNode.data != oldNode.data)
            .generateDiffTree();

        expect(diffTree).toEqual(expectedDiffTree);
    });

    it("can handle move and inner addition diff", () => {
        const oldTree = new Node({ id: 1 }, [
            new Node({ id: 2 }, []),
            new Node({ id: 3 }, [
                new Node({ id: 4 }, []),
            ]),
        ]);

        const newTree = new Node({ id: 1 }, [
            new Node({ id: 2 }, [
                new Node({ id: 4 }, [
                    new Node({ id: 5 }, []),
                ]),
            ]),
            new Node({ id: 3 }, []),
        ]);

        const expectedDiffTree = toPlainObject(
            new DiffNode({ id: 1 }, DiffType.NONE, [
                new DiffNode({ id: 2 }, DiffType.NONE, [
                    new DiffNode({ id: 4 }, DiffType.ADDED_MOVE, [
                        new DiffNode({ id: 5 }, DiffType.ADDED, []),
                    ]),
                ]),
                new DiffNode({ id: 3 }, DiffType.NONE, [
                    new DiffNode({ id: 4 }, DiffType.DELETED_MOVE, []),
                ]),
            ])
        );

        checkDiffTreeWithNoModifiedCheck(oldTree, newTree, expectedDiffTree);
    });

    it("can handle move within same level", () => {
        const oldTree = new Node({ id: 1 }, [
            new Node({ id: 2 }, []),
            new Node({ id: 3 }, []),
        ]);

        const newTree = new Node({ id: 1 }, [
            new Node({ id: 3 }, []),
            new Node({ id: 2 }, []),
        ]);

        const expectedDiffTree = toPlainObject(
            new DiffNode({ id: 1 }, DiffType.NONE, [
                new DiffNode({ id: 3 }, DiffType.NONE, []),
                new DiffNode({ id: 2 }, DiffType.NONE, []),
            ])
        );

        checkDiffTreeWithNoModifiedCheck(oldTree, newTree, expectedDiffTree);
    });

    it("can handle addition within middle of level", () => {
        const oldTree = new Node({ id: 1 }, [
            new Node({ id: 2 }, []),
            new Node({ id: 3 }, []),
        ]);

        const newTree = new Node({ id: 1 }, [
            new Node({ id: 2 }, []),
            new Node({ id: 4 }, []),
            new Node({ id: 3 }, []),
        ]);

        const expectedDiffTree = toPlainObject(
            new DiffNode({ id: 1 }, DiffType.NONE, [
                new DiffNode({ id: 2 }, DiffType.NONE, []),
                new DiffNode({ id: 4 }, DiffType.ADDED, []),
                new DiffNode({ id: 3 }, DiffType.NONE, []),
            ])
        );

        checkDiffTreeWithNoModifiedCheck(oldTree, newTree, expectedDiffTree);
    });

    it("can handle deletion within middle of level", () => {
        const oldTree = new Node({ id: 1 }, [
            new Node({ id: 2 }, []),
            new Node({ id: 3 }, []),
            new Node({ id: 4 }, []),
        ]);

        const newTree = new Node({ id: 1 }, [
            new Node({ id: 2 }, []),
            new Node({ id: 4 }, []),
        ]);

        const expectedDiffTree = toPlainObject(
            new DiffNode({ id: 1 }, DiffType.NONE, [
                new DiffNode({ id: 2 }, DiffType.NONE, []),
                new DiffNode({ id: 3 }, DiffType.DELETED, []),
                new DiffNode({ id: 4 }, DiffType.NONE, []),
            ])
        );

        checkDiffTreeWithNoModifiedCheck(oldTree, newTree, expectedDiffTree);
    });

    it("fully visits deletes nodes", () => {
        const oldTree = new Node({ id: 1 }, [
            new Node({ id: 2 }, [
                new Node({ id: 3 }, [
                    new Node({ id: 4 }, []),
                ]),
            ]),
        ]);

        const newTree = new Node({ id: 1 }, [
            new Node({ id: 2 }, []),
            new Node({ id: 3 }, [
                new Node({ id: 4 }, []),
            ]),
        ]);

        const expectedDiffTree = toPlainObject(
            new DiffNode({ id: 1 }, DiffType.NONE, [
                new DiffNode({ id: 2 }, DiffType.NONE, [
                    new DiffNode({ id: 3 }, DiffType.DELETED_MOVE, [
                        new DiffNode({ id: 4 }, DiffType.DELETED_MOVE, []),
                    ]),
                ]),
                new DiffNode({ id: 3 }, DiffType.ADDED_MOVE, [
                    new DiffNode({ id: 4 }, DiffType.NONE, []),
                ]),
            ])
        );

        checkDiffTreeWithNoModifiedCheck(oldTree, newTree, expectedDiffTree);
    });
});
import { DiffGenerator, DiffType } from "../src/utils/diff.js";
import { Node, DiffNode, toPlainObject } from "./utils/tree.js";

const treeOne = new Node({ id: 1 }, [
    new Node({ id: 2 }, []),
    new Node({ id: 3 }, []),
    new Node({ id: 4 }, []),
]);
const treeTwo = new Node({ id: 1 }, [
    new Node({ id: 2 }, []),
    new Node({ id: 3 }, [
        new Node({ id: 5 }, []),
    ]),
    new Node({ id: 4 }, []),
]);

function checkDiffTreeWithNoModifiedCheck(oldTree, newTree, expectedDiffTree) {
    const diffTree = new DiffGenerator(newTree)
        .compareWith(oldTree)
        .withUniqueNodeId(node => node.id)
        .withModifiedCheck(() => false)
        .generateDiffTree();

    expect(diffTree).toEqual(expectedDiffTree);
}

describe("DiffGenerator", () => {
    it("can generate a simple add diff", () => {
        const oldTree = treeOne;
        const newTree = treeTwo;

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
        const oldTree = treeTwo;
        const newTree = treeOne;

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
        const oldTree = treeTwo;

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
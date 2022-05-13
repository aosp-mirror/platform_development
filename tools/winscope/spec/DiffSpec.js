import { DiffGenerator, DiffType } from "../src/utils/diff.js";
import { NodeBuilder, toPlainObject } from "./utils/tree.js";

const treeOne = new NodeBuilder().setId(1).setChildren([
    new NodeBuilder().setId(2).build(),
    new NodeBuilder().setId(3).build(),
    new NodeBuilder().setId(4).build(),
]).build();

const treeTwo = new NodeBuilder().setId(1).setChildren([
    new NodeBuilder().setId(2).build(),
    new NodeBuilder().setId(3).setChildren([
        new NodeBuilder().setId(5).build(),
    ]).build(),
    new NodeBuilder().setId(4).build(),
]).build();

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
            new NodeBuilder().setId(1).setDiffType(DiffType.NONE).setChildren([
                new NodeBuilder().setId(2).setDiffType(DiffType.NONE).build(),
                new NodeBuilder().setId(3).setDiffType(DiffType.NONE).setChildren([
                    new NodeBuilder().setId(5).setDiffType(DiffType.ADDED).build(),
                ]).build(),
                new NodeBuilder().setId(4).setDiffType(DiffType.NONE).build(),
            ]).build()
        );

        checkDiffTreeWithNoModifiedCheck(oldTree, newTree, expectedDiffTree);
    });

    it("can generate a simple delete diff", () => {
        const oldTree = treeTwo;
        const newTree = treeOne;

        const expectedDiffTree = toPlainObject(
            new NodeBuilder().setId(1).setDiffType(DiffType.NONE).setChildren([
                new NodeBuilder().setId(2).setDiffType(DiffType.NONE).build(),
                new NodeBuilder().setId(3).setDiffType(DiffType.NONE).setChildren([
                    new NodeBuilder().setId(5).setDiffType(DiffType.DELETED).build(),
                ]).build(),
                new NodeBuilder().setId(4).setDiffType(DiffType.NONE).build(),
            ]).build()
        );

        checkDiffTreeWithNoModifiedCheck(oldTree, newTree, expectedDiffTree);
    });

    it("can generate a simple move diff", () => {
        const oldTree = treeTwo;

        const newTree = new NodeBuilder().setId(1).setChildren([
            new NodeBuilder().setId(2).build(),
            new NodeBuilder().setId(3).build(),
            new NodeBuilder().setId(4).setChildren([
                new NodeBuilder().setId(5).build(),
            ]).build(),
        ]).build();

        const expectedDiffTree = toPlainObject(
            new NodeBuilder().setId(1).setDiffType(DiffType.NONE).setChildren([
                new NodeBuilder().setId(2).setDiffType(DiffType.NONE).build(),
                new NodeBuilder().setId(3).setDiffType(DiffType.NONE).setChildren([
                    new NodeBuilder().setId(5).setDiffType(DiffType.DELETED_MOVE).build(),
                ]).build(),
                new NodeBuilder().setId(4).setDiffType(DiffType.NONE).setChildren([
                    new NodeBuilder().setId(5).setDiffType(DiffType.ADDED_MOVE).build(),
                ]).build(),
            ]).build()
        );

        checkDiffTreeWithNoModifiedCheck(oldTree, newTree, expectedDiffTree);
    });

    it("can generate a simple modified diff", () => {
        const oldTree = new NodeBuilder().setId(1).setData("xyz").setChildren([
            new NodeBuilder().setId(2).setData("abc").build(),
            new NodeBuilder().setId(3).setData("123").build(),
        ]).build();

        const newTree = new NodeBuilder().setId(1).setData("xyz").setChildren([
            new NodeBuilder().setId(2).setData("def").build(),
            new NodeBuilder().setId(3).setData("123").build(),
        ]).build();

        const expectedDiffTree = toPlainObject(
            new NodeBuilder().setId(1).setData("xyz").setDiffType(DiffType.NONE).setChildren([
                new NodeBuilder().setId(2).setData("def").setDiffType(DiffType.MODIFIED).build(),
                new NodeBuilder().setId(3).setData("123").setDiffType(DiffType.NONE).build(),
            ]).build()
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
        const oldTree = new NodeBuilder().setId(1).setChildren([
            new NodeBuilder().setId(2).build(),
            new NodeBuilder().setId(3).setChildren([
                new NodeBuilder().setId(4).build(),
            ]).build(),
        ]).build();

        const newTree = new NodeBuilder().setId(1).setChildren([
            new NodeBuilder().setId(2).setChildren([
                new NodeBuilder().setId(4).setChildren([
                    new NodeBuilder().setId(5).build(),
                ]).build(),
            ]).build(),
            new NodeBuilder().setId(3).build(),
        ]).build();

        const expectedDiffTree = toPlainObject(
            new NodeBuilder().setId(1).setDiffType(DiffType.NONE).setChildren([
                new NodeBuilder().setId(2).setDiffType(DiffType.NONE).setChildren([
                    new NodeBuilder().setId(4).setDiffType(DiffType.ADDED_MOVE).setChildren([
                        new NodeBuilder().setId(5).setDiffType(DiffType.ADDED).build(),
                    ]).build(),
                ]).build(),
                new NodeBuilder().setId(3).setDiffType(DiffType.NONE).setChildren([
                    new NodeBuilder().setId(4).setDiffType(DiffType.DELETED_MOVE).build(),
                ]).build(),
            ]).build()
        );

        checkDiffTreeWithNoModifiedCheck(oldTree, newTree, expectedDiffTree);
    });

    it("can handle move within same level", () => {
        const oldTree = new NodeBuilder().setId(1).setChildren([
            new NodeBuilder().setId(2).build(),
            new NodeBuilder().setId(3).build(),
        ]).build();

        const newTree = new NodeBuilder().setId(1).setChildren([
            new NodeBuilder().setId(3).build(),
            new NodeBuilder().setId(2).build(),
        ]).build();

        const expectedDiffTree = toPlainObject(
            new NodeBuilder().setId(1).setDiffType(DiffType.NONE).setChildren([
                new NodeBuilder().setId(3).setDiffType(DiffType.NONE).build(),
                new NodeBuilder().setId(2).setDiffType(DiffType.NONE).build(),
            ]).build()
        );

        checkDiffTreeWithNoModifiedCheck(oldTree, newTree, expectedDiffTree);
    });

    it("can handle addition within middle of level", () => {
        const oldTree = new NodeBuilder().setId(1).setChildren([
            new NodeBuilder().setId(2).build(),
            new NodeBuilder().setId(3).build(),
        ]).build();

        const newTree = new NodeBuilder().setId(1).setChildren([
            new NodeBuilder().setId(2).build(),
            new NodeBuilder().setId(4).build(),
            new NodeBuilder().setId(3).build(),
        ]).build();

        const expectedDiffTree = toPlainObject(
            new NodeBuilder().setId(1).setDiffType(DiffType.NONE).setChildren([
                new NodeBuilder().setId(2).setDiffType(DiffType.NONE).build(),
                new NodeBuilder().setId(4).setDiffType(DiffType.ADDED).build(),
                new NodeBuilder().setId(3).setDiffType(DiffType.NONE).build(),
            ]).build()
        );

        checkDiffTreeWithNoModifiedCheck(oldTree, newTree, expectedDiffTree);
    });

    it("can handle deletion within middle of level", () => {
        const oldTree = new NodeBuilder().setId(1).setChildren([
            new NodeBuilder().setId(2).build(),
            new NodeBuilder().setId(3).build(),
            new NodeBuilder().setId(4).build(),
        ]).build();

        const newTree = new NodeBuilder().setId(1).setChildren([
            new NodeBuilder().setId(2).build(),
            new NodeBuilder().setId(4).build(),
        ]).build();

        const expectedDiffTree = toPlainObject(
            new NodeBuilder().setId(1).setDiffType(DiffType.NONE).setChildren([
                new NodeBuilder().setId(2).setDiffType(DiffType.NONE).build(),
                new NodeBuilder().setId(3).setDiffType(DiffType.DELETED).build(),
                new NodeBuilder().setId(4).setDiffType(DiffType.NONE).build(),
            ]).build()
        );

        checkDiffTreeWithNoModifiedCheck(oldTree, newTree, expectedDiffTree);
    });

    it("fully visits deletes nodes", () => {
        const oldTree = new NodeBuilder().setId(1).setChildren([
            new NodeBuilder().setId(2).setChildren([
                new NodeBuilder().setId(3).setChildren([
                    new NodeBuilder().setId(4).build(),
                ]).build(),
            ]).build(),
        ]).build();

        const newTree = new NodeBuilder().setId(1).setChildren([
            new NodeBuilder().setId(2).build(),
            new NodeBuilder().setId(3).setChildren([
                new NodeBuilder().setId(4).build(),
            ]).build(),
        ]).build();

        const expectedDiffTree = toPlainObject(
            new NodeBuilder().setId(1).setDiffType(DiffType.NONE).setChildren([
                new NodeBuilder().setId(2).setDiffType(DiffType.NONE).setChildren([
                    new NodeBuilder().setId(3).setDiffType(DiffType.DELETED_MOVE).setChildren([
                        new NodeBuilder().setId(4).setDiffType(DiffType.DELETED_MOVE).build(),
                    ]).build(),
                ]).build(),
                new NodeBuilder().setId(3).setDiffType(DiffType.ADDED_MOVE).setChildren([
                    new NodeBuilder().setId(4).setDiffType(DiffType.NONE).build(),
                ]).build(),
            ]).build()
        );

        checkDiffTreeWithNoModifiedCheck(oldTree, newTree, expectedDiffTree);
    });

    it("preserves node chips", () => {
        const oldTree = new NodeBuilder().setId(1).setChildren([
            new NodeBuilder().setId(2).setChips(["CHIP2"]).build(),
            new NodeBuilder().setId(3).build(),
        ]).build();

        const newTree = new NodeBuilder().setId(1).setChildren([
            new NodeBuilder().setId(2).setChips(["CHIP2"]).build(),
            new NodeBuilder().setId(4).setChips(["CHIP4"]).build(),
        ]).build();

        const expectedDiffTree = toPlainObject(
            new NodeBuilder().setId(1).setDiffType(DiffType.NONE).setChildren([
                new NodeBuilder().setId(2).setChips(["CHIP2"]).setDiffType(DiffType.NONE).build(),
                new NodeBuilder().setId(3).setDiffType(DiffType.DELETED).build(),
                new NodeBuilder().setId(4).setChips(["CHIP4"]).setDiffType(DiffType.ADDED).build(),
            ]).build()
        );

        checkDiffTreeWithNoModifiedCheck(oldTree, newTree, expectedDiffTree);
    });
});
class NodeBuilder {
    setId(id) {
        this.id = id;
        this.chips = [];
        return this;
    }

    setChildren(children) {
        this.children = children;
        return this;
    }

    setChips(chips) {
        this.chips = chips;
        return this;
    }

    setDiffType(diffType) {
        this.diffType = diffType;
        return this;
    }

    build() {
        var node = {
            name: undefined,
            stableId: undefined,
            kind: undefined,
            shortName: undefined
        };

        if ('id' in this) {
            node.id = this.id;
        }

        node.children = 'children' in this ? this.children : [];

        if ('chips' in this) {
            node.chips = this.chips;
        }

        if ('diffType' in this) {
            node.diff = { type: this.diffType }
        }

        return node;
    }
}

class Node {
    constructor(nodeDef, children) {
        Object.assign(this, nodeDef);
        this.children = children;
    }
}

class DiffNode extends Node {
    constructor(nodeDef, diffType, children) {
        super(nodeDef, children);
        this.diff = { type: diffType };
        this.name = undefined;
        this.stableId = undefined;
        this.kind = undefined;
        this.shortName = undefined;
    }
}

class ObjNode extends Node {
    constructor(name, children, combined, stableId) {
        const nodeDef = {
            kind: '',
            name: name,
            stableId: stableId,
        };
        if (combined) {
            nodeDef.combined = true;
        }
        super(nodeDef, children);
    }
}

class ObjDiffNode extends ObjNode {
    constructor(name, diffType, children, combined, stableId) {
        super(name, children, combined, stableId);
        this.diff = { type: diffType };
    }
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

export { NodeBuilder, Node, DiffNode, ObjNode, ObjDiffNode, toPlainObject };
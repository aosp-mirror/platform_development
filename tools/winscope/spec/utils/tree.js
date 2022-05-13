class NodeBuilder {
    constructor() {
        this.isTransformed = false;
    }

    setTransformed() {
        this.isTransformed = true;
        return this;
    }

    setId(id) {
        this.id = id;
        this.chips = [];
        this.combined = false;
        return this;
    }

    setStableId(stableId) {
        this.stableId = stableId;
        return this;
    }

    setName(name) {
        this.name = name;
        return this;
    }

    setData(data) {
        this.data = data;
        return this;
    }

    setChips(chips) {
        this.chips = chips;
        return this;
    }

    setCombined() {
        this.combined = true;
        return this;
    }

    setDiffType(diffType) {
        this.diffType = diffType;
        return this;
    }

    setChildren(children) {
        this.children = children;
        return this;
    }

    build() {
        var node = {
            name: undefined,
            shortName: undefined,
            stableId: undefined,
            kind: undefined,
        };

        if (this.isTransformed)
        {
            delete node.shortName;
            node.kind = ''
        }

        if ('id' in this) {
            node.id = this.id;
        }

        if ('stableId' in this) {
            node.stableId = this.stableId;
        }

        if ('name' in this) {
            node.name = this.name;
        }

        if ('data' in this) {
            node.data = this.data;
        }

        if ('chips' in this) {
            node.chips = this.chips;
        }

        if (this.combined) {
            node.combined = true;
        }

        if ('diffType' in this) {
            node.diff = { type: this.diffType };
        }

        node.children = 'children' in this ? this.children : [];

        return node;
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

export { NodeBuilder, toPlainObject };
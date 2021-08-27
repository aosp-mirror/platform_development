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

export { Node, DiffNode, toPlainObject };
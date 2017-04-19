(function (root, factory) {
    if (typeof define === 'function' && define.amd) {
        define([], factory);
    } else if (typeof module === 'object' && module.exports) {
        module.exports = factory();
    } else {
        root.insight = factory();
    }
} (this, function () {
    'use strict';

    let document;
    let strsData, mods, tagIds;
    let domPathInput, domFuzzyMatch;
    let domTBody;

    //--------------------------------------------------------------------------
    // DOM Helper Functions
    //--------------------------------------------------------------------------
    function domNewText(text) {
        return document.createTextNode(text);
    }

    function domNewElem(type) {
        let dom = document.createElement(type);
        for (let i = 1; i < arguments.length; ++i) {
            let arg = arguments[i];
            if (typeof(arg) == 'string' || typeof(arg) == 'number') {
                arg = domNewText(arg)
            }
            dom.appendChild(arg);
        }
        return dom;
    }

    function domNewLink(text, onClick) {
        let dom = domNewElem('a', text);
        dom.setAttribute('href', '#');
        dom.addEventListener('click', onClick);
        return dom;
    }

    //--------------------------------------------------------------------------
    // Module Row
    //--------------------------------------------------------------------------
    function countDeps(deps) {
        let direct = 0;
        let indirect = 0;
        if (deps.length > 0) {
            direct = deps[0].length;
            for (let i = 1; i < deps.length; ++i) {
                indirect += deps[i].length;
            }
        }
        return [direct, indirect];
    }

    function Module(id, modData) {
        this.id = id;
        this.path = strsData[modData[0]];
        this.cls = modData[1];
        this.tagIds = new Set(modData[2]);
        this.deps = modData[3];
        this.users = modData[4];

        [this.numDirectDeps, this.numIndirectDeps] = countDeps(this.deps);
        this.numUsers = this.users.length;

        this.dom = null;
        this.visible = false;

        this.linkDoms = Object.create(null);
    }

    Module.prototype.isTagged = function (tagId) {
        return this.tagIds.has(tagId);
    }

    Module.prototype.createModuleLinkDom = function (mod) {
        let dom = domNewElem('a', mod.path);
        dom.setAttribute('href', '#mod_' + mod.id);
        dom.setAttribute('data-mod-id', mod.id);
        dom.setAttribute('data-owner-id', this.id);
        dom.addEventListener('click', onModuleLinkClicked);
        dom.addEventListener('mouseover', onModuleLinkMouseOver);
        dom.addEventListener('mouseout', onModuleLinkMouseOut);

        this.linkDoms[mod.id] = dom;

        return dom;
    }

    Module.prototype.createModuleRelationsDom = function (parent, label,
                                                          modIds) {
        parent.appendChild(domNewElem('h2', label));

        let domOl = domNewElem('ol');
        parent.appendChild(domOl);
        for (let modId of modIds) {
            domOl.appendChild(
                    domNewElem('li', this.createModuleLinkDom(mods[modId])));
        }
    }

    Module.prototype.createModulePathTdDom = function (parent) {
        parent.appendChild(domNewElem('td', this.createModuleLinkDom(this)));
    }

    Module.prototype.createTagsTdDom = function (parent) {
        let domTd = domNewElem('td');
        for (let tag of this.tagIds) {
            domTd.appendChild(domNewElem('p', strsData[tag]));
        }
        parent.appendChild(domTd);
    }

    Module.prototype.createDepsTdDom = function (parent) {
        let domTd = domNewElem(
                'td', this.numDirectDeps + ' + ' + this.numIndirectDeps);

        let deps = this.deps;
        if (deps.length > 0) {
            this.createModuleRelationsDom(domTd, 'Direct', deps[0]);

            for (let i = 1; i < deps.length; ++i) {
                this.createModuleRelationsDom(domTd, 'Indirect #' + i, deps[i]);
            }
        }

        parent.appendChild(domTd);
    }

    Module.prototype.createUsersTdDom = function (parent) {
        let domTd = domNewElem('td', this.numUsers);

        let users = this.users;
        if (users.length > 0) {
            this.createModuleRelationsDom(domTd, 'Direct', users);
        }

        parent.appendChild(domTd);
    }

    Module.prototype.createDom = function () {
        let dom = this.dom = domNewElem('tr');
        dom.setAttribute('id', 'mod_'  + this.id);

        this.createModulePathTdDom(dom);
        this.createTagsTdDom(dom);
        this.createDepsTdDom(dom);
        this.createUsersTdDom(dom)
    }

    Module.prototype.showDom = function () {
        if (this.visible) {
            return;
        }
        domTBody.appendChild(this.dom);
        this.visible = true;
    }

    Module.prototype.hideDom = function () {
        if (!this.visible) {
            return;
        }
        this.dom.parentNode.removeChild(this.dom);
        this.visible = false;
    }

    function createModulesFromData(stringsData, modulesData) {
        return modulesData.map(function (modData, id) {
            return new Module(id, modData);
        });
    }

    function createTagIdsFromData(stringsData, mods) {
        let tagIds = new Set();
        for (let mod of mods) {
            for (let tag of mod.tagIds) {
                tagIds.add(tag);
            }
        }

        tagIds = Array.from(tagIds);
        tagIds.sort(function (a, b) {
            return strsData[a].localeCompare(strsData[b]);
        });

        return tagIds;
    }

    //--------------------------------------------------------------------------
    // Data
    //--------------------------------------------------------------------------
    function init(doc, stringsData, modulesData) {
        document = doc;
        strsData = stringsData;

        mods = createModulesFromData(stringsData, modulesData);
        tagIds = createTagIdsFromData(stringsData, mods);

        document.addEventListener('DOMContentLoaded', function (evt) {
            createControlDom(document.body);
            createTableDom(document.body);
        });
    }

    //--------------------------------------------------------------------------
    // Control
    //--------------------------------------------------------------------------
    function createControlDom(parent) {
        let domTBody = domNewElem('tbody');

        createSelectionTrDom(domTBody);
        createAddByTagsTrDom(domTBody);
        createAddByPathTrDom(domTBody);

        let domTable = domNewElem('table', domTBody);
        domTable.id = 'control';

        let domFixedLink = domNewElem('a', 'Menu');
        domFixedLink.href = '#control';
        domFixedLink.id = 'control_menu';

        parent.appendChild(domFixedLink);
        parent.appendChild(domTable);
    }

    function createControlMenuTr(parent, label, items) {
        let domUl = domNewElem('ul');
        domUl.className = 'menu';
        for (let [txt, callback] of items) {
            domUl.appendChild(domNewElem('li', domNewLink(txt, callback)));
        }

        let domTr = domNewElem('tr',
                               createControlLabelTdDom(label),
                               domNewElem('td', domUl));

        parent.appendChild(domTr);
    }

    function createSelectionTrDom(parent) {
        const items = [
            ['All', onAddAll],
            ['32-bit', onAddAll32],
            ['64-bit', onAddAll64],
            ['Clear', onClear],
        ];

        createControlMenuTr(parent, 'Selection:', items);
    }

    function createAddByTagsTrDom(parent) {
        if (tagIds.length == 0) {
            return;
        }

        const items = tagIds.map(function (tagId) {
            return [strsData[tagId], function (evt) {
                evt.preventDefault(true);
                showModulesByTagId(tagId);
            }];
        });

        createControlMenuTr(parent, 'Add by Tags:', items);
    }

    function createAddByPathTrDom(parent) {
        let domForm = domNewElem('form');
        domForm.addEventListener('submit', onAddModuleByPath);

        domPathInput = domNewElem('input');
        domPathInput.type = 'text';
        domForm.appendChild(domPathInput);

        let domBtn = domNewElem('input');
        domBtn.type = 'submit';
        domBtn.value = 'Add';
        domForm.appendChild(domBtn);

        domFuzzyMatch = domNewElem('input');
        domFuzzyMatch.setAttribute('id', 'fuzzy_match');
        domFuzzyMatch.setAttribute('type', 'checkbox');
        domFuzzyMatch.setAttribute('checked', 'checked');
        domForm.appendChild(domFuzzyMatch);

        let domFuzzyMatchLabel = domNewElem('label', 'Fuzzy Match');
        domFuzzyMatchLabel.setAttribute('for', 'fuzzy_match');
        domForm.appendChild(domFuzzyMatchLabel);

        let domTr = domNewElem('tr',
                               createControlLabelTdDom('Add by Path:'),
                               domNewElem('td', domForm));

        parent.appendChild(domTr);
    }

    function createControlLabelTdDom(text) {
        return domNewElem('td', domNewElem('strong', text));
    }


    //--------------------------------------------------------------------------
    // Table
    //--------------------------------------------------------------------------
    function createTableDom(parent) {
        domTBody = domNewElem('tbody');
        domTBody.id = 'module_tbody';

        createTableHeaderDom(domTBody);
        createAllModulesDom();
        showAllModules();

        let domTable  = domNewElem('table', domTBody);
        domTable.id = 'module_table';

        parent.appendChild(domTable);
    }

    function createTableHeaderDom(parent) {
        const labels = [
            'Name',
            'Tags',
            'Dependencies (Direct + Indirect)',
            'Users',
        ];

        let domTr = domNewElem('tr');
        for (let label of labels) {
            domTr.appendChild(domNewElem('th', label));
        }

        parent.appendChild(domTr);
    }

    function createAllModulesDom() {
        for (let mod of mods) {
            mod.createDom();
        }
    }

    function hideAllModules() {
        for (let mod of mods) {
            mod.hideDom();
        }
    }

    function showAllModules() {
        for (let mod of mods) {
            mod.showDom();
        }
    }

    function showModulesByFilter(pred) {
        let numMatched = 0;
        for (let mod of mods) {
            if (pred(mod)) {
                mod.showDom();
                ++numMatched;
            }
        }
        return numMatched;
    }

    function showModulesByTagId(tagId) {
        showModulesByFilter(function (mod) {
            return mod.isTagged(tagId);
        });
    }


    //--------------------------------------------------------------------------
    // Events
    //--------------------------------------------------------------------------

    function onAddModuleByPath(evt) {
        evt.preventDefault();

        let path = domPathInput.value;
        domPathInput.value = '';

        function escapeRegExp(pattern) {
            return pattern.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, '\\$1');
        }

        function createFuzzyMatcher() {
            let parts = path.split(/\/+/g);
            let pattern = '';
            for (let part of parts) {
                pattern += escapeRegExp(part) + '(?:/[^\/]*)*';
            }
            pattern = RegExp(pattern);

            return function (mod) {
                return pattern.test(mod.path);
            };
        }

        function exactMatcher(mod) {
            return mod.path == path;
        }

        let numMatched = showModulesByFilter(
            domFuzzyMatch.checked ? createFuzzyMatcher() : exactMatcher);

        if (numMatched == 0) {
            alert('No matching modules: ' + path);
        }
    }

    function onAddAll(evt) {
        evt.preventDefault(true);
        hideAllModules();
        showAllModules();
    }

    function onAddAllClass(evt, cls) {
        evt.preventDefault(true);
        hideAllModules();
        showModulesByFilter(function (mod) {
            return mod.cls == cls;
        });
    }

    function onAddAll32(evt) {
        onAddAllClass(evt, 32);
    }

    function onAddAll64(evt) {
        onAddAllClass(evt, 64);
    }

    function onClear(evt) {
        evt.preventDefault(true);
        hideAllModules();
    }

    function onModuleLinkClicked(evt) {
        let modId = parseInt(evt.target.getAttribute('data-mod-id'), 10);
        mods[modId].showDom();
    }

    function setDirectDepBackgroundColor(modId, ownerId, color) {
        let mod = mods[modId];
        let owner = mods[ownerId];
        let ownerLinkDoms = owner.linkDoms;
        if (mod.deps.length > 0) {
            for (let depId of mod.deps[0]) {
                if (depId in ownerLinkDoms) {
                    ownerLinkDoms[depId].style.backgroundColor = color;
                }
            }
        }
    }

    function onModuleLinkMouseOver(evt) {
        let modId = parseInt(evt.target.getAttribute('data-mod-id'), 10);
        let ownerId = parseInt(evt.target.getAttribute('data-owner-id'), 10);
        setDirectDepBackgroundColor(modId, ownerId, '#ffff00');
    }

    function onModuleLinkMouseOut(evt) {
        let modId = parseInt(evt.target.getAttribute('data-mod-id'), 10);
        let ownerId = parseInt(evt.target.getAttribute('data-owner-id'), 10);
        setDirectDepBackgroundColor(modId, ownerId, 'transparent');
    }

    return {
        'init': init,
    };
}));

(function() {
    'use strict';

    let diameter = 1280;
    let radius = diameter / 2;
    let innerRadius = radius - 240;

    let cluster = d3.cluster();
    cluster.size([ 360, innerRadius ]);

    let line = d3.radialLine();
    line.curve(d3.curveBundle.beta(0.85));
    line.radius(function(d) { return d.y; });
    line.angle(function(d) { return d.x / 180 * Math.PI; });

    let link;
    let node;
    let selectedNode;

    function init() {
        let domListCol = document.createElement("div");
        domListCol.id = "violate_list_column";
        let domGraphCol = document.createElement("div");
        domGraphCol.id = "dep_graph_column";
        let domResetBtn = document.createElement("button");
        domResetBtn.id = "reset_btn";
        domResetBtn.innerHTML = "Reset";
        domGraphCol.appendChild(domResetBtn);

        document.body.appendChild(domListCol);
        document.body.appendChild(domGraphCol);

        let canvas = d3.select("#dep_graph_column").append("svg");
        canvas.attr("width", diameter + 200);
        canvas.attr("height", diameter);

        let svg = canvas.append("g");
        svg.attr("transform", "translate(" + (radius + 100) + "," + radius + ")");

        link = svg.append("g").selectAll(".link");
        node = svg.append("g").selectAll(".node");

        showResult(depData, violatedLibs);
    }

    function showList(depMap, violatedLibs) {
        function makeTitle(tagName) {
            let domTitle = document.createElement("div");
            let domText = document.createElement("h3");
            domText.innerHTML = tagName;
            domTitle.appendChild(domText);
            return domTitle;
        }
        function makeButton(libName, count) {
            let domButton = document.createElement("button");
            domButton.className = "violate";
            domButton.innerHTML = libName + " (" + count + ")";
            domButton.onclick = function() {
                this.classList.toggle("active");
                let currentList = this.nextElementSibling;
                if (currentList.style.display === "block") {
                    currentList.style.display = "none";
                    selectedNode = undefined;
                    resetclicked();
                } else {
                    currentList.style.display = "block";
                    if (selectedNode) {
                        selectedNode.classList.toggle("active");
                        selectedNode.nextElementSibling.style.display = "none";
                    }
                    selectedNode = domButton;
                    mouseclicked(depMap[libName]);
                }
            };
            return domButton;
        }
        function makeList(domList, list)
        {
            for (let i = 0; i < list.length; i++) {
                domList.appendChild(makeButton(list[i][0], list[i][1]));
                let domDepList = document.createElement("div");
                let depItem = depMap[list[i][0]];
                let violates = depItem.data.violates;
                for (let j = 0; j < violates.length; j++) {
                    let domDepLib = document.createElement("div");
                    let tag = depMap[violates[j]].data.tag;
                    domDepLib.className = "violate-list";
                    domDepLib.innerHTML = violates[j] + " ["
                            + tag.substring(tag.lastIndexOf(".") + 1) + "]";
                    domDepList.appendChild(domDepLib);
                }
                domList.appendChild(domDepList);
                domDepList.style.display = "none";
            }
        }

        let domViolatedList = document.getElementById("violate_list_column");
        if ("vendor.private.bin" in violatedLibs) {
            let list = violatedLibs["vendor.private.bin"];
            domViolatedList.appendChild(makeTitle("VENDOR (" + list.length + ")"));
            makeList(domViolatedList, list);
        }
        for (let tag in violatedLibs) {
            if (tag === "vendor.private.bin")
                continue;
            let list = violatedLibs[tag];
            if (tag === "system.private.bin")
                tag = "SYSTEM";
            else
                tag = tag.substring(tag.lastIndexOf(".") + 1).toUpperCase();
            domViolatedList.appendChild(makeTitle(tag + " (" + list.length + ")"));
            makeList(domViolatedList, list);
        }
    }

    function showResult(depDumps, violatedLibs) {
        let root = tagHierarchy(depDumps).sum(function(d) { return 1; });
        cluster(root);

        let libsDepData = libsDepends(root.leaves());
        showList(libsDepData[1], violatedLibs);
        link = link.data(libsDepData[0])
                   .enter()
                   .append("path")
                   .each(function(d) { d.source = d[0], d.target = d[d.length - 1]; })
                   .attr("class", function(d) { return d.allow ? "link" : "link--violate" })
                   .attr("d", line);

        node = node.data(root.leaves())
                   .enter()
                   .append("text")
                   .attr("class",
                       function(d) {
                           return d.data.parent.parent.parent.key == "system" ?
                               (d.data.parent.parent.key == "system.public" ?
                                        "node--sys-pub" :
                                        "node--sys-pri") :
                               "node";
                       })
                   .attr("dy", "0.31em")
                   .attr("transform",
                       function(d) {
                           return "rotate(" + (d.x - 90) + ")translate(" + (d.y + 8) + ",0)" +
                               (d.x < 180 ? "" : "rotate(180)");
                       })
                   .attr("text-anchor", function(d) { return d.x < 180 ? "start" : "end"; })
                   .text(function(d) { return d.data.key; })
                   .on("click", mouseclicked);
        document.getElementById("reset_btn").onclick = resetclicked;
    }

    function resetclicked() {
        if (selectedNode) {
            selectedNode.classList.toggle("active");
            selectedNode.nextElementSibling.style.display = "none";
            selectedNode = undefined;
        }
        link.classed("link--target", false)
            .classed("link--source", false);
        node.classed("node--target", false)
            .classed("node--source", false)
            .classed("node--selected", false);
    }

    function mouseclicked(d) {
        node.each(function(n) { n.target = n.source = false; });

        link.classed("link--target",
                function(l) {
                    if (l.target === d) {
                        l.source.source = true;
                        return true;
                    } else {
                        return false;
                    }
                })
            .classed("link--source",
                function(l) {
                    if (l.source === d) {
                        l.target.target = true;
                        return true;
                    } else {
                        return false;
                    }
                })
            .filter(function(l) { return l.target === d || l.source === d; })
            .raise();

        node.classed("node--target",
                function(n) {
                    return n.target;
                })
            .classed("node--source",
                function(n) { return n.source; })
            .classed("node--selected",
                function(n) {
                    return n === d;
                });
    }

    function tagHierarchy(depDumps) {
        let map = {};

        function find(name, tag, data) {
            let node = map[name], i;
            if (!node) {
                node = map[name] = data || { name : name, children : [] };
                if (name.length) {
                    node.parent = find(tag, tag.substring(0, tag.lastIndexOf(".")));
                    node.parent.children.push(node);
                    node.key = name;
                }
            }
            return node;
        }

        depDumps.forEach(function(d) { find(d.name, d.tag, d); });

        return d3.hierarchy(map[""]);
    }

    function libsDepends(nodes) {
        let map = {}, depends = [];

        // Compute a map from name to node.
        nodes.forEach(function(d) { map[d.data.name] = d; });

        // For each dep, construct a link from the source to target node.
        nodes.forEach(function(d) {
            if (d.data.depends)
                d.data.depends.forEach(function(i) {
                    let l = map[d.data.name].path(map[i]);
                    l.allow = true;
                    depends.push(l);
                });
            if (d.data.violates.length) {
                map[d.data.name].not_allow = true;
                d.data.violates.forEach(function(i) {
                    map[i].not_allow = true;
                    let l = map[d.data.name].path(map[i]);
                    l.allow = false;
                    depends.push(l);
                });
            }
        });

        return [ depends, map ];
    }

    window.onload = init;
})();
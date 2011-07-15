/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var UNIT_WIDTH = 5;

function selectEntry(event) {
    var trace = event.target["traceEntry"];
    var entry = trace[1];
    document.getElementById('eventIndex').textContent = trace[0];
    document.getElementById('eventName').textContent = entry.name;
    document.getElementById('eventId').textContent = entry.messageId;
    document.getElementById('wallTime').textContent = entry.wallTime;
    document.getElementById('threadTime').textContent = entry.threadTime;
}

function displayTraces() {
    var wallContainer = document.getElementById('wallContainer');
    var threadContainer = document.getElementById('threadContainer');

    var totalWidth = 0;

    var x = 0;
    var prevX = 0;
    var prevEnd = traces[0].wallStart;
    
    var color = [
        "DeepSkyBlue",
        "MediumBlue",
        "OrangeRed",
        "GoldenRod",
        "Crimson",
        "Teal",
        "Orchid",
        "Navy",
        "Gold",
        "DarkGreen",
        "DarkOrchid",
    ];
    var colorMap = { };
    
    var prevWidth = 0;

    for (i in traces) {
        var entry = traces[i];

        var margin = Math.round((entry.wallStart - prevEnd) / 4);
        var width = Math.max(1, entry.wallTime) * UNIT_WIDTH;

        element = document.createElement('div');
        element.style.width = width;
        if (margin > 0) element.style.marginLeft = margin;
        element.style.marginRight = 1;
        if (!colorMap[entry.name]) {
            colorMap[entry.name] = color[i % color.length];
        }
        element.style.backgroundColor = colorMap[entry.name];
        element["traceEntry"] = [ i, entry ];
        element.addEventListener("mouseover", selectEntry, false);
        wallContainer.appendChild(element);

        prevX = x;
        prevEnd = entry.wallStart + entry.wallTime;
        x += width + margin + 1;
        var prevWidth = width;

        width = Math.max(1, entry.threadTime) * UNIT_WIDTH;

        element = document.createElement('div');
        element.style.marginLeft = margin;
        element.style.marginRight = prevWidth - width + 1;
        element.style.width = width;
        element.style.backgroundColor = colorMap[entry.name];
        element["traceEntry"] = [ i, entry ];
        element.addEventListener("mouseover", selectEntry, false);
        threadContainer.appendChild(element);
    }

    wallContainer.style.width = x;
    threadContainer.style.width = x;
}

window.onload = displayTraces;

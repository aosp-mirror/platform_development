/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.hierarchyviewer.scene;

import com.android.ddmlib.Device;
import com.android.hierarchyviewer.device.DeviceBridge;
import com.android.hierarchyviewer.device.Window;

import org.openide.util.Exceptions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.Comparator;
import java.util.Stack;
import java.util.regex.Pattern;

public class ViewHierarchyLoader {
    @SuppressWarnings("empty-statement")
    public static ViewHierarchyScene loadScene(Device device, Window window) {
        ViewHierarchyScene scene = new ViewHierarchyScene();

        // Read the views tree
        Socket socket = null;
        BufferedReader in = null;
        BufferedWriter out = null;
        
        String line;
        
        try {
            System.out.println("==> Starting client");
            
            socket = new Socket();
            socket.connect(new InetSocketAddress("127.0.0.1",
                    DeviceBridge.getDeviceLocalPort(device)));

            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("==> DUMP");
            
            out.write("DUMP " + window.encode());
            out.newLine();
            out.flush();
            
            Stack<ViewNode> stack = new Stack<ViewNode>();

            boolean setRoot = true;
            ViewNode lastNode = null;
            int lastWhitespaceCount = Integer.MAX_VALUE;

            while ((line = in.readLine()) != null) {
                if ("DONE.".equalsIgnoreCase(line)) {
                    break;
                }
                
                int whitespaceCount = countFrontWhitespace(line);
                if (lastWhitespaceCount < whitespaceCount) {
                    stack.push(lastNode);
                } else if (!stack.isEmpty()) {
                    final int count = lastWhitespaceCount - whitespaceCount;
                    for (int i = 0; i < count; i++) {
                        stack.pop();
                    }
                }

                lastWhitespaceCount = whitespaceCount;
                line = line.trim();
                int index = line.indexOf(' ');
                
                lastNode = new ViewNode();
                lastNode.name = line.substring(0, index);

                line = line.substring(index + 1);
                loadProperties(lastNode, line);

                scene.addNode(lastNode);
                
                if (setRoot) {
                    scene.setRoot(lastNode);
                    setRoot = false;
                }
                
                if (!stack.isEmpty()) {
                    final ViewNode parent = stack.peek();
                    final String edge = parent.name + lastNode.name;
                    scene.addEdge(edge);
                    scene.setEdgeSource(edge, parent);
                    scene.setEdgeTarget(edge, lastNode);
                    lastNode.parent = parent;
                    parent.children.add(lastNode);
                }
            }

            updateIndices(scene.getRoot());

        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                socket.close();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        
        System.out.println("==> DONE");

        return scene;
    }

    private static void updateIndices(ViewNode root) {
        if (root == null) return;

        root.computeIndex();

        for (ViewNode node : root.children) {
            updateIndices(node);
        }
    }

    private static int countFrontWhitespace(String line) {
        int count = 0;
        while (line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    private static void loadProperties(ViewNode node, String data) {
        int start = 0;
        boolean stop;

        do {
            int index = data.indexOf('=', start);
            ViewNode.Property property = new ViewNode.Property();
            property.name = data.substring(start, index);

            int index2 = data.indexOf(',', index + 1);
            int length = Integer.parseInt(data.substring(index + 1, index2));
            start = index2 + 1 + length;
            property.value = data.substring(index2 + 1, index2 + 1 + length);
            
            node.properties.add(property);
            node.namedProperties.put(property.name, property);

            stop = start >= data.length();
            if (!stop) {
                start += 1;
            }
        } while (!stop);

        Collections.sort(node.properties, new Comparator<ViewNode.Property>() {
            public int compare(ViewNode.Property source, ViewNode.Property destination) {
                return source.name.compareTo(destination.name);
            }
        });

        node.decode();
    }
}

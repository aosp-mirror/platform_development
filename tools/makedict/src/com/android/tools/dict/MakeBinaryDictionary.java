/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.tools.dict;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Compresses a list of words and frequencies into a tree structured binary dictionary.
 */
public class MakeBinaryDictionary {

    public static final int ALPHA_SIZE = 256;

    public static final String TAG_WORD = "w";
    public static final String ATTR_FREQ = "f";

    public static final CharNode EMPTY_NODE = new CharNode();

    List<CharNode> roots;
    Map<String, Integer> mDictionary;
    int mWordCount;
    
    static class CharNode {
        char data;
        int freq;
        boolean terminal;
        List<CharNode> children;
        static int sNodes;

        public CharNode() {
            sNodes++;
        }
    }

    public static void usage() {
        System.err.println("Usage: makedict <src.xml> <dest.dict>");
        System.exit(-1);
    }
    
    public static void main(String[] args) {
        if (args.length < 2) {
            usage();
        } else {
            new MakeBinaryDictionary(args[0], args[1]);
        }
    }

    public MakeBinaryDictionary(String srcFilename, String destFilename) {
        populateDictionary(srcFilename);
        writeToDict(destFilename);
        // Enable the code below to verify that the generated tree is traversable.
        if (false) {
            traverseDict(0, new char[32], 0);
        }
    }
    
    private void populateDictionary(String filename) {
        roots = new ArrayList<CharNode>();
        try {
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(new File(filename), new DefaultHandler() {
                boolean inWord;
                int freq;

                @Override
                public void startElement(String uri, String localName,
                        String qName, Attributes attributes) {
                    if (qName.equals("w")) {
                        inWord = true;
                        freq = Integer.parseInt(attributes.getValue(0));
                    }
                }

                @Override
                public void characters(char[] data, int offset, int length) {
                    // Ignore other whitespace
                    if (!inWord) return;
                    
                    // Ignore one letter words
                    if (length < 2) return;
                    mWordCount++;
                    String word = new String(data, offset, length);
                    addWordTop(word, freq);
                }

                @Override
                public void endElement(String uri, String localName,
                        String qName) {
                    if (qName.equals("w")) inWord = false;
                }
            });
        } catch (Exception ioe) {
            System.err.println("Exception in parsing\n" + ioe);
            ioe.printStackTrace();
        }
        System.out.println("Nodes = " + CharNode.sNodes);
    }

    private int indexOf(List<CharNode> children, char c) {
        if (children == null) {
            return -1;
        }
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i).data == c) {
                return i;
            }
        }
        return -1;
    }

    private void addWordTop(String word, int occur) {
        if (occur > 255) occur = 255;

        char firstChar = word.charAt(0);
        int index = indexOf(roots, firstChar);
        if (index == -1) {
            CharNode newNode = new CharNode();
            newNode.data = firstChar;
            newNode.freq = occur;
            index = roots.size();
            roots.add(newNode);
        } else {
            roots.get(index).freq += occur;
        }
        if (word.length() > 1) {
            addWordRec(roots.get(index), word, 1, occur);
        } else {
            roots.get(index).terminal = true;
        }
    }

    private void addWordRec(CharNode parent, String word, int charAt, int occur) {
        CharNode child = null;
        char data = word.charAt(charAt);
        if (parent.children == null) {
            parent.children = new ArrayList<CharNode>();
        } else {
            for (int i = 0; i < parent.children.size(); i++) {
                CharNode node = parent.children.get(i);
                if (node.data == data) {
                    child = node;
                    break;
                }
            }
        }
        if (child == null) {
            child = new CharNode();
            parent.children.add(child);
        }
        child.data = data;
        child.freq += occur;
        if (word.length() > charAt + 1) {
            addWordRec(child, word, charAt + 1, occur);
        } else {
            child.terminal = true;
            child.freq = occur;
        }
    }

    byte[] dict;
    int dictSize;
    static final int CHAR_WIDTH = 8;
    static final int FLAGS_WIDTH = 1; // Terminal flag (word end)
    static final int ADDR_WIDTH = 23; // Offset to children
    static final int FREQ_WIDTH_BYTES = 1;
    static final int COUNT_WIDTH_BYTES = 1;
    static final int NODE_SIZE_BYTES =
            (CHAR_WIDTH + FLAGS_WIDTH + ADDR_WIDTH) / 8 + FREQ_WIDTH_BYTES;

    private void addCount(int count) {
        dict[dictSize++] = (byte) (0xFF & count);
    }
    
    /* TODO: Allow characters to be beyond the 0-255 range. This is required for some latin 
       language not currently supported */
    private void addNode(CharNode node) {
        int charData = 0xFFFF & node.data;
        if (charData > 254) {
            System.out.println("WARNING: Non-ASCII character encountered : " + node.data +
                    ", value = " + charData);
            dict[dictSize++] = '@';
        } else {
            dict[dictSize++] = (byte) (0xFF & node.data);
        }
        dictSize += 3; // Space for children address
        if ((0xFFFFFF & node.freq) > 255) {
            node.freq = (byte) 255;
        }
        dict[dictSize++] = (byte) (0xFF & node.freq);
    }

    private void updateNodeAddress(int nodeAddress, CharNode node,
            int childrenAddress) {
        childrenAddress = 0x7FFFFF & childrenAddress;
        if (node.terminal) {
            childrenAddress |= 0x800000;
        }
        dict[nodeAddress + 1] = (byte) (childrenAddress >> 16);
        dict[nodeAddress + 2] = (byte) ((childrenAddress & 0xFF00) >> 8);
        dict[nodeAddress + 3] = (byte) ((childrenAddress & 0xFF));
    }

    void writeWordsRec(List<CharNode> children) {
        if (children == null || children.size() == 0) {
            return;
        }
        addCount(children.size());
        int childrenStart = dictSize;
        for (int j = 0; j < children.size(); j++) {
            CharNode node = children.get(j);
            addNode(node);
        }
        for (int j = 0; j < children.size(); j++) {
            CharNode node = children.get(j);
            // TODO: Fix this when child length becomes variable
            int nodeAddress = childrenStart + NODE_SIZE_BYTES * j;
            int cacheDictSize = dictSize;
            writeWordsRec(node.children);
            updateNodeAddress(nodeAddress, node, node.children != null
                    ? cacheDictSize : 0);
        }
    }

    void writeToDict(String dictFilename) {
        // 2MB max
        dict = new byte[2 * 1024 * 1024]; // 2MB upper limit. Actual is probably
                                          // < 1MB in most cases, as there is a limit in the
                                          // resource size in apks.
        dictSize = 0;
        writeWordsRec(roots);
        System.out.println("Dict Size = " + dictSize);
        try {
            FileOutputStream fos = new FileOutputStream(dictFilename);
            fos.write(dict, 0, dictSize);
            fos.close();
        } catch (IOException ioe) {
            System.err.println("Error writing dict file:" + ioe);
        }
    }

    void traverseDict(int pos, char[] word, int depth) {
        int count = dict[pos++] & 0xFF;
        for (int i = 0; i < count; i++) {
            char c = (char) (dict[pos] & 0xFF);
            word[depth] = c;
            if ((dict[pos + 1] & 0x80) > 0) {
                showWord(word, depth + 1, dict[pos + 4] & 0xFF);
            }
            int address =
                    ((dict[pos + 1] & 0x7F) << 16)
                            | ((dict[pos + 2] & 0xFF) << 8)
                            | ((dict[pos + 3] & 0xFF));
            if (address != 0) {
                traverseDict(address, word, depth + 1);
            }
            pos += NODE_SIZE_BYTES;
        }
    }

    void showWord(char[] word, int size, int freq) {
        System.out.print(new String(word, 0, size) + " " + freq + "\n");
    }
}

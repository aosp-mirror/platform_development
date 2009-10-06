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

package com.android.layoutopt.uix;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;

import com.android.layoutopt.uix.xml.XmlDocumentBuilder;
import com.android.layoutopt.uix.rules.Rule;
import com.android.layoutopt.uix.rules.GroovyRule;
import com.android.layoutopt.uix.util.IOUtilities;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

/**
 * Analysis engine used to discover inefficiencies in Android XML
 * layout documents.
 *
 * Anaylizing an Android XML layout produces a list of explicit messages
 * as well as possible solutions. 
 */
public class LayoutAnalyzer {
    private static final String RULES_PREFIX = "rules/";

    private final XmlDocumentBuilder mBuilder = new XmlDocumentBuilder();
    private final List<Rule> mRules = new ArrayList<Rule>();

    /**
     * Creates a new layout analyzer. This constructor takes no argument
     * and will use the default options.
     */
    public LayoutAnalyzer() {
        loadRules();
    }

    private void loadRules() {
        ClassLoader parent = getClass().getClassLoader();
        GroovyClassLoader loader = new GroovyClassLoader(parent);
        GroovyShell shell = new GroovyShell(loader);

        URL jar = getClass().getProtectionDomain().getCodeSource().getLocation();
        ZipFile zip = null;
        try {
            zip = new ZipFile(new File(jar.toURI()));
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().startsWith(RULES_PREFIX)) {
                    loadRule(shell, entry.getName(), zip.getInputStream(entry));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } finally {
            try {
                if (zip != null) zip.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private void loadRule(GroovyShell shell, String name, InputStream stream) {
        try {
            Script script = shell.parse(stream);
            mRules.add(new GroovyRule(name, script));
        } catch (Exception e) {
            System.err.println("Could not load rule " + name + ":");
            e.printStackTrace();
        } finally {
            IOUtilities.close(stream);
        }
    }

    public void addRule(Rule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("A rule must be non-null");
        }
        mRules.add(rule);
    }

    /**
     * Analyzes the specified file.
     *
     * @param file The file to analyze.
     *
     * @return A {@link com.android.layoutopt.uix.LayoutAnalysis} which
     *         cannot be null.
     */
    public LayoutAnalysis analyze(File file) {
        if (file != null && file.exists()) {
            InputStream in = null;
            try {
                in = new FileInputStream(file);
                return analyze(file.getPath(), in);
            } catch (FileNotFoundException e) {
                // Ignore, cannot happen
            } finally {
                IOUtilities.close(in);
            }
        }

        return LayoutAnalysis.ERROR;
    }

    /**
     * Analyzes the specified XML stream.
     *
     * @param stream The stream to analyze.
     * @return A {@link com.android.layoutopt.uix.LayoutAnalysis} which
     *         cannot be null.
     */
    public LayoutAnalysis analyze(InputStream stream) {
        return analyze("<unknown>", stream);
    }

    private LayoutAnalysis analyze(String name, InputStream stream) {
         try {
             Document document = mBuilder.parse(stream);
             return analyze(name, document);
         } catch (SAXException e) {
             // Ignore
         } catch (IOException e) {
             // Ignore
         }
         return LayoutAnalysis.ERROR;
    }

    /**
     * Analyzes the specified XML document.
     *
     * @param content The XML document to analyze.
     *
     * @return A {@link com.android.layoutopt.uix.LayoutAnalysis} which
     *         cannot be null.
     */
    public LayoutAnalysis analyze(String content) {
         return analyze("<unknown>", content);
    }

    /**
     * Analyzes the specified XML document.
     *
     * @param name The name of the document.
     * @param content The XML document to analyze.
     *
     * @return A {@link com.android.layoutopt.uix.LayoutAnalysis} which
     *         cannot be null.
     */
    public LayoutAnalysis analyze(String name, String content) {
         try {
             Document document = mBuilder.parse(content);
             return analyze(name, document);
         } catch (SAXException e) {
             // Ignore
         } catch (IOException e) {
             // Ignore
         }
         return LayoutAnalysis.ERROR;
    }

    /**
     * Analyzes the specified XML document.
     *
     * @param document The XML document to analyze.
     *
     * @return A {@link com.android.layoutopt.uix.LayoutAnalysis} which
     *         cannot be null.
     */
    public LayoutAnalysis analyze(Document document) {
        return analyze("<unknown>", document);
    }

    /**
     * Analyzes the specified XML document.
     *
     * @param name The name of the document.
     * @param document The XML document to analyze.
     *
     * @return A {@link com.android.layoutopt.uix.LayoutAnalysis} which
     *         cannot be null.
     */
    public LayoutAnalysis analyze(String name, Document document) {
        LayoutAnalysis analysis = new LayoutAnalysis(name);

        try {
            Element root = document.getDocumentElement();
            analyze(analysis, root);
        } finally {
            analysis.validate();
        }

        return analysis;        
    }

    private void analyze(LayoutAnalysis analysis, Node node) {
        NodeList list = node.getChildNodes();
        int count = list.getLength();

        applyRules(analysis, node);

        for (int i = 0; i < count; i++) {
            Node child = list.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                analyze(analysis, child);
            }
        }
    }

    private void applyRules(LayoutAnalysis analysis, Node node) {
        analysis.setCurrentNode(node);
        for (Rule rule : mRules) {
            rule.run(analysis, node);
        }
    }
}

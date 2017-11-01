/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.checkcolor.lint;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.resources.ResourceUrl;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.android.SdkConstants.TAG_COLOR;
import static com.android.SdkConstants.TAG_ITEM;
import static com.android.SdkConstants.TAG_STYLE;

/**
 * It contains two phases to detect the hardcode colors
 *
 * Phase 1:
 * 1. Check all the direct hardcode color(#ffffff)
 * 2. Store all the potential indirect hardcode color(Hopefully none)
 *
 * Phase 2:
 * 1. Go through colors.xml, recheck all the indirect hardcoded color
 */
public class HardcodedColorDetector extends ResourceXmlDetector {
    private static final Implementation IMPLEMENTATION = new Implementation(
            HardcodedColorDetector.class,
            Scope.RESOURCE_FILE_SCOPE);

    public static final Issue ISSUE = Issue.create(
            "HardcodedColor",
            "Using hardcoded color",
            "Hardcoded color values are bad because theme changes cannot be uniformly applied." +
            "Instead use the theme specific colors such as `?android:attr/textColorPrimary` in " +
            "attributes.\n" +
            "This ensures that a theme change from a light to a dark theme can be uniformly" +
            "applied across the app.",
            Category.CORRECTNESS,
            4,
            Severity.ERROR,
            IMPLEMENTATION);

    private static final String ERROR_MESSAGE = "Using hardcoded colors is not allowed";

    private Multimap<String, Location.Handle> indirectColorMultiMap;
    private Set<String> hardcodedColorSet;
    private Set<String> skipAttributes;

    public HardcodedColorDetector() {
        indirectColorMultiMap = ArrayListMultimap.create();
        skipAttributes = new HashSet<>();
        hardcodedColorSet = new HashSet<>();

        skipAttributes.add("fillColor");
        skipAttributes.add("strokeColor");
        skipAttributes.add("text");
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.LAYOUT || folderType == ResourceFolderType.VALUES
                || folderType == ResourceFolderType.DRAWABLE;
    }

    @Override
    public Collection<String> getApplicableAttributes() {
        return ALL;
    }

    @Override
    @Nullable
    public Collection<String> getApplicableElements() {
        return Arrays.asList(TAG_STYLE, TAG_COLOR);
    }

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        if (!LintUtils.isEnglishResource(context, true)) {
            return;
        }

        final String value = attribute.getValue();
        final ResourceUrl resUrl = ResourceUrl.parse(value);
        if (!skipAttributes.contains(attribute.getLocalName()) && resUrl == null
                && isHardcodedColor(value)) {
            // TODO: check whether the attr is valid to store the color
            if (context.isEnabled(ISSUE)) {
                context.report(ISSUE, attribute, context.getLocation(attribute),
                        ERROR_MESSAGE);
            }
        } else if (resUrl != null && resUrl.type == ResourceType.COLOR && !resUrl.theme) {
            addIndirectColor(context, value, attribute);
        }
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        if (context.getResourceFolderType() != ResourceFolderType.VALUES) {
            return;
        }

        if (!LintUtils.isEnglishResource(context, true)) {
            return;
        }

        final int phase = context.getPhase();
        final String tagName = element.getTagName();
        if (tagName.equals(TAG_STYLE)) {
            final List<Element> itemNodes = LintUtils.getChildren(element);
            for (Element childElement : itemNodes) {
                if (childElement.getNodeType() == Node.ELEMENT_NODE &&
                        TAG_ITEM.equals(childElement.getNodeName())) {
                    final NodeList childNodes = childElement.getChildNodes();
                    for (int i = 0, n = childNodes.getLength(); i < n; i++) {
                        final Node child = childNodes.item(i);
                        if (child.getNodeType() != Node.TEXT_NODE) {
                            break;
                        }

                        final String value = child.getNodeValue();
                        final ResourceUrl resUrl = ResourceUrl.parse(value);
                        if (resUrl == null && isHardcodedColor(value)) {
                            // TODO: check whether the node is valid to store the color
                            context.report(ISSUE, childElement, context.getLocation(child),
                                    ERROR_MESSAGE);
                        } else if (resUrl != null && resUrl.type == ResourceType.COLOR
                                && !resUrl.theme) {
                            addIndirectColor(context, value, child);
                        }
                    }
                }
            }
        } else if (tagName.equals(TAG_COLOR)) {
            final String name = element.getAttribute(SdkConstants.ATTR_NAME);
            final String value = element.getFirstChild().getNodeValue();
            if (isHardcodedColor(value) && context.isEnabled(ISSUE)) {
                context.report(ISSUE, element, context.getLocation(element),
                        ERROR_MESSAGE);

                final String fullColorName = "@color/" + name;
                hardcodedColorSet.add(fullColorName);
            }
        }
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        super.afterCheckProject(context);

        if (context.isEnabled(ISSUE)) {
            for (final String fullColorName : hardcodedColorSet) {
                if (indirectColorMultiMap.containsKey(fullColorName)) {
                    for (Location.Handle handle : indirectColorMultiMap.get(fullColorName)) {
                        context.report(ISSUE, handle.resolve(), ERROR_MESSAGE);
                    }
                }
            }
        }
    }

    /**
     * Test whether {@paramref color} is the hardcoded color using the regex match.
     * The hex hardcoded color has three types.
     * 1. #RGB e.g #fff
     * 2. #RRGGBB e.g #ffffff
     * 3. #AARRGGBB e.g #ffffffff
     *
     * @param color name of the color
     * @return whether it is hardcoded color
     */
    private boolean isHardcodedColor(String color) {
        return color.matches("#[0-9a-fA-F]{3}")
                || color.matches("#[0-9a-fA-F]{6}")
                || color.matches("#[0-9a-fA-F]{8}");
    }

    /**
     * Add indirect color for further examination. For example, in layout file we don't know
     * whether "@color/color_to_examine" is the hardcoded color. So I store the name and location
     * first
     * @param context used to create the location handle
     * @param color name of the indirect color
     * @param node postion in xml file
     */
    private void addIndirectColor(XmlContext context, String color, Node node) {
        final Location.Handle handle = context.createLocationHandle(node);
        handle.setClientData(node);

        indirectColorMultiMap.put(color, handle);
    }
}
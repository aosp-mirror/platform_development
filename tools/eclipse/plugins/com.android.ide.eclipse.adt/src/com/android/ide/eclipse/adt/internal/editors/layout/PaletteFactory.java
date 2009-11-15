/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.internal.editors.layout;

import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetData;

import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.PaletteTemplateEntry;

import java.util.List;

/**
 * Factory that creates the palette for the {@link GraphicalLayoutEditor}.
 *
 * @since GLE1
 */
public class PaletteFactory {

    /** Static factory, nothing to instantiate here. */
    private PaletteFactory() {
    }

    public static PaletteRoot createPaletteRoot(PaletteRoot currentPalette,
            AndroidTargetData targetData) {

        if (currentPalette == null) {
            currentPalette = new PaletteRoot();
        }

        for (int n = currentPalette.getChildren().size() - 1; n >= 0; n--) {
            currentPalette.getChildren().remove(n);
        }

        if (targetData != null) {
            addTools(currentPalette);
            addViews(currentPalette, "Layouts",
                    targetData.getLayoutDescriptors().getLayoutDescriptors());
            addViews(currentPalette, "Views",
                    targetData.getLayoutDescriptors().getViewDescriptors());
        }

        return currentPalette;
    }

    private static void addTools(PaletteRoot paletteRoot) {
        PaletteGroup group = new PaletteGroup("Tools");

        // Default tools: selection.
        // Do not use the MarqueeToolEntry since we don't support multiple selection.
        /* -- Do not put the selection tool. It's the unique tool so it looks useless.
              Leave this piece of code here in case we want it back later.
        PanningSelectionToolEntry entry = new PanningSelectionToolEntry();
        group.add(entry);
        paletteRoot.setDefaultEntry(entry);
        */

        paletteRoot.add(group);
    }

    private static void addViews(PaletteRoot paletteRoot, String groupName,
            List<ElementDescriptor> descriptors) {
        PaletteDrawer group = new PaletteDrawer(groupName);

        for (ElementDescriptor desc : descriptors) {
            PaletteTemplateEntry entry = new PaletteTemplateEntry(
                    desc.getUiName(),           // label
                    desc.getTooltip(),          // short description
                    desc,                       // template
                    desc.getImageDescriptor(),  // small icon
                    desc.getImageDescriptor()   // large icon
                    );

            group.add(entry);
        }

        paletteRoot.add(group);
    }
}

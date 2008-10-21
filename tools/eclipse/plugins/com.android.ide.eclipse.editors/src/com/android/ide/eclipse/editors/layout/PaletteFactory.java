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

package com.android.ide.eclipse.editors.layout;

import com.android.ide.eclipse.editors.EditorsPlugin;
import com.android.ide.eclipse.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.editors.layout.descriptors.LayoutDescriptors;

import org.eclipse.gef.palette.CombinedTemplateCreationEntry;
import org.eclipse.gef.palette.MarqueeToolEntry;
import org.eclipse.gef.palette.PaletteDrawer;
import org.eclipse.gef.palette.PaletteGroup;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.palette.PanningSelectionToolEntry;
import org.eclipse.gef.requests.CreationFactory;

import java.util.List;

/**
 * Factory that creates the palette for the {@link GraphicalLayoutEditor}.
 */
public class PaletteFactory {

    private static PaletteRoot sPaletteRoot;
    private static Runnable sSdkChangedListener;

    /** Static factory, nothing to instantiate here. */
    private PaletteFactory() {
    }

    public static PaletteRoot createPaletteRoot() {
        if (sSdkChangedListener == null) {
            sSdkChangedListener = new Runnable() {
                public void run() {
                    if (sPaletteRoot != null) {
                        // The SDK has changed. Remove the drawer groups and rebuild them.
                        for (int n = sPaletteRoot.getChildren().size() - 1; n >= 0; n--) {
                            sPaletteRoot.getChildren().remove(n);
                        }
                        
                        addTools(sPaletteRoot);
                        addViews(sPaletteRoot, "Layouts",
                                LayoutDescriptors.getInstance().getLayoutDescriptors());
                        addViews(sPaletteRoot, "Views",
                                LayoutDescriptors.getInstance().getViewDescriptors());
                    }
                }
            };
            EditorsPlugin.getDefault().addResourceChangedListener(sSdkChangedListener);
        }
        
        if (sPaletteRoot == null) {
            sPaletteRoot = new PaletteRoot();
            sSdkChangedListener.run();
        }
        return sPaletteRoot;
    }

    private static void addTools(PaletteRoot paletteRoot) {
        PaletteGroup group = new PaletteGroup("Tools");
        
        // Default tools: selection and marquee selection
        PanningSelectionToolEntry entry = new PanningSelectionToolEntry();
        group.add(entry);
        paletteRoot.setDefaultEntry(entry);

        group.add(new MarqueeToolEntry());
        
        paletteRoot.add(group);
    }

    private static void addViews(PaletteRoot paletteRoot, String groupName,
            List<ElementDescriptor> descriptors) {
        PaletteDrawer group = new PaletteDrawer(groupName);
        
        for (ElementDescriptor desc : descriptors) {
            CombinedTemplateCreationEntry entry = new CombinedTemplateCreationEntry(
                    desc.getUiName(),           // label
                    desc.getTooltip(),          // short description
                    desc.getClass(),            // template
                    new ElementFactory(desc),   // factory
                    desc.getImageDescriptor(),  // small icon
                    desc.getImageDescriptor()   // large icon
                    );
            group.add(entry);
        }
        
        paletteRoot.add(group);
    }

    //------------------------------------------

    public static class ElementFactory implements CreationFactory {

        private final ElementDescriptor mDescriptor;

        public ElementFactory(ElementDescriptor descriptor) {
            mDescriptor = descriptor;
        }

        public Object getNewObject() {
            return mDescriptor;
        }

        public Object getObjectType() {
            return mDescriptor;
        }

    }
}

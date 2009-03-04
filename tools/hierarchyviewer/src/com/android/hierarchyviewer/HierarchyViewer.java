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

package com.android.hierarchyviewer;

import com.android.hierarchyviewer.ui.Workspace;
import com.android.hierarchyviewer.device.DeviceBridge;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class HierarchyViewer {
    private static final CharSequence OS_WINDOWS = "Windows";
    private static final CharSequence OS_MACOSX = "Mac OS X";

    private static void initUserInterface() {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("apple.awt.brushMetalLook", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "HierarchyViewer");

        final String os = System.getProperty("os.name");

        try {
            if (os.contains(OS_WINDOWS) || os.contains(OS_MACOSX)) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } else {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());                
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        initUserInterface();
        DeviceBridge.initDebugBridge();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Workspace workspace = new Workspace();
                workspace.setDefaultCloseOperation(Workspace.EXIT_ON_CLOSE);
                workspace.setLocationRelativeTo(null);
                workspace.setVisible(true);
            }
        });
    }
}

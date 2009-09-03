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

package com.android.ide.eclipse.adt.internal.project;

import com.android.ide.eclipse.adt.AndroidConstants;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Export helper for project.
 */
public final class ExportHelper {

    private static IExportCallback sCallback;

    public interface IExportCallback {
        void startExportWizard(IProject project);
    }

    public static void setCallback(IExportCallback callback) {
        sCallback = callback;
    }

    public static void startExportWizard(IProject project) {
        if (sCallback != null) {
            sCallback.startExportWizard(project);
        }
    }

    /**
     * Exports an <b>unsigned</b> version of the application created by the given project.
     * @param project the project to export
     */
    public static void exportProject(IProject project) {
        Shell shell = Display.getCurrent().getActiveShell();

        // get the java project to get the output directory
        IFolder outputFolder = BaseProjectHelper.getOutputFolder(project);
        if (outputFolder != null) {
            IPath binLocation = outputFolder.getLocation();

            // make the full path to the package
            String fileName = project.getName() + AndroidConstants.DOT_ANDROID_PACKAGE;

            File file = new File(binLocation.toOSString() + File.separator + fileName);

            if (file.exists() == false || file.isFile() == false) {
                MessageDialog.openError(Display.getCurrent().getActiveShell(),
                        "Android IDE Plug-in",
                        String.format("Failed to export %1$s: %2$s doesn't exist!",
                                project.getName(), file.getPath()));
                return;
            }

            // ok now pop up the file save window
            FileDialog fileDialog = new FileDialog(shell, SWT.SAVE);

            fileDialog.setText("Export Project");
            fileDialog.setFileName(fileName);

            String saveLocation = fileDialog.open();
            if (saveLocation != null) {
                // get the stream from the original file

                ZipInputStream zis = null;
                ZipOutputStream zos = null;
                FileInputStream input = null;
                FileOutputStream output = null;

                try {
                    input = new FileInputStream(file);
                    zis = new ZipInputStream(input);

                    // get an output stream into the new file
                    File saveFile = new File(saveLocation);
                    output = new FileOutputStream(saveFile);
                    zos = new ZipOutputStream(output);
                } catch (FileNotFoundException e) {
                    // only the input/output stream are throwing this exception.
                    // so we only have to close zis if output is the one that threw.
                    if (zis != null) {
                        try {
                            zis.close();
                        } catch (IOException e1) {
                            // pass
                        }
                    }

                    MessageDialog.openError(shell, "Android IDE Plug-in",
                            String.format("Failed to export %1$s: %2$s doesn't exist!",
                                    project.getName(), file.getPath()));
                    return;
                }

                try {
                    ZipEntry entry;

                    byte[] buffer = new byte[4096];

                    while ((entry = zis.getNextEntry()) != null) {
                        String name = entry.getName();

                        // do not take directories or anything inside the META-INF folder since
                        // we want to strip the signature.
                        if (entry.isDirectory() || name.startsWith("META-INF/")) { //$NON-NL1$
                            continue;
                        }

                        ZipEntry newEntry;

                        // Preserve the STORED method of the input entry.
                        if (entry.getMethod() == JarEntry.STORED) {
                            newEntry = new JarEntry(entry);
                        } else {
                            // Create a new entry so that the compressed len is recomputed.
                            newEntry = new JarEntry(name);
                        }

                        // add the entry to the jar archive
                        zos.putNextEntry(newEntry);

                        // read the content of the entry from the input stream, and write it into the archive.
                        int count;
                        while ((count = zis.read(buffer)) != -1) {
                            zos.write(buffer, 0, count);
                        }

                        // close the entry for this file
                        zos.closeEntry();
                        zis.closeEntry();
                    }

                } catch (IOException e) {
                    MessageDialog.openError(shell, "Android IDE Plug-in",
                            String.format("Failed to export %1$s: %2$s",
                                    project.getName(), e.getMessage()));
                } finally {
                    try {
                        zos.close();
                    } catch (IOException e) {
                        // pass
                    }
                    try {
                        zis.close();
                    } catch (IOException e) {
                        // pass
                    }
                }

                // this is unsigned export. Let's tell the developers to run zip align
                MessageDialog.openWarning(shell, "Android IDE Plug-in", String.format(
                        "An unsigned package of the application was saved at\n%1$s\n\n" +
                        "Before publishing the application you will need to:\n" +
                        "- Sign the application with your release key,\n" +
                        "- run zipalign on the signed package. ZipAlign is located in <SDK>/tools/\n\n" +
                        "Aligning applications allows Android to use application resources\n" +
                        "more efficiently.", saveLocation));

            }
        } else {
            MessageDialog.openError(shell, "Android IDE Plug-in",
                    String.format("Failed to export %1$s: Could not get project output location",
                            project.getName()));
        }
    }
}

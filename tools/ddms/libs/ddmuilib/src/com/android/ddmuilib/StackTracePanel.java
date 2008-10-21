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

package com.android.ddmuilib;

import com.android.ddmlib.Client;
import com.android.ddmlib.IStackTraceInfo;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

/**
 * Stack Trace Panel.
 * <p/>This is not a panel in the regular sense. Instead this is just an object around the creation
 * and management of a Stack Trace display.
 * <p/>UI creation is done through
 * {@link #createPanel(Composite, String, String, String, String, String, IPreferenceStore)}.
 *
 */
public final class StackTracePanel {

    private static ISourceRevealer sSourceRevealer;

    private Table mStackTraceTable;
    private TableViewer mStackTraceViewer;

    private Client mCurrentClient;
    
    
    /**
     * Content Provider to display the stack trace of a thread.
     * Expected input is a {@link IStackTraceInfo} object.
     */
    private static class StackTraceContentProvider implements IStructuredContentProvider {
        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof IStackTraceInfo) {
                // getElement cannot return null, so we return an empty array
                // if there's no stack trace
                StackTraceElement trace[] = ((IStackTraceInfo)inputElement).getStackTrace();
                if (trace != null) {
                    return trace;
                }
            }

            return new Object[0];
        }

        public void dispose() {
            // pass
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // pass
        }
    }
    

    /**
     * A Label Provider to use with {@link StackTraceContentProvider}. It expects the elements to be
     * of type {@link StackTraceElement}.
     */
    private static class StackTraceLabelProvider implements ITableLabelProvider {

        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            if (element instanceof StackTraceElement) {
                StackTraceElement traceElement = (StackTraceElement)element;
                switch (columnIndex) {
                    case 0:
                        return traceElement.getClassName();
                    case 1:
                        return traceElement.getMethodName();
                    case 2:
                        return traceElement.getFileName();
                    case 3:
                        return Integer.toString(traceElement.getLineNumber());
                    case 4:
                        return Boolean.toString(traceElement.isNativeMethod());
                }
            }

            return null;
        }

        public void addListener(ILabelProviderListener listener) {
            // pass
        }

        public void dispose() {
            // pass
        }

        public boolean isLabelProperty(Object element, String property) {
            // pass
            return false;
        }

        public void removeListener(ILabelProviderListener listener) {
            // pass
        }
    }
    
    /**
     * Classes which implement this interface provide a method that is able to reveal a method
     * in a source editor
     */
    public interface ISourceRevealer {
        /**
         * Sent to reveal a particular line in a source editor
         * @param applicationName the name of the application running the source.
         * @param className the fully qualified class name
         * @param line the line to reveal
         */
        public void reveal(String applicationName, String className, int line);
    }
    
    
    /**
     * Sets the {@link ISourceRevealer} object able to reveal source code in a source editor.
     * @param revealer
     */
    public static void setSourceRevealer(ISourceRevealer revealer) {
        sSourceRevealer = revealer;
    }
    
    /**
     * Creates the controls for the StrackTrace display.
     * <p/>This method will set the parent {@link Composite} to use a {@link GridLayout} with
     * 2 columns.
     * @param parent the parent composite.
     * @param prefs_stack_col_class 
     * @param prefs_stack_col_method 
     * @param prefs_stack_col_file 
     * @param prefs_stack_col_line 
     * @param prefs_stack_col_native 
     * @param store
     */
    public Table createPanel(Composite parent, String prefs_stack_col_class,
            String prefs_stack_col_method, String prefs_stack_col_file, String prefs_stack_col_line,
            String prefs_stack_col_native, IPreferenceStore store) {
        
        mStackTraceTable = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION);
        mStackTraceTable.setHeaderVisible(true);
        mStackTraceTable.setLinesVisible(true);
        
        TableHelper.createTableColumn(
                mStackTraceTable,
                "Class",
                SWT.LEFT,
                "SomeLongClassName", //$NON-NLS-1$
                prefs_stack_col_class, store);

        TableHelper.createTableColumn(
                mStackTraceTable,
                "Method",
                SWT.LEFT,
                "someLongMethod", //$NON-NLS-1$
                prefs_stack_col_method, store);

        TableHelper.createTableColumn(
                mStackTraceTable,
                "File",
                SWT.LEFT,
                "android/somepackage/someotherpackage/somefile.class", //$NON-NLS-1$
                prefs_stack_col_file, store);

        TableHelper.createTableColumn(
                mStackTraceTable,
                "Line",
                SWT.RIGHT,
                "99999", //$NON-NLS-1$
                prefs_stack_col_line, store);

        TableHelper.createTableColumn(
                mStackTraceTable,
                "Native",
                SWT.LEFT,
                "Native", //$NON-NLS-1$
                prefs_stack_col_native, store);
        
        mStackTraceViewer = new TableViewer(mStackTraceTable);
        mStackTraceViewer.setContentProvider(new StackTraceContentProvider());
        mStackTraceViewer.setLabelProvider(new StackTraceLabelProvider());
        
        mStackTraceViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                if (sSourceRevealer != null && mCurrentClient != null) {
                    // get the selected stack trace element
                    ISelection selection = mStackTraceViewer.getSelection();
                    
                    if (selection instanceof IStructuredSelection) {
                        IStructuredSelection structuredSelection = (IStructuredSelection)selection;
                        Object object = structuredSelection.getFirstElement();
                        if (object instanceof StackTraceElement) {
                            StackTraceElement traceElement = (StackTraceElement)object;
                            
                            if (traceElement.isNativeMethod() == false) {
                                sSourceRevealer.reveal(
                                        mCurrentClient.getClientData().getClientDescription(), 
                                        traceElement.getClassName(),
                                        traceElement.getLineNumber());
                            }
                        }
                    }
                }
            }
        });

        return mStackTraceTable;
    }
    
    /**
     * Sets the input for the {@link TableViewer}.
     * @param input the {@link IStackTraceInfo} that will provide the viewer with the list of
     * {@link StackTraceElement}
     */
    public void setViewerInput(IStackTraceInfo input) {
        mStackTraceViewer.setInput(input);
        mStackTraceViewer.refresh();
    }
    
    /**
     * Sets the current client running the stack trace.
     * @param currentClient the {@link Client}.
     */
    public void setCurrentClient(Client currentClient) {
        mCurrentClient = currentClient;
    }
}

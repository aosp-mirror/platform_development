/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.monkeyrunner;

import com.google.common.collect.Sets;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

/**
 * Swing Frame that displays all the variables that the monkey exposes on the device.
 */
public class VariableFrame extends JFrame {
    private static final Logger LOG = Logger.getLogger(VariableFrame.class.getName());
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private MonkeyManager monkeyManager;

    private static class VariableHolder implements Comparable<VariableHolder> {
        private final String key;
        private final String value;

        public VariableHolder(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public int compareTo(VariableHolder o) {
            return key.compareTo(o.key);
        }
    }

    private static <E> E getNthElement(Set<E> set, int offset) {
        int current = 0;
        for (E elem : set) {
            if (current == offset) {
                return elem;
            }
            current++;
        }
        return null;
    }

    private class VariableTableModel extends AbstractTableModel {
        private final TreeSet<VariableHolder> set = Sets.newTreeSet();

        public void refresh() {
            Collection<String> variables;
            try {
                variables = monkeyManager.listVariable();
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Error getting list of variables", e);
                return;
            }
            for (final String variable : variables) {
                EXECUTOR.execute(new Runnable() {
                    public void run() {
                        String value;
                        try {
                            value = monkeyManager.getVariable(variable);
                        } catch (IOException e) {
                            LOG.log(Level.SEVERE,
                                    "Error getting variable value for " + variable, e);
                            return;
                        }
                        synchronized (set) {
                            set.add(new VariableHolder(variable, value));
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    VariableTableModel.this.fireTableDataChanged();
                                }
                            });

                        }
                    }
                });
            }
        }

        public int getColumnCount() {
            return 2;
        }

        public int getRowCount() {
            synchronized (set) {
                return set.size();
            }
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            VariableHolder nthElement;
            synchronized (set) {
                nthElement = getNthElement(set, rowIndex);
            }
            if (columnIndex == 0) {
                return nthElement.getKey();
            }
            return nthElement.getValue();
        }
    }

    public VariableFrame() {
        super("Variables");
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        final VariableTableModel tableModel = new VariableTableModel();

        JButton refreshButton = new JButton("Refresh");
        add(refreshButton);
        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tableModel.refresh();
            }
        });


        JTable table = new JTable(tableModel);
        add(table);

        tableModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                pack();
            }
        });

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                tableModel.refresh();
            }
        });

        pack();
    }

    public void init(MonkeyManager monkeyManager) {
        this.monkeyManager = monkeyManager;
    }
}

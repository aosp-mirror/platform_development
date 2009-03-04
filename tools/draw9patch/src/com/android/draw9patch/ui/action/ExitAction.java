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

package com.android.draw9patch.ui.action;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.JFrame;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.Toolkit;

public class ExitAction extends AbstractAction {
    public static final String ACTION_NAME = "exit";
    private JFrame frame;

    public ExitAction(JFrame frame) {
        putValue(NAME, "Quit");
        putValue(SHORT_DESCRIPTION, "Quit");
        putValue(LONG_DESCRIPTION, "Quit");
        putValue(MNEMONIC_KEY, KeyEvent.VK_Q);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        this.frame = frame;
    }

    public void actionPerformed(ActionEvent e) {
        frame.dispose();
        System.exit(0);
    }
}

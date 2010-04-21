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

import com.android.ddmlib.IDevice;
import com.android.ddmlib.RawImage;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Main window for MonkeyController.
 */
public class MonkeyControllerFrame extends JFrame {
    private static final Logger LOG = Logger.getLogger(MonkeyControllerFrame.class.getName());

    private final DebugBridge adb;

    private final JButton refreshButton = new JButton("Refresh");
    private final JButton variablesButton = new JButton("Variable");
    private final JLabel imageLabel = new JLabel();
    private final VariableFrame variableFrame;

    private IDevice preferredDevice;
    private MonkeyManager monkeyManager;
    private BufferedImage currentImage;

    private final Timer timer = new Timer(1000, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            updateScreen();
        }
    });

    private class PressAction extends AbstractAction {
        private final PhysicalButton button;

        public PressAction(PhysicalButton button) {
            this.button = button;
        }
        public void actionPerformed(ActionEvent event) {
            try {
                monkeyManager.press(button);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            updateScreen();
        }
    }

    private JButton createToolbarButton(PhysicalButton hardButton) {
        JButton button = new JButton(new PressAction(hardButton));
        button.setText(hardButton.getKeyName());
        return button;
    }

    public MonkeyControllerFrame(DebugBridge adb) {
        super("MonkeyController");

        this.adb = adb;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        JToolBar toolbar = new JToolBar();

        toolbar.add(createToolbarButton(PhysicalButton.HOME));
        toolbar.add(createToolbarButton(PhysicalButton.BACK));
        toolbar.add(createToolbarButton(PhysicalButton.SEARCH));
        toolbar.add(createToolbarButton(PhysicalButton.MENU));

        add(toolbar);
        add(refreshButton);
        add(variablesButton);
        add(imageLabel);

        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateScreen();
            }
        });

        variableFrame = new VariableFrame();
        variablesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                variableFrame.setVisible(true);
            }
        });

        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                try {
                    monkeyManager.touch(event.getX(), event.getY());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                updateScreen();
            }

        });

        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addKeyEventDispatcher(new KeyEventDispatcher() {
            public boolean dispatchKeyEvent(KeyEvent event) {
                if (KeyEvent.KEY_TYPED == event.getID()) {
                    try {
                        monkeyManager.type(event.getKeyChar());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return false;
            }
        });

        SwingUtilities.invokeLater(new Runnable(){
            public void run() {
                init();
                variableFrame.init(monkeyManager);
            }
        });

        pack();
    }

    private void updateScreen() {
        RawImage screenshot;
        try {
            screenshot = preferredDevice.getScreenshot();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Error getting screenshot", e);
            throw new RuntimeException(e);
        }

        currentImage = ImageUtils.convertImage(screenshot, currentImage);
        imageLabel.setIcon(new ImageIcon(currentImage));

        pack();
    }

    private void init() {
        preferredDevice = adb.getPreferredDevice();
        monkeyManager = new MonkeyManager(preferredDevice);
        updateScreen();
        timer.start();
    }

}

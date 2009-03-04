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

package com.android.hierarchyviewer.laf;

import javax.swing.border.AbstractBorder;
import java.awt.*;

public class UnifiedContentBorder extends AbstractBorder {
    private static final Color BORDER_TOP_COLOR1 = new Color(0x575757);
    private static final Color BORDER_BOTTOM_COLOR1 = new Color(0x404040);
    private static final Color BORDER_BOTTOM_COLOR2 = new Color(0xd8d8d8);

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        g.setColor(BORDER_TOP_COLOR1);
        g.drawLine(x, y, x + width, y);
        g.setColor(BORDER_BOTTOM_COLOR1);
        g.drawLine(x, y + height - 2, x + width, y + height - 2);
        g.setColor(BORDER_BOTTOM_COLOR2);
        g.drawLine(x, y + height - 1, x + width, y + height - 1);
    }

    public Insets getBorderInsets(Component component) {
        return new Insets(1, 0, 2, 0);
    }

    public boolean isBorderOpaque() {
        return true;
    }
}

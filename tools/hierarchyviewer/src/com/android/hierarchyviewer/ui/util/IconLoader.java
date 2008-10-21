package com.android.hierarchyviewer.ui.util;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsConfiguration;

public class IconLoader {
    public static Icon load(Class<?> klass, String path) {
        try {
            return new ImageIcon(ImageIO.read(klass.getResourceAsStream(path)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static GraphicsConfiguration getGraphicsConfiguration() {
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        return environment.getDefaultScreenDevice().getDefaultConfiguration();
    }

    private static boolean isHeadless() {
        return GraphicsEnvironment.isHeadless();
    }

    public static BufferedImage toCompatibleImage(BufferedImage image) {
        if (isHeadless()) {
            return image;
        }

        if (image.getColorModel().equals(
                getGraphicsConfiguration().getColorModel())) {
            return image;
        }

        BufferedImage compatibleImage = getGraphicsConfiguration().createCompatibleImage(
                    image.getWidth(), image.getHeight(), image.getTransparency());
        Graphics g = compatibleImage.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return compatibleImage;
    }
}

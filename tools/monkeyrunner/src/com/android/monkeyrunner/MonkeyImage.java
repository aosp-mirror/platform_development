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

import com.android.ddmlib.RawImage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Jython object to encapsulate images that have been taken.
 */
public class MonkeyImage {
    private final RawImage screenshot;

    /**
     * Create a new monkey image.
     *
     * @param screenshot the screenshot to wrap.
     */
    MonkeyImage(RawImage screenshot) {
        this.screenshot = screenshot;
    }

    /**
     * Write out the file to the specified location.  This function tries to guess at the output
     * format depending on the file extension given.  If unable to determine, it uses PNG.
     *
     * @param path where to write the file
     * @return success.
     */
    public boolean writeToFile(String path) {
        int offset = path.lastIndexOf('.');
        if (offset < 0) {
            return writeToFile(path, "png");
        }
        String ext = path.substring(offset + 1);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix(ext);
        if (!writers.hasNext()) {
            return writeToFile(path, "png");
        }
        ImageWriter writer = writers.next();
        BufferedImage image = ImageUtils.convertImage(screenshot);
        try {
            File f = new File(path);
            f.delete();

            ImageOutputStream outputStream = ImageIO.createImageOutputStream(f);
            writer.setOutput(outputStream);

            try {
                writer.write(image);
            } finally {
                writer.dispose();
                outputStream.flush();
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * Write out the file to the specified location.
     *
     * @param path where to write the file
     * @param format the ImageIO format to use.
     * @return success.
     */
    public boolean writeToFile(String path, String format) {
        BufferedImage image = ImageUtils.convertImage(screenshot);
        try {
            ImageIO.write(image, format, new File(path));
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}

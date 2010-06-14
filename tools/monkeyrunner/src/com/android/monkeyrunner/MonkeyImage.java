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

import com.google.common.base.Preconditions;

import com.android.monkeyrunner.doc.MonkeyRunnerExported;

import org.python.core.ArgParser;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PyTuple;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Jython object to encapsulate images that have been taken.
 */
public abstract class MonkeyImage {
    /**
     * Convert the MonkeyImage into a BufferedImage.
     *
     * @return a BufferedImage for this MonkeyImage.
     */
    public abstract BufferedImage createBufferedImage();

    // Cache the BufferedImage so we don't have to generate it every time.
    private WeakReference<BufferedImage> cachedBufferedImage = null;

    /**
     * Utility method to handle getting the BufferedImage and managing the cache.
     *
     * @return the BufferedImage for this image.
     */
    private BufferedImage getBufferedImage() {
        // Check the cache first
        if (cachedBufferedImage != null) {
            BufferedImage img = cachedBufferedImage.get();
            if (img != null) {
                return img;
            }
        }

        // Not in the cache, so create it and cache it.
        BufferedImage img = createBufferedImage();
        cachedBufferedImage = new WeakReference<BufferedImage>(img);
        return img;
    }

    @MonkeyRunnerExported(doc = "Encode the image into a format and return the bytes.",
        args = {"format"},
        argDocs = { "The (optional) format in which to encode the image (PNG for example)" },
        returns = "A String containing the bytes.")
    public byte[] convertToBytes(PyObject[] args, String[] kws) {
      ArgParser ap = JythonUtils.createArgParser(args, kws);
      Preconditions.checkNotNull(ap);

      String format = ap.getString(0, "png");

      BufferedImage argb = convertSnapshot();

      ByteArrayOutputStream os = new ByteArrayOutputStream();
      try {
          ImageIO.write(argb, format, os);
      } catch (IOException e) {
          return new byte[0];
      }
      return os.toByteArray();
    }

    @MonkeyRunnerExported(doc = "Write out the file to the specified location.  If no " +
            "format is specified, this function tries to guess at the output format " +
            "depending on the file extension given.  If unable to determine, it uses PNG.",
            args = {"path", "format"},
            argDocs = {"Where to write out the file",
                       "The format in which to encode the image (PNG for example)"},
            returns = "True if writing succeeded.")
    public boolean writeToFile(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        String path = ap.getString(0);
        String format = ap.getString(1, null);

        if (format != null) {
            return writeToFile(path, format);
        }
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
        BufferedImage image = getBufferedImage();
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

    @MonkeyRunnerExported(doc = "Get a single ARGB pixel from the image",
            args = { "x", "y" },
            argDocs = { "the x offset of the pixel", "the y offset of the pixel" },
            returns = "A tuple of (A, R, G, B) for the pixel")
    public PyObject getRawPixel(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        int x = ap.getInt(0);
        int y = ap.getInt(1);
        int pixel = getPixel(x, y);
        PyInteger a = new PyInteger((pixel & 0xFF000000) >> 24);
        PyInteger r = new PyInteger((pixel & 0x00FF0000) >> 16);
        PyInteger g = new PyInteger((pixel & 0x0000FF00) >> 8);
        PyInteger b = new PyInteger((pixel & 0x000000FF) >> 0);
        return new PyTuple(a, r, g ,b);
    }

    @MonkeyRunnerExported(doc = "Get a single ARGB pixel from the image",
            args = { "x", "y" },
            argDocs = { "the x offset of the pixel", "the y offset of the pixel" },
            returns = "An integer for the ARGB pixel")
    public int getRawPixelInt(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        int x = ap.getInt(0);
        int y = ap.getInt(1);
        return getPixel(x, y);
    }

    private int getPixel(int x, int y) {
        BufferedImage image = getBufferedImage();
        return image.getRGB(x, y);
    }

    private BufferedImage convertSnapshot() {
        BufferedImage image = getBufferedImage();

        // Convert the image to ARGB so ImageIO writes it out nicely
        BufferedImage argb = new BufferedImage(image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics g = argb.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return argb;
    }

    public boolean writeToFile(String path, String format) {
        BufferedImage argb = convertSnapshot();

        try {
            ImageIO.write(argb, format, new File(path));
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @MonkeyRunnerExported(doc = "Compare this image to the other image.",
            args = {"other", "percent"},
            argDocs = {"The other image.",
                       "A float from 0.0 to 1.0 indicating the percentage " +
                           "of pixels that need to be the same.  Defaults to 1.0"},
            returns = "True if they are the same image.")
    public boolean sameAs(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        PyObject otherObject = ap.getPyObject(0);
        MonkeyImage other = (MonkeyImage) otherObject.__tojava__(MonkeyImage.class);

        double percent = JythonUtils.getFloat(ap, 1, 1.0);

        BufferedImage otherImage = other.getBufferedImage();
        BufferedImage myImage = getBufferedImage();

        // Easy size check
        if (otherImage.getWidth() != myImage.getWidth()) {
            return false;
        }
        if (otherImage.getHeight() != myImage.getHeight()) {
            return false;
        }

        int[] otherPixel = new int[1];
        int[] myPixel = new int[1];

        int width = myImage.getWidth();
        int height = myImage.getHeight();

        int numDiffPixels = 0;
        // Now, go through pixel-by-pixel and check that the images are the same;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (myImage.getRGB(x, y) != otherImage.getRGB(x, y)) {
                    numDiffPixels++;
                }
            }
        }
        double numberPixels = (height * width);
        double diffPercent = numDiffPixels / numberPixels;
        return percent <= 1.0 - diffPercent;
    }

    private static class BufferedImageMonkeyImage extends MonkeyImage {
        private final BufferedImage image;

        public BufferedImageMonkeyImage(BufferedImage image) {
            this.image = image;
        }

        @Override
        public BufferedImage createBufferedImage() {
            return image;
        }

    }

    @MonkeyRunnerExported(doc = "Get a sub-image of this image.",
            args = {"rect"},
            argDocs = {"A Tuple of (x, y, w, h) representing the area of the image to extract."},
            returns = "The newly extracted image.")
    public MonkeyImage getSubImage(PyObject[] args, String[] kws) {
        ArgParser ap = JythonUtils.createArgParser(args, kws);
        Preconditions.checkNotNull(ap);

        PyTuple rect = (PyTuple) ap.getPyObjectByType(0, PyTuple.TYPE);
        int x = rect.__getitem__(0).asInt();
        int y = rect.__getitem__(1).asInt();
        int w = rect.__getitem__(2).asInt();
        int h = rect.__getitem__(3).asInt();

        BufferedImage image = getBufferedImage();
        return new BufferedImageMonkeyImage(image.getSubimage(x, y, w, h));
    }
}
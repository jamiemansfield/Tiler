/*
 * This file is part of Tiler, licensed under the MIT License (MIT).
 *
 * Copyright (c) Jamie Mansfield <https://www.jamierocks.uk/>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package me.jamiemansfield.tiler;

import com.google.common.collect.ImmutableSet;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

public final class TilerMain {

    private static final Set<String> ALLOWED_EXTENSTIONS = ImmutableSet.<String>builder()
            .add("png")
            .add("jpg")
            .build();

    private static final Predicate<String> INTEGER = Pattern.compile("^\\d+$").asPredicate();

    public static void main(final String[] args) {
        // Get arguments from command line
        final String inputImage = args[0];
        final String zoomLevel  = args[1];

        // Verify arguments are valid
        final String extension;
        {
            final int extensionStart = inputImage.lastIndexOf('.');
            if (extensionStart == -1) throw new RuntimeException("Extension could not be obtained for: " + inputImage);
            extension = inputImage.substring(extensionStart + 1);
            if (!ALLOWED_EXTENSTIONS.contains(extension)) throw new RuntimeException("Extension '" + extension + "' not supported by Tiler");
        }
        final int zoom;
        {
            if (!INTEGER.test(zoomLevel)) throw new RuntimeException("The given zoom level '" + zoomLevel + "' is not an integer!");
            zoom = Integer.parseInt(zoomLevel);
        }

        // Read image
        final BufferedImage image;
        try {
            image = ImageIO.read(new File(inputImage));
        } catch (final IOException ex) {
            throw new RuntimeException("Failed to read image", ex);
        }

        // Tiling
        for (int currentZoom = 0; currentZoom <= zoom; currentZoom++) {
            final File zoomDirectory = new File("" + currentZoom);
            if (!zoomDirectory.exists()) zoomDirectory.mkdirs();

            final int rowCount = (int) Math.sqrt(Math.pow(4, currentZoom));
            final int realTileLength = image.getWidth() / rowCount;

            for (int currentColumn = 0; currentColumn < rowCount; currentColumn++) {
                final File columnDirectory = new File(zoomDirectory, "" + currentColumn);
                if (!columnDirectory.exists()) columnDirectory.mkdirs();

                for (int currentRow = 0; currentRow < rowCount; currentRow++) {
                    // Get sub image
                    final int x = currentRow    * realTileLength;
                    final int y = currentColumn * realTileLength;
                    final int w = realTileLength;
                    final int h = realTileLength;
                    final BufferedImage subImageRaw = image.getSubimage(x, y, w, h);

                    // Size image for saving
                    final BufferedImage subImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
                    {
                        final Graphics g = subImage.createGraphics();
                        g.drawImage(subImageRaw, 0, 0, 256, 256, null);
                        g.dispose();
                    }

                    // Write image to file
                    try {
                        ImageIO.write(subImage, extension, new File(columnDirectory, currentRow + "." + extension));
                    } catch (final IOException ex) {
                        throw new RuntimeException("Failed to output file!");
                    }
                }
            }
        }
    }

    private TilerMain() {
    }

}

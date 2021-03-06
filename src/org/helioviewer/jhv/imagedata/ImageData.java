package org.helioviewer.jhv.imagedata;

import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import org.helioviewer.jhv.astronomy.Position;
import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.metadata.MetaData;

public abstract class ImageData {

    public enum ImageFormat {
        Single8, Single16, RGB24, ARGB32
    }

    final int width;
    final int height;
    private final int bpp;
    private final double gamma;
    Buffer buffer;

    BufferedImage image = null;

    private Region region;
    private MetaData metaData;
    private Position.Q viewpoint;
    private boolean uploaded = false;

    ImageData(int _width, int _height, int _bpp, double _gamma) {
        width = _width;
        height = _height;
        bpp = _bpp;
        gamma = _gamma;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public int getBitsPerPixel() {
        return bpp;
    }

    public double getGamma() {
        return gamma;
    }

    public Buffer getBuffer() {
        return buffer;
    }

    public BufferedImage getBufferedImage() {
        if (image == null) {
            image = createBufferedImageFromImageTransport();
        }
        return image;
    }

    public abstract ImageFormat getImageFormat();

    /**
     * Internal function to create a BufferedImage from the image transport
     * object.
     *
     * This function will be called from {@link #getBufferedImage()} when
     * necessary.
     *
     * @return the created BufferedImage
     */
    protected abstract BufferedImage createBufferedImageFromImageTransport();

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region r) {
        region = r;
    }

    public void setMetaData(MetaData m) {
        metaData = m;
    }

    public MetaData getMetaData() {
        return metaData;
    }

    public void setViewpoint(Position.Q p) {
        viewpoint = p;
    }

    public Position.Q getViewpoint() {
        return viewpoint;
    }

    public boolean getUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean _uploaded) {
        uploaded = _uploaded;
    }

    private static final double BRIGHTNESS_F1 = 0.001;
    private static final double BRIGHTNESS_F2 = 128 + 64 + 32;

    public double getAutoBrightness() {
        if (!(buffer instanceof ByteBuffer))
            return 1;

        byte[] ba = ((ByteBuffer) buffer).array();
        int len = ba.length;
        int[] histogram = new int[256];
        for (int i = 0; i < len; i++) {
            histogram[getUnsigned(ba[i])]++;
        }

        long ct = 0;
        int j;
        for (j = 255; j >= 0; j--) {
            ct += histogram[j];
            if (ct > BRIGHTNESS_F1 * len) {
                break;
            }
        }
        if (j == 0)
            return 1;

        double factor = BRIGHTNESS_F2 / j;
        // System.out.println(">> " + factor + " " + j);
        factor /= metaData.getResponseFactor();
        if (factor > 2)
            return 2;
        else if (factor < 0.5)
            return 0.5;
        return factor;
    }

    private static int getUnsigned(byte b) {
        return (b + 256) & 0xFF;
    }

}

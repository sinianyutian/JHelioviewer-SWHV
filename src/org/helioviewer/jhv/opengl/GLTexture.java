package org.helioviewer.jhv.opengl;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.helioviewer.jhv.imagedata.ImageData;
import org.helioviewer.jhv.imagedata.ImageData.ImageFormat;
import org.helioviewer.jhv.log.Log;

import com.jogamp.opengl.GL2;

public class GLTexture {

    private int texID = -1;

    private int prev_width = -1;
    private int prev_height = -1;
    private int prev_inputGLFormat = -1;
    private int prev_bppGLType = -1;

    public GLTexture(GL2 gl) {
        int[] tmp = new int[1];
        gl.glGenTextures(1, tmp, 0);
        texID = tmp[0];
    }

    public void bind(GL2 gl, int target, int unit) {
        gl.glActiveTexture(unit);
        gl.glBindTexture(target, texID);
    }

    public void delete(GL2 gl) {
        gl.glDeleteTextures(1, new int[] { texID }, 0);
        texID = prev_width = -1;
    }

    private static void genTexture2D(GL2 gl, int internalFormat, int width, int height, int inputFormat, int inputType, Buffer buffer) {
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_BASE_LEVEL, 0);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAX_LEVEL, 0);
        gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, inputFormat, inputType, buffer);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
    }

    public void copyImageData2D(GL2 gl, ImageData source) {
        int w = source.getWidth();
        int h = source.getHeight();
        if (w <= 0 || h <= 0 || w > GLInfo.maxTextureSize || h > GLInfo.maxTextureSize) {
            Log.error("GLTexture.copyImageData2D: w= " + w + " h=" + h);
            return;
        }

        int bitsPerPixel = source.getBitsPerPixel();
        gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, bitsPerPixel >> 3);

        ImageFormat imageFormat = source.getImageFormat();
        int inputGLFormat = mapImageFormatToInputGLFormat(imageFormat);
        int bppGLType = mapBitsPerPixelToGLType(bitsPerPixel);

        if (w != prev_width || h != prev_height || prev_inputGLFormat != inputGLFormat || prev_bppGLType != bppGLType) {
            int internalGLFormat = mapImageFormatToInternalGLFormat(imageFormat);
            genTexture2D(gl, internalGLFormat, w, h, inputGLFormat, bppGLType, null);

            prev_width = w;
            prev_height = h;
            prev_inputGLFormat = inputGLFormat;
            prev_bppGLType = bppGLType;
        }

        gl.glTexSubImage2D(GL2.GL_TEXTURE_2D, 0, 0, 0, w, h, inputGLFormat, bppGLType, source.getBuffer());
    }

    public static void copyBufferedImage2D(GL2 gl, BufferedImage source) {
        int w = source.getWidth();
        int h = source.getHeight();
        if (w <= 0 || h <= 0 || w > GLInfo.maxTextureSize || h > GLInfo.maxTextureSize) {
            Log.error("GLTexture.copyBufferedImage2D: w= " + w + " h=" + h);
            return;
        }

        DataBuffer rawBuffer = source.getRaster().getDataBuffer();
        Buffer buffer;

        switch (rawBuffer.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            buffer = ByteBuffer.wrap(((DataBufferByte) rawBuffer).getData());
            break;
        case DataBuffer.TYPE_USHORT:
            buffer = ShortBuffer.wrap(((DataBufferUShort) rawBuffer).getData());
            break;
        case DataBuffer.TYPE_SHORT:
            buffer = ShortBuffer.wrap(((DataBufferShort) rawBuffer).getData());
            break;
        case DataBuffer.TYPE_INT:
            buffer = IntBuffer.wrap(((DataBufferInt) rawBuffer).getData());
            break;
        default:
            buffer = null;
        }

        gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, mapDataBufferTypeToGLAlign(rawBuffer.getDataType()));
        genTexture2D(gl, mapTypeToInternalGLFormat(source.getType()), w, h, mapTypeToInputGLFormat(source.getType()), mapDataBufferTypeToGLType(rawBuffer.getDataType()), buffer);
    }

    public static void copyBuffer1D(GL2 gl, IntBuffer source) {
        gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 4);
        gl.glTexImage1D(GL2.GL_TEXTURE_1D, 0, GL2.GL_RGBA, source.limit(), 0, GL2.GL_BGRA, GL2.GL_UNSIGNED_INT_8_8_8_8_REV, source);
        gl.glTexParameteri(GL2.GL_TEXTURE_1D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_1D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_1D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
    }

    /**
     * Internal function to map the application internal image formats to OpenGL
     * image formats, used for saving the texture.
     *
     * @param imageFormat
     *            Application internal image format
     * @return OpenGL memory image format
     */
    private static int mapImageFormatToInternalGLFormat(ImageFormat imageFormat) {
        if (imageFormat == ImageFormat.Single8)
            return GL2.GL_LUMINANCE8;
        else if (imageFormat == ImageFormat.Single16)
            return GL2.GL_LUMINANCE16;
        else if (imageFormat == ImageFormat.ARGB32 || imageFormat == ImageFormat.RGB24)
            return GL2.GL_RGBA;
        else
            throw new IllegalArgumentException("Format is not supported");
    }

    /**
     * Internal function to map the application internal image formats to OpenGL
     * image formats, used for transferring the texture.
     *
     * @param imageFormat
     *            Application internal image format
     * @return OpenGL input image format
     */
    private static int mapImageFormatToInputGLFormat(ImageFormat imageFormat) {
        if (imageFormat == ImageFormat.Single8 || imageFormat == ImageFormat.Single16)
            return GL2.GL_LUMINANCE;
        else if (imageFormat == ImageFormat.ARGB32 || imageFormat == ImageFormat.RGB24)
            return GL2.GL_BGRA;
        else
            throw new IllegalArgumentException("Format is not supported");
    }

    /**
     * Internal function to map BufferedImage image formats to OpenGL image
     * formats, used for saving the texture.
     *
     * @param type
     *            BufferedImage internal image format
     * @return OpenGL memory image format
     */
    private static int mapTypeToInternalGLFormat(int type) {
        if (type == BufferedImage.TYPE_BYTE_GRAY || type == BufferedImage.TYPE_BYTE_INDEXED)
            return GL2.GL_LUMINANCE;
        else
            return GL2.GL_RGBA;
    }

    /**
     * Internal function to map BufferedImage image formats to OpenGL image
     * formats, used for transferring the texture.
     *
     * @param type
     *            BufferedImage internal image format
     * @return OpenGL input image format
     */
    private static int mapTypeToInputGLFormat(int type) {
        if (type == BufferedImage.TYPE_BYTE_GRAY || type == BufferedImage.TYPE_BYTE_INDEXED)
            return GL2.GL_LUMINANCE;
        else if (type == BufferedImage.TYPE_4BYTE_ABGR || type == BufferedImage.TYPE_INT_BGR || type == BufferedImage.TYPE_INT_ARGB)
            return GL2.GL_BGRA;
        else
            return GL2.GL_RGBA;
    }

    /**
     * Internal function to map the number of bits per pixel to OpenGL types,
     * used for transferring the texture.
     *
     * @param bitsPerPixel
     *            Bits per pixel of the input data
     * @return OpenGL type to use
     */
    private static int mapBitsPerPixelToGLType(int bitsPerPixel) {
        switch (bitsPerPixel) {
        case 8:
            return GL2.GL_UNSIGNED_BYTE;
        case 16:
            return GL2.GL_UNSIGNED_SHORT;
        case 32:
            return GL2.GL_UNSIGNED_INT_8_8_8_8_REV;
        default:
            return 0;
        }
    }

    /**
     * Internal function to map the type of the a DataBuffer to OpenGL types,
     * used for transferring the texture.
     *
     * @param dataBufferType
     *            DataBuffer type of the input data
     * @return OpenGL type to use
     */
    private static int mapDataBufferTypeToGLType(int dataBufferType) {
        switch (dataBufferType) {
        case DataBuffer.TYPE_BYTE:
            return GL2.GL_UNSIGNED_BYTE;
        case DataBuffer.TYPE_SHORT:
            return GL2.GL_SHORT;
        case DataBuffer.TYPE_USHORT:
            return GL2.GL_UNSIGNED_SHORT;
        case DataBuffer.TYPE_INT:
            return GL2.GL_UNSIGNED_INT_8_8_8_8_REV;
        default:
            return 0;
        }
    }

    /**
     * Internal function to map the type of the a DataBuffer to OpenGL aligns,
     * used for reading input data.
     *
     * @param dataBufferType
     *            DataBuffer type of the input data
     * @return OpenGL type to use
     */
    private static int mapDataBufferTypeToGLAlign(int dataBufferType) {
        switch (dataBufferType) {
        case DataBuffer.TYPE_BYTE:
            return 1;
        case DataBuffer.TYPE_SHORT:
            return 2;
        case DataBuffer.TYPE_USHORT:
            return 2;
        case DataBuffer.TYPE_INT:
            return 4;
        default:
            return 0;
        }
    }

}

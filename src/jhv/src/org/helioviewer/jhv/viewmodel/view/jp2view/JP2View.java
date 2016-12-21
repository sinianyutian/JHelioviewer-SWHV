package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.awt.EventQueue;
import java.net.URI;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus.CacheStatus;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.AbstractView;

// This class is responsible for reading and decoding of JPEG2000 images
public class JP2View extends AbstractView {

    private final JP2ViewExecutor executor = new JP2ViewExecutor();

    protected JP2Image _jp2Image;

    private int targetFrame = 0;
    private int trueFrame = -1;

    private int frameCount = 0;
    private long frameCountStart;
    private float frameRate;

    private MetaData[] metaDataArray;
    private int maximumFrame;

    public void setJP2Image(JP2Image newJP2Image) {
        _jp2Image = newJP2Image;

        metaDataArray = _jp2Image.metaDataList;
        maximumFrame = metaDataArray.length - 1;
        frameCountStart = System.currentTimeMillis();
    }

    public String getXMLMetaData() {
        return _jp2Image.getXML(trueFrame + 1);
    }

    private volatile boolean isAbolished = false;

    @Override
    public void abolish() {
        if (isAbolished)
            return;
        isAbolished = true;

        new Thread(() -> {
            executor.abolish();
            if (_jp2Image != null) {
                _jp2Image.abolish();
                _jp2Image = null;
            }
        }).start();
    }

    // if instance was built before cancelling
    @Override
    protected void finalize() throws Throwable {
        try {
            abolish();
        } finally {
            super.finalize();
        }
    }

    /**
     * This function is used as a callback function which is called by
     * {@link J2KRender} when it has finished decoding an image.
     */
    void setImageData(ImageData newImageData) {
        int frame = newImageData.getMetaData().getFrameNumber();
        if (frame != trueFrame) {
            trueFrame = frame;
            ++frameCount;
        }

        if (dataHandler != null) {
            dataHandler.handleData(newImageData);
        }
    }

    @Override
    public float getCurrentFramerate() {
        long currentTime = System.currentTimeMillis();
        long delta = currentTime - frameCountStart;

        if (delta > 1000) {
            frameRate = 1000 * frameCount / (float) delta;
            frameCount = 0;
            frameCountStart = currentTime;
        }

        return frameRate;
    }

    @Override
    public boolean isMultiFrame() {
        return maximumFrame > 0;
    }

    @Override
    public int getMaximumFrameNumber() {
        return maximumFrame;
    }

    @Override
    public int getCurrentFrameNumber() {
        return targetFrame;
    }

    // to be accessed only from Layers
    @Override
    public JHVDate getNextTime(AnimationMode mode, int deltaT) {
        int next = targetFrame + 1;
        switch (mode) {
        case STOP:
            if (next > maximumFrame) {
                return null;
            }
            break;
        case SWING:
            if (targetFrame == maximumFrame) {
                Layers.setAnimationMode(AnimationMode.SWINGDOWN);
                return metaDataArray[targetFrame - 1].getViewpoint().time;
            }
            break;
        case SWINGDOWN:
            if (targetFrame == 0) {
                Layers.setAnimationMode(AnimationMode.SWING);
                return metaDataArray[1].getViewpoint().time;
            }
            return metaDataArray[targetFrame - 1].getViewpoint().time;
        default: // LOOP
            if (next > maximumFrame) {
                return metaDataArray[0].getViewpoint().time;
            }
        }
        return metaDataArray[next].getViewpoint().time;
    }

    @Override
    public void setFrame(JHVDate time) {
        int frame = getFrameNumber(time);
        if (frame != targetFrame) {
            CacheStatus status = _jp2Image.getImageCacheStatus().getVisibleStatus(frame);
            if (status != CacheStatus.PARTIAL && status != CacheStatus.COMPLETE)
                return;
            targetFrame = frame;
        }
    }

    private int getFrameNumber(JHVDate time) {
        int frame = -1;
        long lastDiff, currentDiff = -Long.MAX_VALUE;
        do {
            lastDiff = currentDiff;
            currentDiff = metaDataArray[++frame].getViewpoint().time.milli - time.milli;
        } while (currentDiff < 0 && frame < maximumFrame);

        if (-lastDiff < currentDiff) {
            return frame - 1;
        } else {
            return frame;
        }
    }

    @Override
    public JHVDate getFrameTime(int frame) {
        if (frame < 0) {
            frame = 0;
        } else if (frame > maximumFrame) {
            frame = maximumFrame;
        }

        return metaDataArray[frame].getViewpoint().time;
    }

    @Override
    public JHVDate getFirstTime() {
        return metaDataArray[0].getViewpoint().time;
    }

    @Override
    public JHVDate getLastTime() {
        return metaDataArray[maximumFrame].getViewpoint().time;
    }

    @Override
    public JHVDate getFrameTime(JHVDate time) {
        return metaDataArray[getFrameNumber(time)].getViewpoint().time;
    }

    @Override
    public MetaData getMetaData(JHVDate time) {
        return metaDataArray[getFrameNumber(time)];
    }

    // //

    private Camera camera;
    private Viewport vp;
    private Position.Q viewpoint;

    @Override
    public void render(Camera _camera, Viewport _vp, double factor) {
        vp = _vp;
        camera = _camera;
        if (camera != null) {
            viewpoint = camera.getViewpoint();
        }
        signalRender(_jp2Image, targetFrame, factor);
    }

    void signalRenderFromReader(JP2Image image, int frame, double factor) {
        if (isAbolished || frame != targetFrame)
            return;
        EventQueue.invokeLater(() -> signalRender(image, frame, factor));
    }

    private void signalRender(JP2Image image, int frame, double factor) {
        executor.execute(camera, vp, viewpoint, this, image, frame, factor);
    }

    @Override
    public String getName() {
        return _jp2Image.getName();
    }

    @Override
    public URI getURI() {
        return _jp2Image.getURI();
    }

    @Override
    public LUT getDefaultLUT() {
        return _jp2Image.getDefaultLUT();
    }

    @Override
    public CacheStatus getImageCacheStatus(int frame) {
        return _jp2Image.getImageCacheStatus().getVisibleStatus(frame);
    }

}

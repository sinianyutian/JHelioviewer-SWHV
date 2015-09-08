package org.helioviewer.viewmodel.view.jp2view;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.base.Region;
import org.helioviewer.base.math.GL3DVec2d;
import org.helioviewer.base.time.ImmutableDateTime;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.RenderListener;
import org.helioviewer.jhv.gui.filters.lut.DefaultTable;
import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.jhv.threads.JHVThread;
import org.helioviewer.viewmodel.imagecache.ImageCacheStatus;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.metadata.ObserverMetaData;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.ViewROI;
import org.helioviewer.viewmodel.view.jp2view.JP2Image.ReaderMode;
import org.helioviewer.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.viewmodel.view.jp2view.image.SubImage;

/**
 * Implementation of View for JPG2000 images.
 * <p>
 * This class represents the gateway to the heart of the helioviewer project. It
 * is responsible for reading and decoding JPG2000 images. Therefore, it manages
 * two threads: one thread for communicating with the JPIP server, the other one
 * for decoding the images.
 *
 */
public class JHVJP2View extends AbstractView implements RenderListener {

    static private class RejectExecution implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            //System.out.println(Thread.currentThread().getName());
        }
    }

    BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<Runnable>(1);
    RejectedExecutionHandler rejectedExecutionHandler = new RejectExecution();//new ThreadPoolExecutor.CallerRunsPolicy();
    int numOfThread = 1;
    private final ExecutorService exec = new ThreadPoolExecutor(numOfThread, numOfThread, 10000L, TimeUnit.MILLISECONDS, blockingQueue, new JHVThread.NamedThreadFactory("Render"), rejectedExecutionHandler);

    protected Region region;

    // Member related to JP2
    protected JP2Image jp2Image;
    protected JP2ImageParameter imageViewParams;

    private int targetFrame;

    private int frameCount = 0;
    private long frameCountStart;
    private float frameRate;

    private boolean stopRender = false;

    public JHVJP2View() {
        Displayer.addRenderListener(this);
        frameCountStart = System.currentTimeMillis();
    }

    /**
     * Sets the JPG2000 image used by this class.
     *
     * This functions sets up the whole infrastructure needed for using the
     * image, including the two threads.
     *
     * <p>
     * Thus, this functions also works as a constructor.
     *
     * @param newJP2Image
     */
    public void setJP2Image(JP2Image newJP2Image) throws Exception {
        if (jp2Image != null) {
            throw new Exception("JP2 image already set");
        }

        jp2Image = newJP2Image;
        jp2Image.addReference();

        metaDataArray = jp2Image.metaDataList;
        MetaData metaData = metaDataArray[0];

        if (region == null)
            region = new Region(metaData.getPhysicalLowerLeft(), metaData.getPhysicalSize());

        imageViewParams = calculateParameter(region, 0);

        jp2Image.startReader(this);
    }

    private int getTrueFrameNumber() {
        int frameNumber = 0;
        if (imageData != null) {
            frameNumber = imageData.getFrameNumber();
        }
        return frameNumber;
    }

    @Override
    public String getName() {
        MetaData metaData = metaDataArray[getTrueFrameNumber()];
        if (metaData instanceof ObserverMetaData) {
            return ((ObserverMetaData) metaData).getFullName();
        } else {
            String name = jp2Image.getURI().getPath();
            return name.substring(name.lastIndexOf('/') + 1, name.lastIndexOf('.'));
        }
    }

    public String getXMLMetaData() {
        return jp2Image.getXML(getTrueFrameNumber() + 1);
    }

    @Override
    public URI getUri() {
        return jp2Image.getURI();
    }

    @Override
    public URI getDownloadURI() {
        return jp2Image.getDownloadURI();
    }

    @Override
    public boolean isRemote() {
        return jp2Image.isRemote();
    }

    private class AbolishThread extends Thread {
        private JHVJP2View view;

        public Runnable init(JHVJP2View view) {
            this.view = view;
            return this;
        }

        @Override
        public void run() {
            EventQueue.invokeLater(new Runnable() {
                private JHVJP2View view;

                @Override
                public void run() {
                    view.abolishExternal();
                }

                public Runnable init(JHVJP2View view) {
                    this.view = view;
                    return this;
                }
            }.init(this.view));
        }
    }

    // Destroy the resources associated with this object
    @Override
    public void abolish() {
        AbolishThread thread = new AbolishThread();
        stopRender = true;
        thread.init(this);
        exec.submit(thread);
    }

    public void abolishExternal() {
        Displayer.removeRenderListener(this);
        jp2Image.abolish();
        jp2Image = null;
    }

    /**
     * Recalculates the image parameters.
     *
     * This function maps between the set of parameters used within the view
     * chain and the set of parameters used within the jp2-package.
     *
     * <p>
     * To achieve this, calculates the set of parameters used within the
     * jp2-package according to the given requirements from the view chain.
     *
     * @param v
     *            Viewport the image will be displayed in
     * @param r
     *            Physical region
     * @param frameNumber
     *            Frame number to show (has to be 0 for single images)
     * @return Set of parameters used within the jp2-package
     */
    protected JP2ImageParameter calculateParameter(Region r, int frameNumber) {
        MetaData m = metaDataArray[frameNumber];

        double mWidth = m.getPhysicalSize().x;
        double mHeight = m.getPhysicalSize().y;
        double rWidth = r.getWidth();
        double rHeight = r.getHeight();

        double ratio = Displayer.getViewportHeight() / Displayer.getActiveCamera().getCameraWidth();
        int w = (int) (rWidth * ratio);
        int h = (int) (rHeight * ratio);

        ratio = mWidth / rWidth;
        int totalWidth = (int) (w * ratio);
        int totalHeight = (int) (h * ratio);

        ResolutionLevel res = jp2Image.getResolutionSet().getNextResolutionLevel(new Dimension(totalWidth, totalHeight));
        int viewportImageWidth = res.getResolutionBounds().width;
        int viewportImageHeight = res.getResolutionBounds().height;

        double currentMeterPerPixel = mWidth / viewportImageWidth;
        int imageWidth = (int) Math.round(rWidth / currentMeterPerPixel);
        int imageHeight = (int) Math.round(rHeight / currentMeterPerPixel);

        GL3DVec2d rUpperLeft = r.getUpperLeftCorner();
        GL3DVec2d mUpperLeft = m.getPhysicalUpperLeft();
        double displacementX = rUpperLeft.x - mUpperLeft.x;
        double displacementY = rUpperLeft.y - mUpperLeft.y;

        int imagePositionX = (int) Math.round(displacementX / mWidth * viewportImageWidth);
        int imagePositionY = -(int) Math.round(displacementY / mHeight * viewportImageHeight);
        SubImage subImage = new SubImage(imagePositionX, imagePositionY, imageWidth, imageHeight);

        return new JP2ImageParameter(subImage, res, frameNumber);
    }

    /*
     * NOTE: The following section is for communications with the two threads,
     * J2KReader and J2KRender. Thus, the visibility is set to "default" (also
     * known as "package"). These functions should not be used by any other
     * class.
     */

    JP2ImageParameter getImageViewParams() {
        return new JP2ImageParameter(imageViewParams.subImage, imageViewParams.resolution, imageViewParams.compositionLayer);
    }

    /**
     * Sets the new image data for the given region.
     *
     * <p>
     * This function is used as a callback function which is called by
     * {@link J2KRender} when it has finished decoding an image.
     *
     * @param newImageData
     *            New image data
     * @param params
     *            New JP2Image parameters
     */
    void setSubimageData(ImageData newImageData, JP2ImageParameter params) {
        int frame = params.compositionLayer;
        MetaData metaData = metaDataArray[frame];

        newImageData.setFrameNumber(frame);
        newImageData.setMetaData(metaData);

        if (metaData instanceof HelioviewerMetaData) {
            newImageData.setRegion(((HelioviewerMetaData) metaData).roiToRegion(params.subImage, params.resolution.getZoomPercent()));
        }

        if (imageData != null && imageData.getFrameNumber() != frame)
            ++frameCount;

        imageData = newImageData;
        if (dataHandler != null) {
            dataHandler.handleData(this, imageData);
        }
    }

    @Override
    public ImageCacheStatus getImageCacheStatus() {
        return jp2Image.getImageCacheStatus();
    }

    @Override
    public float getActualFramerate() {
        long currentTime = System.currentTimeMillis();
        long delta = currentTime - frameCountStart;

        if (delta > 1000) {
            frameRate = 1000.f * frameCount / delta;
            frameCount = 0;
            frameCountStart = currentTime;
        }

        return frameRate;
    }

    @Override
    public boolean isMultiFrame() {
        return jp2Image.isMultiFrame();
    }

    @Override
    public int getMaximumFrameNumber() {
        return jp2Image.getMaximumFrameNumber();
    }

    @Override
    public int getCurrentFrameNumber() {
        return targetFrame;
    }

    /**
     * Before actually setting the new frame number, checks whether that is
     * necessary. If the frame number has changed, also triggers an update of
     * the image.
     *
     * @param frame
     */
    // to be accessed only from Layers
    @Override
    public void setFrame(int frame) {
        if (frame != targetFrame && frame >= 0 && frame <= jp2Image.getMaximumAccessibleFrameNumber()) {
            targetFrame = frame;

            // necessary for fov change
            jp2Image.readerSignal.signal();
            if (jp2Image.getReaderMode() != ReaderMode.ONLYFIREONCOMPLETE) {
                signalRender(false);
            }
        }
    }

    // to be accessed only from Layers
    @Override
    public int getFrame(ImmutableDateTime time) {
        int frame = -1;
        long timeMillis = time.getMillis();
        long lastDiff, currentDiff = -Long.MAX_VALUE;
        do {
            lastDiff = currentDiff;
            currentDiff = metaDataArray[++frame].getDateObs().getMillis() - timeMillis;
        } while (currentDiff < 0 && frame < jp2Image.getMaximumFrameNumber());

        if (-lastDiff < currentDiff) {
            return frame - 1;
        } else {
            return frame;
        }
    }

    @Override
    public void render() {
        region = ViewROI.getSingletonInstance().updateROI(metaDataArray[targetFrame]);
        signalRender(false);
    }

    void signalRender(boolean hasExtraData) {
        // from reader on EDT, might come after abolish
        if (stopRender == true || jp2Image == null)
            return;

        JP2ImageParameter newParams = calculateParameter(region, targetFrame);
        if (!hasExtraData && imageData != null && newParams.equals(imageViewParams)) {
            return;
        }
        imageViewParams = newParams;

        J2KRender task = new J2KRender(this, jp2Image, imageViewParams);
        {
            blockingQueue.poll();
            blockingQueue.add(task);
        }
        exec.submit(task, Boolean.TRUE);
    }

    @Override
    public LUT getDefaultLUT() {
        int[] builtIn = jp2Image.getBuiltInLUT();
        if (builtIn != null) {
            return new LUT("built-in", builtIn/* , builtIn */);
        }

        MetaData metaData = metaDataArray[0];
        if (metaData instanceof HelioviewerMetaData) {
            String colorKey = DefaultTable.getSingletonInstance().getColorTable((HelioviewerMetaData) metaData);
            if (colorKey != null) {
                return LUT.getStandardList().get(colorKey);
            }
        }
        return null;
    }

}

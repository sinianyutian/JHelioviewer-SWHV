package org.helioviewer.viewmodel.view.jp2view;

import java.util.Date;

import org.helioviewer.base.math.Interval;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.RenderListener;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.PlayStateChangedReason;
import org.helioviewer.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.ObserverMetaData;
import org.helioviewer.viewmodel.view.CachedMovieView;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.cache.DateTimeCache;
import org.helioviewer.viewmodel.view.cache.HelioviewerDateTimeCache;
import org.helioviewer.viewmodel.view.cache.ImageCacheStatus;
import org.helioviewer.viewmodel.view.cache.ImageCacheStatus.CacheStatus;
import org.helioviewer.viewmodel.view.cache.LocalImageCacheStatus;
import org.helioviewer.viewmodel.view.cache.RemoteImageCacheStatus;
import org.helioviewer.viewmodel.view.jp2view.J2KRender.RenderReasons;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;
import org.helioviewer.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.viewmodel.view.jp2view.image.SubImage;

/**
 * Implementation of TimedMovieView for JPX files.
 *
 * <p>
 * This class is an extensions of {@link JHVJP2View} for JPX-Files, providing
 * additional movie commands.
 *
 * <p>
 * For information about image series, see
 * {@link org.helioviewer.viewmodel.view.MovieView} and
 * {@link org.helioviewer.viewmodel.view.TimedMovieView}.
 *
 * @author Markus Langenberg
 */
public class JHVJPXView extends JHVJP2View implements TimedMovieView, CachedMovieView, RenderListener {

    // Caching
    protected ImageCacheStatus imageCacheStatus;
    protected DateTimeCache dateTimeCache;
    protected int lastRenderedCompositionLayer = -1;
    public int texID = -1;
    /*
     * Set to true if you want to use the displaylock.
     */
    private boolean blockingMode;
    /**
     * Linking movies, if the movie is not linked, this has to be null
     */
    protected LinkedMovieManager linkedMovieManager;
    private boolean fullyLoadedMode = false;
    private boolean differenceMode = false;
    private boolean baseDifferenceMode = false;

    /**
     * Default constructor.
     *
     * <p>
     * When the view is not marked as a main view, it is assumed, that the view
     * will only serve one single image and will not have to perform any kind of
     * update any more. The effect of this assumption is, that the view will not
     * try to reconnect to the JPIP server when the connection breaks and that
     * there will be no other timestamps used than the first one.
     *
     * @param isMainView
     *            Whether the view is a main view or not
     */
    public JHVJPXView(boolean isMainView, Interval<Date> range, boolean blockingMode) {
        super(isMainView, range);
        this.blockingMode = blockingMode;
        if (isMainView) {
            Displayer.getSingletonInstance().addRenderListener(this);
        }
    }

    public JHVJPXView(boolean isMainView, Interval<Date> range) {
        super(isMainView, range);
        this.blockingMode = false;
        if (isMainView) {
            Displayer.getSingletonInstance().addRenderListener(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setJP2Image(JP2Image newJP2Image) {
        if (!isMainView) {
            super.setJP2Image(newJP2Image);
            imageCacheStatus = ((JHVJPXView) jp2Image.getParentView()).getImageCacheStatus();
            dateTimeCache = ((JHVJPXView) jp2Image.getParentView()).getDateTimeCache();
            return;
        }

        if (jp2Image != null) {
            abolish(false);
        }

        jp2Image = newJP2Image;

        if (newJP2Image.isRemote()) {
            imageCacheStatus = new RemoteImageCacheStatus(this);
        } else {
            imageCacheStatus = new LocalImageCacheStatus(this);
        }
        jp2Image.setImageCacheStatus(imageCacheStatus);

        dateTimeCache = new HelioviewerDateTimeCache(this, jp2Image);

        super.setJP2Image(newJP2Image);

        dateTimeCache.startParsing();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DateTimeCache getDateTimeCache() {
        return dateTimeCache;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageCacheStatus getImageCacheStatus() {
        return imageCacheStatus;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentFrame(int frameNumber, ChangeEvent event) {
        setCurrentFrame(frameNumber, event, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentFrame(int frameNumber, ChangeEvent event, boolean forceSignal) {

        frameNumber = Math.max(0, Math.min(getMaximumFrameNumber(), frameNumber));

        if (forceSignal && linkedMovieManager != null) {
            while (getMaximumAccessibleFrameNumber() < imageViewParams.compositionLayer) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            linkedMovieManager.setCurrentFrame(getFrameDateTime(frameNumber), event, forceSignal);
        } else {
            boolean changed;
            changed = setCurrentFrameNumber(frameNumber, event, forceSignal);
            if (changed && linkedMovieManager != null) {
                linkedMovieManager.setCurrentFrame(getFrameDateTime(frameNumber), event, forceSignal);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentFrame(ImmutableDateTime time, ChangeEvent event) {
        setCurrentFrame(time, event, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCurrentFrame(ImmutableDateTime time, ChangeEvent event, boolean forceSignal) {

        if (time == null)
            return;

        if (linkedMovieManager != null && linkedMovieManager.setCurrentFrame(time, event, forceSignal)) {
            return;
        }

        int frameNumber = -1;
        long timeMillis = time.getMillis();
        long lastDiff, currentDiff = -Long.MAX_VALUE;

        do {
            lastDiff = currentDiff;

            if (dateTimeCache.getDateTime(++frameNumber) == null) {
                return;
            }

            currentDiff = dateTimeCache.getDateTime(frameNumber).getMillis() - timeMillis;
        } while (currentDiff < 0 && frameNumber < jp2Image.getCompositionLayerRange().getEnd());

        if (-lastDiff < currentDiff) {
            setCurrentFrameNumber(frameNumber - 1, event, forceSignal);
        } else {
            setCurrentFrameNumber(frameNumber, event, forceSignal);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCurrentFrameNumber() {
        return imageViewParams.compositionLayer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaximumFrameNumber() {
        return jp2Image.getCompositionLayerRange().getEnd();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaximumAccessibleFrameNumber() {
        if (dateTimeCache == null || imageCacheStatus == null) {
            return -1;
        }
        return Math.min(dateTimeCache.getMetaStatus(), imageCacheStatus.getImageCachedPartiallyUntil());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImmutableDateTime getCurrentFrameDateTime() {
        return dateTimeCache.getDateTime(getCurrentFrameNumber());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImmutableDateTime getFrameDateTime(int frameNumber) {
        return dateTimeCache.getDateTime(frameNumber);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAnimationMode(AnimationMode mode) {
        if (render != null) {
            render.setAnimationMode(mode);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDesiredRelativeSpeed(int framesPerSecond) {
        if (render != null) {
            render.setMovieRelativeSpeed(framesPerSecond);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDesiredAbsoluteSpeed(int observationSecondsPerSecond) {
        if (render != null) {
            render.setMovieAbsoluteSpeed(observationSecondsPerSecond);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void linkMovie() {
        linkedMovieManager = LinkedMovieManager.getActiveInstance();
        linkedMovieManager.linkMovie(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unlinkMovie() {
        if (linkedMovieManager != null) {
            LinkedMovieManager temp = linkedMovieManager;
            linkedMovieManager = null;
            temp.unlinkMovie(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getActualFramerate() {

        if (render != null)
            return render.getActualMovieFramerate();

        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pauseMovie() {
        if (!isMoviePlaying()) {
            return;
        }

        if (linkedMovieManager != null) {
            linkedMovieManager.pauseLinkedMovies();
        }
        readerSignal.signal();
        if (render != null) {
            render.setMovieMode(false);
            render.setLinkedMovieMode(false);
        }

        // send notification
        ChangeEvent fireEvent = null;
        synchronized (event) {
            fireEvent = event.clone();
            event.reinitialize();
        }
        fireEvent.addReason(new PlayStateChangedReason(this, this.linkedMovieManager, false));
        notifyViewListeners(fireEvent);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void playMovie() {
        if (getMaximumFrameNumber() > 0) {
            if (linkedMovieManager == null || !linkedMovieManager.playLinkedMovies()) {
                if (linkedMovieManager == null || linkedMovieManager.isMaster(this)) {
                    if (render != null) {
                        render.setMovieMode(true);
                    }
                    readerSignal.signal();
                    if (readerMode != ReaderMode.ONLYFIREONCOMPLETE) {
                        renderRequestedSignal.signal(RenderReasons.MOVIE_PLAY);
                    }
                } else {
                    if (render != null) {
                        render.setLinkedMovieMode(true);
                    }
                }
                // send notification
                ChangeEvent fireEvent = null;
                synchronized (event) {
                    fireEvent = event.clone();
                    event.reinitialize();
                }
                fireEvent.addReason(new PlayStateChangedReason(this, this.linkedMovieManager, true));
                notifyViewListeners(fireEvent);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMoviePlaying() {
        if (render != null) {
            return render.isMovieMode() || (linkedMovieManager != null && linkedMovieManager.isPlaying());
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void abolish() {
        abolish(true);
    }

    /**
     * Abolishes the jpx view
     *
     * @param unlinkMovie
     *            true, if the movie should be unlinked before abolishing it
     */
    public void abolish(boolean unlinkMovie) {
        if (unlinkMovie && linkedMovieManager != null) {
            linkedMovieManager.unlinkMovie(this);
        }
        pauseMovie();
        dateTimeCache.stopParsing();
        super.abolish();
    }

    /**
     * {@inheritDoc}
     */

    @Override
    void setSubimageData(ImageData newImageData, SubImage roi, int compositionLayer, double zoompercent, boolean fullyLoaded) {
        fullyLoaded = this.imageCacheStatus.getImageStatus(compositionLayer) == CacheStatus.COMPLETE;

        if (blockingMode) {
            synchronized (Displayer.displaylock) {
                setSubimageDataHelper(newImageData, roi, compositionLayer, zoompercent, fullyLoaded);
            }
        }
        else{
            setSubimageDataHelper(newImageData, roi, compositionLayer, zoompercent, fullyLoaded);
        }
        if(fullyLoadedMode && !fullyLoaded){
            //this.readerSignal.signal();
        }
    }

    private void setSubimageDataHelper(ImageData newImageData, SubImage roi, int compositionLayer, double zoompercent, boolean fullyLoaded) {
        lastRenderedCompositionLayer = compositionLayer;

        if (metaData instanceof ObserverMetaData) {
            ObserverMetaData observerMetaData = (ObserverMetaData) metaData;
            observerMetaData.updateDateTime(dateTimeCache.getDateTime(compositionLayer));
            event.addReason(new TimestampChangedReason(this, observerMetaData.getDateTime()));
        }
        super.setSubimageData(newImageData, roi, compositionLayer, zoompercent, fullyLoaded);
    }

    @Override
    public LinkedMovieManager getLinkedMovieManager() {
        return linkedMovieManager;
    }

    /**
     * Internal function for setting the current frame number.
     *
     * Before actually setting the new frame number, checks whether that is
     * necessary. If the frame number has changed, also triggers an update of
     * the image.
     *
     * @param frameNumber
     * @return true, if the frame number has changed
     */
    protected boolean setCurrentFrameNumber(int frameNumber, ChangeEvent event, boolean forceSignal) {
        if (frameNumber != imageViewParams.compositionLayer || forceSignal) {

            imageViewParams.compositionLayer = frameNumber;

            while (getMaximumAccessibleFrameNumber() < imageViewParams.compositionLayer) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            this.event.copyFrom(event);
            readerSignal.signal();
            if (readerMode != ReaderMode.ONLYFIREONCOMPLETE) {
                renderRequestedSignal.signal(RenderReasons.MOVIE_PLAY);
            }

            return true;
        }
        return false;
    }

    /**
     * Recalculates the image parameters.
     *
     * <p>
     * This function maps between the set of parameters used within the view
     * chain and the set of parameters used within the jp2-package.
     *
     * <p>
     * To achieve this, calls {@link #calculateParameter(int, int)} with the
     * currently used number of quality layers and the current frame number.
     *
     * @return Set of parameters used within the jp2-package
     */
    @Override
    protected JP2ImageParameter calculateParameter() {
        return calculateParameter(getCurrentNumQualityLayers(), getCurrentFrameNumber());
    }

    /**
     * @see org.helioviewer.viewmodel.view.MovieView#setReuseBuffer(boolean)
     */
    @Override
    public void setReuseBuffer(boolean reuseBuffer) {
        render.setReuseBuffer(reuseBuffer);
    }

    /**
     * @see org.helioviewer.viewmodel.view.MovieView#isReuseBuffer()
     */
    @Override
    public boolean isReuseBuffer() {
        return render.isReuseBuffer();
    }

    @Override
    public void render() {
        renderRequestedSignal.signal(RenderReasons.NEW_DATA);
    }

    public void removeRenderListener() {
        Displayer.getSingletonInstance().removeRenderListener(this);
    }

    @Override
    public long getCurrentDateMillis() {
        HelioviewerMetaData metadata = (HelioviewerMetaData) this.getMetadata();
        return metadata.getDateTime().getMillis();
    }

    public void setBlocking(boolean blockingMode) {
        this.blockingMode = blockingMode;
    }

    public void setDifferenceMode(boolean differenceMode) {
        this.differenceMode = differenceMode;
        this.render.setDifferenceMode(differenceMode);
    }
    public void setFullyLoadedMode(boolean fullyLoadedMode) {
        this.fullyLoadedMode = fullyLoadedMode;
    }
    public boolean getDifferenceMode(){
        return this.differenceMode;
    }

    public void setBaseDifferenceMode(boolean selected) {
        this.baseDifferenceMode = selected;
    }

    public boolean getBaseDifferenceMode() {
        return baseDifferenceMode;
    }
}

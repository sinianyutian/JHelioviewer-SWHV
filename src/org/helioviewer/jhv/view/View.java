package org.helioviewer.jhv.view;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helioviewer.jhv.base.lut.LUT;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.imagedata.ImageDataHandler;
import org.helioviewer.jhv.io.APIRequest;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.time.JHVDate;

public interface View {

    enum AnimationMode {
        Loop, Stop, Swing, SwingDown
    }

    APIRequest getAPIRequest();
    void setAPIRequest(APIRequest apiRequest);

    void abolish();

    void render(Camera camera, Viewport vp, double factor);

    URI getURI();
    String getName();

    LUT getDefaultLUT();

    boolean isMultiFrame();
    int getCurrentFrameNumber();
    int getMaximumFrameNumber();
    int getCurrentFramerate();

    void setImageLayer(ImageLayer imageLayer);
    ImageLayer getImageLayer();

    void setDataHandler(ImageDataHandler dataHandler);

    boolean isDownloading();
    boolean isComplete();

    AtomicBoolean getFrameCacheStatus(int frame);

    JHVDate getFrameTime(int frame);
    JHVDate getFirstTime();
    JHVDate getLastTime();

    // <!- only for Layers
    JHVDate getNextTime(AnimationMode mode, int deltaT);
    void setFrame(JHVDate time);
    JHVDate getFrameTime(JHVDate time);
    MetaData getMetaData(JHVDate time);
    // -->

    String getXMLMetaData() throws Exception;

}

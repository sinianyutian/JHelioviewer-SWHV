package org.helioviewer.jhv.plugins.pfss;

import java.awt.Component;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.TimespanListener;
import org.helioviewer.jhv.opengl.GLLine;
import org.helioviewer.jhv.plugins.pfss.data.PfssData;
import org.helioviewer.jhv.plugins.pfss.data.PfssNewDataLoader;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;
import org.helioviewer.jhv.threads.CancelTask;

import com.jogamp.opengl.GL2;

public class PfssRenderable extends AbstractRenderable implements TimespanListener {

    private final PfssOptionsPanel optionsPanel = new PfssOptionsPanel();
    private PfssData previousPfssData = null;
    private final GLLine line = new GLLine();

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;

        PfssData pfssData = PfssPlugin.getPfsscache().getData(Layers.getLastUpdatedTimestamp().milli);
        if (pfssData != null) {
            renderData(gl, pfssData, vp.aspect);
            previousPfssData = pfssData;
        }
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return "PFSS Model";
    }

    private String timeString = null;

    @Override
    public String getTimeString() {
        return timeString;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!visible)
            timeString = null;
    }

    @Override
    public void timespanChanged(long start, long end) {
        PfssPlugin.getPfsscache().clear();

        FutureTask<Void> dataLoaderTask = new FutureTask<>(new PfssNewDataLoader(start, end), null);
        PfssPlugin.pfssNewLoadPool.execute(dataLoaderTask);
        PfssPlugin.pfssReaperPool.schedule(new CancelTask(dataLoaderTask), 60 * 5, TimeUnit.SECONDS);
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void init(GL2 gl) {
        line.init(gl);
    }

    @Override
    public void dispose(GL2 gl) {
        previousPfssData = null;
        line.dispose(gl);
    }

    private void renderData(GL2 gl, PfssData data, double aspect) {
        int qualityReduction = optionsPanel.getQualityReduction();
        boolean fixedColor = optionsPanel.getFixedColor();

        if (data != previousPfssData || data.needsUpdate(qualityReduction, fixedColor)) {
            data.calculatePositions(qualityReduction, fixedColor);
            line.setData(gl, data.vertices, data.colors);

            timeString = data.getDateObs().toString();
            ImageViewerGui.getRenderableContainer().fireTimeUpdated(this);
        }
        line.render(gl, aspect);
    }

}

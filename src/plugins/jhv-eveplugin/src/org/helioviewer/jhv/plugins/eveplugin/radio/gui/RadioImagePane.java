package org.helioviewer.jhv.plugins.eveplugin.radio.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.Collection;
import java.util.Date;

import org.helioviewer.jhv.plugins.eveplugin.draw.DrawableElement;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawableElementType;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.DrawableAreaMap;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.NoDataConfig;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.PlotConfig;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.RadioPlotModel;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.RadioPlotModelListener;

public class RadioImagePane implements ImageObserver, RadioPlotModelListener, DrawableElement {

    private YAxisElement yAxisElement;
    private boolean intervalTooBig;

    public RadioImagePane() {
        intervalTooBig = false;
    }

    @Override
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
        return false;
    }

    @Override
    public void drawBufferedImage(BufferedImage image, DrawableAreaMap map) {
    }

    @Override
    public void changeVisibility(long iD) {
    }

    @Override
    public void removeDownloadRequestData(long iD) {
    }

    @Override
    public DrawableElementType getDrawableElementType() {
        return DrawableElementType.RADIO;
    }

    @Override
    public void draw(Graphics2D g, Graphics2D leftAxisG, Rectangle graphArea, Rectangle leftAxisArea, Point mousePosition) {
        // Log.trace("redraw radio image pane for plot : " + plotIdentifier);
        // Thread.dumpStack();
        if (!intervalTooBig) {
            Collection<NoDataConfig> noDataConfigs = RadioPlotModel.getSingletonInstance().getNoDataConfigurations();
            for (NoDataConfig ndc : noDataConfigs) {
                ndc.draw(g);
            }
            Collection<PlotConfig> configs = RadioPlotModel.getSingletonInstance().getPlotConfigurations();
            for (PlotConfig pc : configs) {
                pc.draw(g);
            }
        } else {
            String text1 = "The selected interval is too big.";
            String text2 = "Reduce the interval to see the radio spectrograms.";
            final int text1Width = (int) g.getFontMetrics().getStringBounds(text1, g).getWidth();
            final int text2Width = (int) g.getFontMetrics().getStringBounds(text2, g).getWidth();
            final int text1height = (int) g.getFontMetrics().getStringBounds(text2, g).getHeight();
            final int text2height = (int) g.getFontMetrics().getStringBounds(text2, g).getHeight();
            final int x1 = graphArea.x + (graphArea.width / 2) - (text1Width / 2);
            final int y1 = (int) (graphArea.y + (graphArea.height / 2) - 1.5 * text1height);
            final int x2 = graphArea.x + (graphArea.width / 2) - (text2Width / 2);
            final int y2 = (int) (graphArea.y + graphArea.height / 2 + 0.5 * text2height);
            g.setColor(Color.black);
            g.drawString(text1, x1, y1);
            g.drawString(text2, x2, y2);
        }
    }

    @Override
    public void setYAxisElement(YAxisElement yAxisElement) {
        this.yAxisElement = yAxisElement;

    }

    @Override
    public YAxisElement getYAxisElement() {
        return yAxisElement;
    }

    @Override
    public boolean hasElementsToDraw() {
        return (RadioPlotModel.getSingletonInstance().getPlotConfigurations() != null && !RadioPlotModel.getSingletonInstance().getPlotConfigurations().isEmpty()) || !RadioPlotModel.getSingletonInstance().getNoDataConfigurations().isEmpty();
    }

    public void setIntervalTooBig(boolean b) {
        intervalTooBig = b;
    }

    public boolean getIntervalTooBig() {
        return intervalTooBig;
    }

    @Override
    public Date getLastDateWithData() {
        return null;
    }

}

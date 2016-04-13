package org.helioviewer.jhv.plugins.eveplugin.lines.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawableElement;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawableElementType;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxisElement;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.Band;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.EVEValues;
import org.helioviewer.jhv.plugins.eveplugin.lines.model.EVEDrawController;

public class EVEDrawableElement implements DrawableElement {

    private final List<GraphPolyline> graphPolylines = new ArrayList<EVEDrawableElement.GraphPolyline>();
    private Band[] bands = new Band[0];
    // private EVEValues[] values = null;
    private Interval interval;
    private YAxisElement yAxisElement;
    private long lastMilliWithData;

    public EVEDrawableElement(Interval interval, Band[] bands, EVEValues[] values, YAxisElement yAxisElement) {
        this.interval = interval;
        this.bands = bands;
        // this.values = values;
        this.yAxisElement = yAxisElement;
        lastMilliWithData = -1;
    }

    public EVEDrawableElement() {
        interval = new Interval(Calendar.getInstance().getTime(), Calendar.getInstance().getTime());
        bands = new Band[0];
        // values = new EVEValues[0];
        yAxisElement = new YAxisElement();
        lastMilliWithData = -1;
    }

    @Override
    public DrawableElementType getDrawableElementType() {
        return DrawableElementType.LINE;
    }

    @Override
    public void draw(Graphics2D g, Graphics2D leftAxisG, Rectangle graphArea, Rectangle leftAxisArea, Point mousePosition) {
        updateGraphsData(interval, graphArea);
        drawGraphs(g, graphArea);
    }

    private void updateGraphsData(Interval interval, Rectangle graphArea) {
        double minValue = yAxisElement.getScaledMinValue();
        double maxValue = yAxisElement.getScaledMaxValue();

        double ratioX = graphArea.width / (double) (interval.end.getTime() - interval.start.getTime());
        double ratioY = maxValue < minValue ? 0 : graphArea.height / (maxValue - minValue);

        graphPolylines.clear();

        long intervalStartTime = interval.start.getTime();
        int dY = graphArea.y + graphArea.height;

        for (int i = 0; i < bands.length; ++i) {
            if (bands[i].isVisible()) {
                EVEValues values = EVEDrawController.getSingletonInstance().getValues(bands[i], interval, graphArea);
                // Log.debug(values.dates.length);
                int num = values.getNumberOfValues();
                final ArrayList<Point> pointList = new ArrayList<Point>();
                final LinkedList<Integer> warnLevels = new LinkedList<Integer>();
                final LinkedList<String> warnLabels = new LinkedList<String>();
                HashMap<String, Double> unconvertedWarnLevels = bands[i].getBandType().getWarnLevels();

                Iterator<Map.Entry<String, Double>> it = unconvertedWarnLevels.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Double> pairs = it.next();
                    warnLevels.add(dY - computeY(yAxisElement.scale(pairs.getValue()), ratioY, minValue));
                    warnLabels.add(pairs.getKey());
                }

                int counter = 0;
                for (int j = 0; j < num; j++) {
                    float value = values.maxValues[j];

                    if (value < 10e-32) {
                        if (counter > 1) {
                            graphPolylines.add(new GraphPolyline(pointList, bands[i].getGraphColor(), warnLevels, warnLabels, ratioX, graphArea.getWidth()));
                        }
                        pointList.clear();
                        counter = 0;
                        continue;
                    }

                    long date = values.dates[j];
                    int x = (int) ((date - intervalStartTime) * ratioX) + graphArea.x;
                    int y = dY;
                    y -= computeY(yAxisElement.scale(value), ratioY, minValue);

                    if (date > lastMilliWithData) {
                        lastMilliWithData = date;
                    }

                    final Point point = new Point(x, y);
                    pointList.add(point);
                    counter++;
                }
                if (counter > 0) {
                    graphPolylines.add(new GraphPolyline(pointList, bands[i].getGraphColor(), warnLevels, warnLabels, ratioX, graphArea.getWidth()));
                }
            }

        }
    }

    private int computeY(double orig, double ratioY, double minV) {
        return (int) (ratioY * (orig - minV));
    }

    private void drawGraphs(final Graphics2D g, Rectangle graphArea) {
        for (GraphPolyline line : graphPolylines) {
            g.setColor(line.color);
            for (int k = 0; k < line.xPoints.size(); k++) {
                g.drawPolyline(line.xPointsArray.get(k), line.yPointsArray.get(k), line.yPoints.get(k).size());
            }

            for (int j = 0; j < line.warnLevels.length; j++) {
                g.drawLine(graphArea.x, line.warnLevels[j], graphArea.x + graphArea.width, line.warnLevels[j]);
                g.drawString(line.warnLabels[j], graphArea.x, line.warnLevels[j] - 2);
                // TODO draw label under line if it will be cut off.
            }
        }
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Graph Polyline
    // //////////////////////////////////////////////////////////////////////////////

    private static class GraphPolyline {

        // //////////////////////////////////////////////////////////////////////////
        // Definitions
        // //////////////////////////////////////////////////////////////////////////

        public final ArrayList<ArrayList<Integer>> xPoints;
        public final ArrayList<ArrayList<Integer>> yPoints;
        public final ArrayList<int[]> xPointsArray;
        public final ArrayList<int[]> yPointsArray;
        public final int[] warnLevels;
        public final String[] warnLabels;

        public final Color color;

        // //////////////////////////////////////////////////////////////////////////
        // Methods
        // //////////////////////////////////////////////////////////////////////////

        public GraphPolyline(final List<Point> points, final Color color, final List<Integer> warnLevels, final List<String> warnLabels, double ratioX, double graphWidth) {
            xPoints = new ArrayList<ArrayList<Integer>>();
            yPoints = new ArrayList<ArrayList<Integer>>();
            xPointsArray = new ArrayList<int[]>();
            yPointsArray = new ArrayList<int[]>();
            this.color = color;

            int numberOfWarnLevels = warnLevels.size();
            this.warnLevels = new int[numberOfWarnLevels];
            this.warnLabels = new String[numberOfWarnLevels];
            int counter = -1;
            double localGraphWidth = graphWidth > 0 ? graphWidth : 10000;
            Integer previousX = null;
            int len = points.size();
            int jump = (int) (len / localGraphWidth);
            if (jump == 0) {
                jump = 1;
            }
            int index = 0;
            while (index < len) {
                Point point = points.get(index);
                /*
                 * if (previousX != null) { if ((point.x - previousX) != 0) { //
                 * Log.debug("distance between previous and folowing x : " // +
                 * ((point.x - previousX) / ratioX)); } }
                 */
                if (previousX == null || (point.x - previousX) / ratioX > Math.max(1 / ratioX, 120000)) {
                    xPoints.add(new ArrayList<Integer>());
                    yPoints.add(new ArrayList<Integer>());
                    counter++;
                }
                xPoints.get(counter).add(point.x);
                yPoints.get(counter).add(point.y);
                previousX = point.x;
                index += jump;
            }

            for (int i = 0; i < xPoints.size(); i++) {
                int[] xPointsArr = new int[xPoints.get(i).size()];
                int[] yPointsArr = new int[yPoints.get(i).size()];
                for (int j = 0; j < xPoints.get(i).size(); j++) {
                    xPointsArr[j] = xPoints.get(i).get(j);
                    yPointsArr[j] = yPoints.get(i).get(j);
                }
                xPointsArray.add(xPointsArr);
                yPointsArray.add(yPointsArr);
            }

            counter = 0;
            for (final Integer warnLevel : warnLevels) {
                this.warnLevels[counter] = warnLevel;
                counter++;
            }
            counter = 0;
            for (final String warnLabel : warnLabels) {
                this.warnLabels[counter] = warnLabel;
                counter++;
            }
        }
    }

    public void set(Interval interval, Band[] bands, YAxisElement yAxisElement) {
        this.interval = interval;
        this.bands = bands;
        this.yAxisElement = yAxisElement;
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
        return bands.length > 0;
    }

    @Override
    public Date getLastDateWithData() {
        if (lastMilliWithData >= 0) {
            return new Date(lastMilliWithData);
        } else {
            return null;
        }
    }

}

package org.helioviewer.plugins.eveplugin.controller;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;

import org.helioviewer.base.math.Interval;
import org.helioviewer.plugins.eveplugin.lines.data.BandController;
import org.helioviewer.plugins.eveplugin.lines.data.DownloadController;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpace;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceListener;
import org.helioviewer.plugins.eveplugin.model.PlotAreaSpaceManager;
import org.helioviewer.plugins.eveplugin.settings.BandType;
//import org.helioviewer.plugins.eveplugin.model.PlotTimeSpace;
import org.helioviewer.plugins.eveplugin.settings.EVEAPI.API_RESOLUTION_AVERAGES;

/**
 * 
 * @author Stephan Pagel
 * */
public class ZoomController implements PlotAreaSpaceListener {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    /** the sole instance of this class */
    private static final ZoomController singletonInstance = new ZoomController();

    public enum ZOOM {
        CUSTOM, All, Year, Month, Day, Hour
    };

    private final LinkedList<ZoomControllerListener> listeners = new LinkedList<ZoomControllerListener>();

    private Interval<Date> availableInterval = new Interval<Date>(null, null);
    private Interval<Date> selectedInterval = new Interval<Date>(null, null);

    private API_RESOLUTION_AVERAGES selectedResolution = API_RESOLUTION_AVERAGES.MINUTE_1;

    private final PlotAreaSpaceManager plotAreaSpaceManager;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    /**
     * The private constructor to support the singleton pattern.
     * */
    private ZoomController() {
        plotAreaSpaceManager = PlotAreaSpaceManager.getInstance();
        plotAreaSpaceManager.addPlotAreaSpaceListenerToAllSpaces(this);
    }

    /**
     * Method returns the sole instance of this class.
     * 
     * @return the only instance of this class.
     * */
    public static ZoomController getSingletonInstance() {
        return singletonInstance;
    }

    public boolean addZoomControllerListener(final ZoomControllerListener listener) {
        return listeners.add(listener);
    }

    public boolean removeControllerListener(final ZoomControllerListener listener) {
        return listeners.remove(listener);
    }

    public void setAvailableInterval(final Interval<Date> interval) {
        availableInterval = makeCompleteDay(interval);
        fireAvailableIntervalChanged(availableInterval);

        // request data if needed
        final Calendar calendar = new GregorianCalendar();
        calendar.clear();
        calendar.setTime(availableInterval.getEnd());
        calendar.add(Calendar.DAY_OF_MONTH, -1);

        final Interval<Date> downloadInterval = new Interval<Date>(availableInterval.getStart(), calendar.getTime());
        final BandType[] bandTypes = BandController.getSingletonInstance().getAllAvailableBandTypes();

        DownloadController.getSingletonInstance().updateBands(bandTypes, downloadInterval, selectedInterval);

        // check if selected interval is in available interval and correct it if
        // needed
        setSelectedInterval(selectedInterval, false);
        // PlotTimeSpace.getInstance().setSelectedMinAndMaxTime(interval.getStart(),
        // interval.getEnd());
    }

    private Interval<Date> makeCompleteDay(final Interval<Date> interval) {
        return makeCompleteDay(interval.getStart(), interval.getEnd());
    }

    private Interval<Date> makeCompleteDay(final Date start, final Date end) {
        final Interval<Date> interval = new Interval<Date>(null, null);

        if (start == null || end == null) {
            return interval;
        }

        final Calendar calendar = new GregorianCalendar();
        calendar.clear();
        calendar.setTime(start);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        interval.setStart(calendar.getTime());

        calendar.clear();
        calendar.setTime(end);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_MONTH, 1);

        interval.setEnd(calendar.getTime());

        return interval;
    }

    public final Interval<Date> getAvailableInterval() {
        return availableInterval;
    }

    private void fireAvailableIntervalChanged(final Interval<Date> newInterval) {
        for (ZoomControllerListener listener : listeners) {
            listener.availableIntervalChanged(newInterval);
        }
    }

    public Interval<Date> setSelectedInterval(final Interval<Date> newSelectedInterval, boolean useFullValueSpace) {
        synchronized (selectedInterval) {
            if (availableInterval.getStart() == null || availableInterval.getEnd() == null) {
                selectedInterval = new Interval<Date>(null, null);
            } else if (newSelectedInterval.getStart() == null || newSelectedInterval.getEnd() == null) {
                selectedInterval = availableInterval;
            } else if (availableInterval.containsInclusive(newSelectedInterval)) {
                selectedInterval = newSelectedInterval;
            } else {
                Date start = newSelectedInterval.getStart();
                Date end = newSelectedInterval.getEnd();

                start = availableInterval.containsPointInclusive(start) ? start : availableInterval.getStart();
                end = availableInterval.containsPointInclusive(end) ? end : availableInterval.getEnd();

                if (start.equals(end)) {
                    selectedInterval = availableInterval;
                } else {
                    selectedInterval = new Interval<Date>(start, end);
                }
            }

            updatePlotAreaSpace(selectedInterval);

            fireSelectedIntervalChanged(selectedInterval, useFullValueSpace);

            return selectedInterval;
        }
    }

    public Interval<Date> zoomTo(final ZOOM zoom, final int value) {
        Interval<Date> newInterval = new Interval<Date>(null, null);
        switch (zoom) {
        case CUSTOM:
            newInterval = selectedInterval;
            break;
        case All:
            newInterval = availableInterval;
            break;
        case Day:
            newInterval = computeZoomInterval(selectedInterval, Calendar.DAY_OF_MONTH, value);
            break;
        case Hour:
            newInterval = computeZoomInterval(selectedInterval, Calendar.HOUR, value);
            break;
        case Month:
            newInterval = computeZoomInterval(selectedInterval, Calendar.MONTH, value);
            break;
        case Year:
            newInterval = computeZoomInterval(selectedInterval, Calendar.YEAR, value);
            break;
        }
        return setSelectedInterval(newInterval, true);
    }

    public Interval<Date> zoomTo(final ZOOM zoom) {
        return zoomTo(zoom, 1);
    }

    private Interval<Date> computeZoomInterval(final Interval<Date> interval, final int calendarField, final int difference) {
        final Date startDate = interval.getStart();
        final Date endDate = interval.getEnd();
        final Date availableEndDate = availableInterval.getEnd();

        if (startDate == null || endDate == null || availableEndDate == null) {
            return new Interval<Date>(null, null);
        }

        final GregorianCalendar calendar = new GregorianCalendar();

        // add difference to start date -> when calculated end date is within
        // available interval it is the result
        calendar.clear();
        calendar.setTime(selectedInterval.getStart());
        calendar.add(calendarField, difference);

        if (availableInterval.containsPointInclusive(calendar.getTime())) {
            return new Interval<Date>(startDate, calendar.getTime());
        }

        // computed end date is outside of available interval -> compute start
        // date from available end date based on difference
        calendar.clear();
        calendar.setTime(availableEndDate);
        calendar.add(calendarField, -difference);

        if (availableInterval.containsPointInclusive(calendar.getTime())) {
            return new Interval<Date>(calendar.getTime(), availableEndDate);
        }

        // available interval is smaller then requested one -> return available
        // interval
        return new Interval<Date>(availableInterval);
    }

    public Interval<Date> getSelectedInterval() {
        return selectedInterval;
    }

    private void fireSelectedIntervalChanged(final Interval<Date> newInterval, boolean keepFullValueSpace) {
        for (ZoomControllerListener listener : listeners) {
            listener.selectedIntervalChanged(newInterval, keepFullValueSpace);
        }
    }

    public void setSelectedResolution(final API_RESOLUTION_AVERAGES resolution) {
        selectedResolution = resolution;

        fireSelectedResolutionChanged(selectedResolution);
    }

    public API_RESOLUTION_AVERAGES getSelectedResolution() {
        return selectedResolution;
    }

    private void fireSelectedResolutionChanged(final API_RESOLUTION_AVERAGES reolution) {
        for (ZoomControllerListener listener : listeners) {
            listener.selectedResolutionChanged(reolution);
        }
    }

    @Override
    public void plotAreaSpaceChanged(double scaledMinValue, double scaledMaxValue, double scaledMinTime, double scaledMaxTime,
            double scaledSelectedMinValue, double scaledSelectedMaxValue, double scaledSelectedMinTime, double scaledSelectedMaxTime,
            boolean forced) {
        if (availableInterval.getStart() != null && availableInterval.getEnd() != null && selectedInterval.getStart() != null
                && selectedInterval.getEnd() != null) {
            synchronized (selectedInterval) {
                long diffTime = availableInterval.getEnd().getTime() - availableInterval.getStart().getTime();
                double scaleDiff = scaledMaxTime - scaledMinTime;
                double selectedMin = (scaledSelectedMinTime - scaledMinTime) / scaleDiff;
                double selectedMax = (scaledSelectedMaxTime - scaledMinTime) / scaleDiff;
                Date newSelectedStartTime = new Date(availableInterval.getStart().getTime() + Math.round(diffTime * selectedMin));
                Date newSelectedEndTime = new Date(availableInterval.getStart().getTime() + Math.round(diffTime * selectedMax));
                // Log.info("plotareachanged starttime: " + newSelectedStartTime
                // + " endtime: " + newSelectedEndTime);
                if (forced
                        || !(newSelectedEndTime.equals(selectedInterval.getEnd()) && newSelectedStartTime.equals(selectedInterval
                                .getStart()))) {
                    selectedInterval = new Interval<Date>(newSelectedStartTime, newSelectedEndTime);
                    fireSelectedIntervalChanged(selectedInterval, false);
                }
            }
        }

    }

    private void updatePlotAreaSpace(Interval<Date> selectedInterval) {
        if (availableInterval != null && availableInterval.getStart() != null && availableInterval.getEnd() != null
                && selectedInterval != null && selectedInterval.getStart() != null && selectedInterval.getEnd() != null) {
            for (PlotAreaSpace pas : plotAreaSpaceManager.getAllPlotAreaSpaces()) {
                long diffAvailable = availableInterval.getEnd().getTime() - availableInterval.getStart().getTime();
                double diffPlotAreaTime = pas.getScaledMaxTime() - pas.getScaledMinTime();
                double scaledSelectedStart = pas.getScaledMinTime()
                        + (1.0 * (selectedInterval.getStart().getTime() - availableInterval.getStart().getTime()) * diffPlotAreaTime / diffAvailable);
                double scaledSelectedEnd = pas.getScaledMinTime()
                        + (1.0 * (selectedInterval.getEnd().getTime() - availableInterval.getStart().getTime()) * diffPlotAreaTime / diffAvailable);
                pas.setScaledSelectedTimeAndValue(scaledSelectedStart, scaledSelectedEnd, pas.getScaledMinValue(), pas.getScaledMaxValue());

            }
        }
    }
}

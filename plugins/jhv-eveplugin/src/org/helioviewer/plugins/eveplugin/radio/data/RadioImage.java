package org.helioviewer.plugins.eveplugin.radio.data;


import java.awt.Color;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.helioviewer.base.math.Interval;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.plugins.eveplugin.radio.model.ResolutionSetting;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;

public class RadioImage {
	private Interval<Date> timeInterval;
	private FrequencyInterval freqInterval;
	private int frameInJPX;
	private ResolutionSet resolutioSet;
	private List<ResolutionSetting> resolutionSettings;
	private String plotIdentifier;
	private boolean isVisible;
	private RadioDataManager radioDataManager;
	private long ID;
	
	public RadioImage(long ID,Interval<Date> timeInterval,
			FrequencyInterval freqInterval, int frameInJPX, ResolutionSet rs, List<ResolutionSetting> resolutionSettings, String plotIdentifier) {
		super();
		this.ID = ID;
		this.timeInterval = timeInterval;
		this.freqInterval = freqInterval;
		this.frameInJPX = frameInJPX;
		this.resolutioSet = rs;
		this.resolutionSettings = resolutionSettings;
		this.plotIdentifier = plotIdentifier;
		this.isVisible = true;
		this.radioDataManager = RadioDataManager.getSingletonInstance();
	}
	
	public long getID() {
		return ID;
	}

	public void setID(long iD) {
		ID = iD;
	}



	public Interval<Date> getTimeInterval() {
		return timeInterval;
	}
	
	public void setTimeInterval(Interval<Date> timeInterval) {
		this.timeInterval = timeInterval;
	}
	
	public FrequencyInterval getFreqInterval() {
		return freqInterval;
	}
	
	public void setFreqInterval(FrequencyInterval freqInterval) {
		this.freqInterval = freqInterval;
	}
	
	public int getFrameInJPX() {
		return frameInJPX;
	}
	
	public void setFrameInJPX(int frameInJPX) {
		this.frameInJPX = frameInJPX;
	}

	public ResolutionSet getResolutioSet() {
		return resolutioSet;
	}

	public void setResolutioSet(ResolutionSet resolutioSet) {
		this.resolutioSet = resolutioSet;
	}
	
	public ResolutionSetting defineBestResolutionSetting(double ratioX, double ratioY){
		ResolutionSetting currentBest = null;
		int highestLevel = 0;
		for (ResolutionSetting rs : resolutionSettings){
			if(rs.getxRatio() < ratioX || rs.getyRatio() < ratioY){
				if(rs.getResolutionLevel() > highestLevel){
					highestLevel = rs.getResolutionLevel();
					currentBest = rs;
				}
			}
			if(rs.getResolutionLevel()==0 && currentBest == null){
				currentBest = rs;
			}
		}
		return currentBest;
	}
	
	public boolean withinInterval(Interval<Date> intervalToBeIn, FrequencyInterval freqIntervalToBeIn){
		return intervalToBeIn.overlapsInclusive(timeInterval) && freqIntervalToBeIn.overlaps(freqInterval); 
	}

	/*@Override
	public void removeLineData() {
		radioDataManager.removeRadioData(this);		
	}

	@Override
	public void setVisibility(boolean visible) {
		this.isVisible = visible;
	}

	@Override
	public boolean isVisible() {
		return this.isVisible;
	}

	@Override
	public String getName() {
		return "Callisto radiogram";
	}

	@Override
	public Color getDataColor() {
		return Color.BLACK;
	}

	@Override
	public void setDataColor(Color c) {}

	@Override
	public boolean isDownloading() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getPlotIdentifier() {
		return this.plotIdentifier;
	}

	@Override
	public void setPlotIndentifier(String identifier) {
		this.plotIdentifier = identifier;
	}

	@Override
	public boolean isAvailable() {
		return true;
	}*/
}

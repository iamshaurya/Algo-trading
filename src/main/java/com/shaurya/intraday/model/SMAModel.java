/**
 * 
 */
package com.shaurya.intraday.model;

import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Shaurya
 *
 */
@Getter
@Setter
public class SMAModel {
	private TreeMap<Date, IndicatorValue> smaMap;
	private List<Candle> prevCandles;
	private int timePeriod;

	public SMAModel(TreeMap<Date, IndicatorValue> smaMap, List<Candle> prevCandles, int timePeriod) {
		super();
		this.smaMap = smaMap;
		this.prevCandles = prevCandles;
		this.timePeriod = timePeriod;
	}

}

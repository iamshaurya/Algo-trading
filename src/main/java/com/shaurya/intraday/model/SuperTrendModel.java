/**
 * 
 */
package com.shaurya.intraday.model;

import java.util.Date;
import java.util.TreeMap;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Shaurya
 *
 */
@Getter
@Setter
public class SuperTrendModel {
	private TreeMap<Date, IndicatorValue> superTrendMap;
	private double prevFinalUpperBand;
	private double prevFinalLowerBand;
	private Candle prevCandle;

	public SuperTrendModel(TreeMap<Date, IndicatorValue> superTrendMap, double prevFinalUpperBand,
			double prevFinalLowerBand, Candle prevCandle) {
		super();
		this.superTrendMap = superTrendMap;
		this.prevFinalUpperBand = prevFinalUpperBand;
		this.prevFinalLowerBand = prevFinalLowerBand;
		this.prevCandle = prevCandle;
	}

}

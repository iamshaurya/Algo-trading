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
public class RSIModel {
	private TreeMap<Date, IndicatorValue> rsiMap;
	private double avgGain;
	private double avgLoss;
	private Candle lastCandle;

	public RSIModel(TreeMap<Date, IndicatorValue> rsiMap, double avgGain, double avgLoss, Candle lastCandle) {
		super();
		this.rsiMap = rsiMap;
		this.avgGain = avgGain;
		this.avgLoss = avgLoss;
		this.lastCandle = lastCandle;
	}
	

}

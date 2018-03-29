/**
 * 
 */
package com.shaurya.intraday.model;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Shaurya
 *
 */
@Getter
@Setter
public class ADXModel {
	private IndicatorValue adx;
	private double prevTR;
	private double prevPositiveDM;
	private double prevNegativeDM;
	private Candle lastCandle;

	public ADXModel(IndicatorValue adx, double prevTR, double prevPositiveDM, double prevNegativeDM,
			Candle lastCandle) {
		super();
		this.adx = adx;
		this.prevTR = prevTR;
		this.prevPositiveDM = prevPositiveDM;
		this.prevNegativeDM = prevNegativeDM;
		this.lastCandle = lastCandle;
	}

}

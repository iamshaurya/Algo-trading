package com.shaurya.intraday.enums;
/**
 * 
 */

/**
 * @author Shaurya
 *
 */
public enum IndicatorType {
	EMA(1), SMA(2), RSI(3), MACD(4), MACD_SIGNAL(5), ATR(6), ADX(7), SUPER_TREND(8);
	private int id;

	private IndicatorType(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

}

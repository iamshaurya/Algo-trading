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
public class HeikinAshiCandle {
	private Candle haCandle;
	private Candle candle;

	public HeikinAshiCandle(Candle haCandle, Candle candle) {
		this.haCandle = haCandle;
		this.candle = candle;
	}
}

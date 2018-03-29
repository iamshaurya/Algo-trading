/**
 * 
 */
package com.shaurya.intraday.util;

import java.util.ArrayList;
import java.util.List;

import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.HeikinAshiCandle;

/**
 * @author Shaurya
 *
 */
public class HeikinAshiBuilder {
	public static HeikinAshiCandle convert(Candle currentCandle, HeikinAshiCandle prevHACandle) {
		Candle haCandle = null;
		double haOpen;
		double haClose;
		double haHigh;
		double haLow;
		if (prevHACandle == null) {
			haClose = (currentCandle.getOpen() + currentCandle.getClose() + currentCandle.getHigh()
					+ currentCandle.getLow()) / 4;
			haOpen = (currentCandle.getOpen() + currentCandle.getClose()) / 2;
			haLow = currentCandle.getLow();
			haHigh = currentCandle.getHigh();
		} else {
			Candle prevCandle = prevHACandle.getHaCandle();
			haClose = (currentCandle.getOpen() + currentCandle.getClose() + currentCandle.getHigh()
					+ currentCandle.getLow()) / 4;
			haOpen = (prevCandle.getOpen() + prevCandle.getClose()) / 2;
			haLow = Math.min(currentCandle.getLow(), Math.min(haOpen, haClose));
			haHigh = Math.max(currentCandle.getHigh(), Math.max(haOpen, haClose));
		}
		haCandle = new Candle(currentCandle.getSecurity(), currentCandle.getTime(), haOpen, haHigh, haLow, haClose,
				currentCandle.getVolume());
		return new HeikinAshiCandle(haCandle, currentCandle);
	}

	public static List<HeikinAshiCandle> convertList(List<Candle> cList) {
		List<HeikinAshiCandle> haCandleList = new ArrayList<>();
		haCandleList.add(convert(cList.get(0), null));
		for (int i = 1; i < cList.size(); i++) {
			haCandleList.add(convert(cList.get(i), haCandleList.get(haCandleList.size() - 1)));
		}
		return haCandleList;
	}
}

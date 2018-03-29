/**
 * 
 */
package com.shaurya.intraday.indicator;

import static com.shaurya.intraday.util.HelperUtil.convertListToMap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import com.shaurya.intraday.enums.IndicatorType;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.IndicatorValue;

/**
 * @author Shaurya
 *
 */
public class EMA {
	public static TreeMap<Date, IndicatorValue> calculateEMA(int timePeriod, List<Candle> cList) {
		List<IndicatorValue> emaList = new ArrayList<>();
		if (timePeriod <= cList.size() && timePeriod > 0) {
			double initalSMA = 0;
			for (int i = 0; i < timePeriod; i++) {
				initalSMA += cList.get(i).getClose();
			}
			initalSMA = (double) initalSMA / timePeriod;

			double multiplier = (double) 2 / (timePeriod + 1);

			emaList.add(new IndicatorValue(cList.get(timePeriod - 1).getTime(), initalSMA, IndicatorType.EMA));

			populateEMAList(multiplier, emaList, cList, timePeriod, timePeriod);

		}

		return convertListToMap(emaList);
	}

	public static void populateEMAList(double multiplier, List<IndicatorValue> emaList, List<Candle> cList,
			int timePeriod, int index) {
		if (index < cList.size()) {
			double ema = (cList.get(index).getClose() - emaList.get(emaList.size() - 1).getIndicatorValue())
					* multiplier + emaList.get(emaList.size() - 1).getIndicatorValue();
			emaList.add(new IndicatorValue(cList.get(index).getTime(), ema, IndicatorType.EMA));
			populateEMAList(multiplier, emaList, cList, timePeriod, ++index);
		}
	}

	public static double calculateEMA(int timePeriod, Candle candle, double prevEma) {
		double multiplier = (double) 2 / (timePeriod + 1);
		return (candle.getClose() - prevEma) * multiplier + prevEma;
	}
}

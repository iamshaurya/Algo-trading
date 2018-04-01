/**
 * 
 */
package com.shaurya.intraday.indicator;

import static com.shaurya.intraday.util.HelperUtil.convertListToMap;

import java.util.ArrayList;
import java.util.List;

import com.shaurya.intraday.enums.IndicatorType;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.IndicatorValue;
import com.shaurya.intraday.model.SMAModel;

/**
 * @author Shaurya
 *
 */
public class SMA {
	public static SMAModel calculateSMA(int timePeriod, List<Candle> cList) {
		List<IndicatorValue> smaList = new ArrayList<>();
		if (timePeriod <= cList.size() && timePeriod > 0) {
			double initalSMA = 0;
			for (int i = 0; i < timePeriod; i++) {
				initalSMA += cList.get(i).getClose();
			}
			initalSMA = (double) initalSMA / timePeriod;

			smaList.add(new IndicatorValue(cList.get(timePeriod - 1).getTime(), initalSMA, IndicatorType.SMA));

			populateSMAList(smaList, cList, timePeriod, timePeriod);

		}
		return new SMAModel(convertListToMap(smaList), cList.subList(cList.size() - timePeriod - 1, cList.size() - 1),
				timePeriod);
	}

	public static void populateSMAList(List<IndicatorValue> smaList, List<Candle> cList,
			int timePeriod, int index) {
		if (index < cList.size()) {
			double sma = ((smaList.get(smaList.size() - 1).getIndicatorValue() * timePeriod)
					+ cList.get(index).getClose() - cList.get(index - timePeriod).getClose()) / timePeriod;
			smaList.add(new IndicatorValue(cList.get(index).getTime(), sma, IndicatorType.SMA));
			populateSMAList(smaList, cList, timePeriod, ++index);
		}
	}

	public static void updateSMA(int timePeriod, Candle candle, SMAModel prevSma) {
		double preSmaVal = prevSma.getSmaMap().lastEntry().getValue().getIndicatorValue();
		double sma = (preSmaVal * timePeriod) + candle.getClose()
				- prevSma.getPrevCandles().get(prevSma.getPrevCandles().size() - timePeriod).getClose() / timePeriod;
		prevSma.getSmaMap().put(candle.getTime(), new IndicatorValue(candle.getTime(), sma, IndicatorType.SMA));
		prevSma.getPrevCandles().add(candle);
	}
}

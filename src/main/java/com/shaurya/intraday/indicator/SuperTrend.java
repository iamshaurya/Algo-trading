/**
 * 
 */
package com.shaurya.intraday.indicator;

import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import com.shaurya.intraday.enums.IndicatorType;
import com.shaurya.intraday.model.ATRModel;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.IndicatorValue;
import com.shaurya.intraday.model.SuperTrendModel;

/**
 * @author Shaurya
 *
 */
public class SuperTrend {
	public static SuperTrendModel calculateSuperTrend(List<Candle> cList, ATRModel atr, int offset) {
		TreeMap<Date, IndicatorValue> superTrendMap = new TreeMap<>();
		double prevFinalUpperBand = 0;
		double prevFinalLowerBand = 0;
		TreeMap<Date, IndicatorValue> atrMap = atr.getAtrMap();
		for (int i = 0; i < cList.size(); i++) {
			Candle c = cList.get(i);
			if (atrMap.get(c.getTime()) != null) {
				double basicUpperBand = (double) ((c.getHigh() + c.getLow()) / 2)
						+ (offset * atrMap.get(c.getTime()).getIndicatorValue());
				double basicLowerBand = (double) ((c.getHigh() + c.getLow()) / 2)
						- (offset * atrMap.get(c.getTime()).getIndicatorValue());

				double finalUpperBand = ((basicUpperBand < prevFinalUpperBand)
						|| (cList.get(i - 1).getClose() > prevFinalUpperBand)) ? basicUpperBand : prevFinalUpperBand;
				double finalLowerBand = ((basicLowerBand > prevFinalLowerBand)
						|| (cList.get(i - 1).getClose() < prevFinalLowerBand)) ? basicLowerBand : prevFinalLowerBand;

				double supperTrend = c.getClose() <= finalUpperBand ? finalUpperBand : finalLowerBand;

				superTrendMap.put(c.getTime(), new IndicatorValue(c.getTime(), supperTrend, IndicatorType.SUPER_TREND));
				prevFinalUpperBand = finalUpperBand;
				prevFinalLowerBand = finalLowerBand;
			}
		}
		return new SuperTrendModel(superTrendMap, prevFinalUpperBand, prevFinalLowerBand, cList.get(cList.size() - 1));
	}

	public static void updateSuperTrend(Candle c, ATRModel atr, SuperTrendModel superTrend, int offset) {
		double basicUpperBand = (double) ((c.getHigh() + c.getLow()) / 2)
				+ (offset * atr.getAtrMap().get(c.getTime()).getIndicatorValue());
		double basicLowerBand = (double) ((c.getHigh() + c.getLow()) / 2)
				- (offset * atr.getAtrMap().get(c.getTime()).getIndicatorValue());

		double finalUpperBand = ((basicUpperBand < superTrend.getPrevFinalUpperBand())
				|| (superTrend.getPrevCandle().getClose() > superTrend.getPrevFinalUpperBand())) ? basicUpperBand
						: superTrend.getPrevFinalUpperBand();
		double finalLowerBand = ((basicLowerBand > superTrend.getPrevFinalLowerBand())
				|| (superTrend.getPrevCandle().getClose() < superTrend.getPrevFinalLowerBand())) ? basicLowerBand
						: superTrend.getPrevFinalLowerBand();

		double supperTrend = c.getClose() <= finalUpperBand ? finalUpperBand : finalLowerBand;
		superTrend.getSuperTrendMap().put(c.getTime(),
				new IndicatorValue(c.getTime(), supperTrend, IndicatorType.SUPER_TREND));
		superTrend.setPrevFinalLowerBand(finalLowerBand);
		superTrend.setPrevFinalUpperBand(finalUpperBand);
		superTrend.setPrevCandle(c);
	}
}

/**
 * 
 */
package com.shaurya.intraday.indicator;

import static com.shaurya.intraday.util.HelperUtil.convertIndiactorValueToCandle;
import static com.shaurya.intraday.util.HelperUtil.convertListToMap;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.shaurya.intraday.enums.IndicatorType;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.IndicatorValue;
import com.shaurya.intraday.model.MACDModel;
import com.shaurya.intraday.util.HelperUtil;

/**
 * @author Shaurya
 *
 */
public class MACD {
	public static MACDModel calculateMACD(Map<Date, IndicatorValue> fastEma, Map<Date, IndicatorValue> slowEma,
			int fastEMATimePeriod) {
		List<IndicatorValue> macdList = new ArrayList<>();
		for (Entry<Date, IndicatorValue> sEma : slowEma.entrySet()) {
			double macdValue = fastEma.get(sEma.getKey()).getIndicatorValue() - sEma.getValue().getIndicatorValue();
			macdList.add(new IndicatorValue(sEma.getKey(), macdValue, IndicatorType.MACD));
		}
		int signalPeriod = (int) Math.ceil(0.75 * fastEMATimePeriod);
		return new MACDModel(convertListToMap(macdList),
				EMA.calculateEMA(signalPeriod, convertIndiactorValueToCandle(macdList)));
	}

	public static void updateMacdModel(MACDModel prevMacd, Candle candle, double fastEMAValue, double slowEMAValue,
			int fastEMATimePeriod) {
		double macd = (double) fastEMAValue - slowEMAValue;
		int signalPeriod = (int) Math.ceil(0.75 * fastEMATimePeriod);
		double signal = EMA.calculateEMA(signalPeriod,
				new Candle(candle.getSecurity(), candle.getTime(), 0, 0, 0, macd, 0),
				prevMacd.getSignalMap().lastEntry().getValue().getIndicatorValue());
		prevMacd.getMacdMap().put(candle.getTime(), new IndicatorValue(candle.getTime(), macd, IndicatorType.MACD));
		prevMacd.getSignalMap().put(candle.getTime(), new IndicatorValue(candle.getTime(), signal, IndicatorType.EMA));
	}
}

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
import com.shaurya.intraday.model.RSIModel;

/**
 * @author Shaurya
 *
 */
public class RSI {
	public static RSIModel calculateRSI(List<Candle> cList) {
		RSIModel rsi = null;
		List<IndicatorValue> rsiList = new ArrayList<>();
		double avgGain = 0;
		double avgLoss = 0;
		if (cList.size() >= 14) {
			for (int i = 1; i < 14; i++) {
				if (cList.get(i - 1).getClose() < cList.get(i).getClose()) {
					avgGain += (cList.get(i).getClose() - cList.get(i - 1).getClose());
				} else {
					avgLoss += (cList.get(i - 1).getClose() - cList.get(i).getClose());
				}
			}
			avgGain = (double) avgGain / 14;
			avgLoss = (double) avgLoss / 14;

			double firstRSI = (double) 100 - (100 / (1 + (avgGain / avgLoss)));

			rsiList.add(new IndicatorValue(cList.get(13).getTime(), firstRSI, IndicatorType.RSI));

			rsi = populateRSI(rsiList, cList, avgGain, avgLoss, 14);
		}
		rsi.setRsiMap(convertListToMap(rsiList));
		rsi.setLastCandle(cList.get(cList.size()-1));
		return rsi;
	}

	public static RSIModel populateRSI(List<IndicatorValue> rsiList, List<Candle> cList, double avgGain, double avgLoss,
			int index) {
		double avgGainLocal = avgGain;
		double avgLossLocal = avgLoss;
		while (index < cList.size()) {
			double gain = 0;
			double loss = 0;
			if (cList.get(index - 1).getClose() < cList.get(index).getClose()) {
				gain = (cList.get(index).getClose() - cList.get(index - 1).getClose());
			} else {
				loss = (cList.get(index - 1).getClose() - cList.get(index).getClose());
			}
			avgGainLocal = (double) (avgGainLocal * 13 + gain) / 14;
			avgLossLocal = (double) (avgLossLocal * 13 + loss) / 14;
			double rsi = (double) 100 - (100 / (1 + (avgGainLocal / avgLossLocal)));

			rsiList.add(new IndicatorValue(cList.get(index).getTime(), rsi, IndicatorType.RSI));

			//populateRSI(rsiList, cList, avgGain, avgLoss, ++index);
			index++;
		}
		return new RSIModel(null, avgGainLocal, avgLossLocal, null);
	}

	public static void updateRSI(Candle candle, RSIModel rsi) {
		double gain = 0;
		double loss = 0;
		if (rsi.getLastCandle().getClose() < candle.getClose()) {
			gain = (candle.getClose() - rsi.getLastCandle().getClose());
		} else {
			loss = (rsi.getLastCandle().getClose() - candle.getClose());
		}
		double avgGain = (double) (rsi.getAvgGain() * 13 + gain) / 14;
		double avgLoss = (double) (rsi.getAvgLoss() * 13 + loss) / 14;
		double rsiValue = (double) 100 - (100 / (1 + (avgGain / avgLoss)));

		rsi.getRsiMap().put(candle.getTime(), new IndicatorValue(candle.getTime(), rsiValue, IndicatorType.RSI));
		rsi.setAvgGain(avgGain);
		rsi.setAvgLoss(avgLoss);
		rsi.setLastCandle(candle);
	}
}

/**
 *
 */
package com.shaurya.intraday.indicator;

import static com.shaurya.intraday.util.HelperUtil.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.shaurya.intraday.enums.IndicatorType;
import com.shaurya.intraday.model.ATRModel;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.IndicatorValue;
import com.shaurya.intraday.util.HelperUtil;

/**
 * @author Shaurya
 *
 */
public class ATR {

  public static ATRModel calculateATR(List<Candle> cList, int period) {
    List<IndicatorValue> atrList = new ArrayList<>();
    if (cList.size() >= period) {
      double hlDiff = (double) cList.get(0).getHigh() - cList.get(0).getLow();
      double hcDiff = 0;
      double lcDiff = 0;
      double firstTR = hlDiff;
      for (int i = 1; i < period; i++) {
        hlDiff = (double) cList.get(i).getHigh() - cList.get(i).getLow();
        hcDiff = Math.abs((double) (cList.get(i).getHigh() - cList.get(i - 1).getClose()));
        lcDiff = Math.abs((double) (cList.get(i).getLow() - cList.get(i - 1).getClose()));

        firstTR += Math.max(hlDiff, Math.max(hcDiff, lcDiff));
      }
      firstTR = (double) firstTR / period;

      atrList.add(new IndicatorValue(cList.get(period - 1).getTime(), firstTR, IndicatorType.ATR));

      populateATR(atrList, cList, period, period);
    }
    return new ATRModel(convertListToMap(atrList),
        cList.size() == 0 ? null : cList.get(cList.size() - 1),
        EMA.calculateEMA(5, convertIndiactorValueToCandle(atrList)),
        EMA.calculateEMA(10, convertIndiactorValueToCandle(atrList)));
  }

  public static void populateATR(List<IndicatorValue> atrList, List<Candle> cList, int period,
      int index) {
    if (index < cList.size()) {
      double hlDiff = (double) cList.get(index).getHigh() - cList.get(index).getLow();
      double hcDiff = Math
          .abs((double) (cList.get(index).getHigh() - cList.get(index - 1).getClose()));
      double lcDiff = Math
          .abs((double) (cList.get(index).getLow() - cList.get(index - 1).getClose()));

      double tr = Math.max(hlDiff, Math.max(hcDiff, lcDiff));

      tr = (double) (atrList.get(atrList.size() - 1).getIndicatorValue() * (period - 1) + tr)
          / period;

      atrList.add(new IndicatorValue(cList.get(index).getTime(), tr, IndicatorType.ATR));

      populateATR(atrList, cList, period, ++index);
    }
  }

  public static double calculateATR(Candle candle, double prevAtr) {
    double hlDiff = (double) candle.getHigh() - candle.getLow();
    double hcDiff = (double) candle.getHigh() - candle.getClose();
    double lcDiff = (double) candle.getClose() - candle.getLow();

    double tr = Math.max(hlDiff, Math.max(hcDiff, lcDiff));

    return (double) (prevAtr * 13 + tr) / 14;
  }

  public static void updateATR(Candle candle, ATRModel atr, int period) {
    double hlDiff = (double) candle.getHigh() - candle.getLow();
    double hcDiff = Math.abs((double) (candle.getHigh() - atr.getLastCandle().getClose()));
    double lcDiff = Math.abs((double) (candle.getLow() - atr.getLastCandle().getClose()));

    double tr = Math.max(hlDiff, Math.max(hcDiff, lcDiff));

    double prevAtr = atr.getAtrMap().lastEntry().getValue().getIndicatorValue();
    double prevAtrSignal = atr.getAtrSignal().lastEntry().getValue().getIndicatorValue();
    double currentAtr = (double) (prevAtr * (period - 1) + tr) / period;
    double atrSignal = EMA.calculateEMA(5,
        new Candle(candle.getSecurity(), candle.getToken(), candle.getTime(), 0, 0, 0, currentAtr,
            0),
        prevAtrSignal);
    atr.getAtrMap()
        .put(candle.getTime(), new IndicatorValue(candle.getTime(), currentAtr, IndicatorType.ATR));
    atr.getAtrSignal()
        .put(candle.getTime(), new IndicatorValue(candle.getTime(), atrSignal, IndicatorType.EMA));
    atr.setLastCandle(candle);
  }
}

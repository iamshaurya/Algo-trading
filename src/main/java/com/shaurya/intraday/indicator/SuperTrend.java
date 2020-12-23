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

  /*SuperTrend Algorithm :

        BASIC UPPERBAND = (HIGH + LOW) / 2 + Multiplier * ATR
        BASIC LOWERBAND = (HIGH + LOW) / 2 - Multiplier * ATR

        FINAL UPPERBAND = IF( (Current BASICUPPERBAND < Previous FINAL UPPERBAND) or (Previous Close > Previous FINAL UPPERBAND))
                            THEN (Current BASIC UPPERBAND) ELSE Previous FINALUPPERBAND)
        FINAL LOWERBAND = IF( (Current BASIC LOWERBAND > Previous FINAL LOWERBAND) or (Previous Close < Previous FINAL LOWERBAND))
                            THEN (Current BASIC LOWERBAND) ELSE Previous FINAL LOWERBAND)

        SUPERTREND = IF((Previous SUPERTREND = Previous FINAL UPPERBAND) and (Current Close <= Current FINAL UPPERBAND)) THEN
                        Current FINAL UPPERBAND
                    ELSE
                        IF((Previous SUPERTREND = Previous FINAL UPPERBAND) and (Current Close > Current FINAL UPPERBAND)) THEN
                            Current FINAL LOWERBAND
                        ELSE
                            IF((Previous SUPERTREND = Previous FINAL LOWERBAND) and (Current Close >= Current FINAL LOWERBAND)) THEN
                                Current FINAL LOWERBAND
                            ELSE
                                IF((Previous SUPERTREND = Previous FINAL LOWERBAND) and (Current Close < Current FINAL LOWERBAND)) THEN
                                    Current FINAL UPPERBAND
  */
  public static SuperTrendModel calculateSuperTrend(List<Candle> cList, ATRModel atr, int offset) {
    TreeMap<Date, IndicatorValue> superTrendMap = new TreeMap<>();
    double prevFinalUpperBand = Integer.MAX_VALUE;
    double prevFinalLowerBand = Integer.MIN_VALUE;
    TreeMap<Date, IndicatorValue> atrMap = atr.getAtrMap();
    for (int i = 0; i < cList.size(); i++) {
      Candle c = cList.get(i);
      if (atrMap.get(c.getTime()) != null) {
        double basicUpperBand = (double) ((c.getHigh() + c.getLow()) / 2)
            + (offset * atrMap.get(c.getTime()).getIndicatorValue());
        double basicLowerBand = (double) ((c.getHigh() + c.getLow()) / 2)
            - (offset * atrMap.get(c.getTime()).getIndicatorValue());

        double finalUpperBand = ((basicUpperBand < prevFinalUpperBand)
            || (cList.get(i - 1).getClose() > prevFinalUpperBand)) ? basicUpperBand
            : prevFinalUpperBand;
        double finalLowerBand = ((basicLowerBand > prevFinalLowerBand)
            || (cList.get(i - 1).getClose() < prevFinalLowerBand)) ? basicLowerBand
            : prevFinalLowerBand;

        //double supperTrend = c.getClose() <= finalUpperBand ? finalUpperBand : finalLowerBand;
        double prevSupertrend = superTrendMap.get(cList.get(i - 1).getTime()) == null ? 0.0
            : superTrendMap.get(cList.get(i - 1).getTime()).getIndicatorValue();
        double superTrend = 0.0;
        if ((prevSupertrend == 0.0 || (prevSupertrend == prevFinalUpperBand)) && (c.getClose()
            <= finalUpperBand)) {
          superTrend = finalUpperBand;
        } else if ((prevSupertrend == 0.0 || (prevSupertrend == prevFinalUpperBand)) && (
            c.getClose() > finalUpperBand)) {
          superTrend = finalLowerBand;
        } else if ((prevSupertrend == 0.0 || (prevSupertrend == prevFinalLowerBand)) && (
            c.getClose() >= finalLowerBand)) {
          superTrend = finalLowerBand;
        } else if ((prevSupertrend == 0.0 || (prevSupertrend == prevFinalLowerBand)) && (
            c.getClose() < finalLowerBand)) {
          superTrend = finalUpperBand;
        }

        superTrendMap.put(c.getTime(),
            new IndicatorValue(c.getTime(), superTrend, IndicatorType.SUPER_TREND));
        prevFinalUpperBand = finalUpperBand;
        prevFinalLowerBand = finalLowerBand;
      }
    }
    return new SuperTrendModel(superTrendMap, prevFinalUpperBand, prevFinalLowerBand,
        cList.get(cList.size() - 1));
  }

  public static void updateSuperTrend(Candle c, ATRModel atr, SuperTrendModel superTrend,
      int offset) {
    double basicUpperBand = (double) ((c.getHigh() + c.getLow()) / 2)
        + (offset * atr.getAtrMap().get(c.getTime()).getIndicatorValue());
    double basicLowerBand = (double) ((c.getHigh() + c.getLow()) / 2)
        - (offset * atr.getAtrMap().get(c.getTime()).getIndicatorValue());

    double finalUpperBand = ((basicUpperBand < superTrend.getPrevFinalUpperBand())
        || (superTrend.getPrevCandle().getClose() > superTrend.getPrevFinalUpperBand()))
        ? basicUpperBand
        : superTrend.getPrevFinalUpperBand();
    double finalLowerBand = ((basicLowerBand > superTrend.getPrevFinalLowerBand())
        || (superTrend.getPrevCandle().getClose() < superTrend.getPrevFinalLowerBand()))
        ? basicLowerBand
        : superTrend.getPrevFinalLowerBand();

    double supperTrend = c.getClose() <= finalUpperBand ? finalUpperBand : finalLowerBand;
    superTrend.getSuperTrendMap().put(c.getTime(),
        new IndicatorValue(c.getTime(), supperTrend, IndicatorType.SUPER_TREND));
    superTrend.setPrevFinalLowerBand(finalLowerBand);
    superTrend.setPrevFinalUpperBand(finalUpperBand);
    superTrend.setPrevCandle(c);
  }
}

/**
 *
 */
package com.shaurya.intraday.strategy.impl;

import static com.shaurya.intraday.util.HelperUtil.stopLossReached;

import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.indicator.ATR;
import com.shaurya.intraday.indicator.SuperTrend;
import com.shaurya.intraday.model.ATRModel;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.model.SuperTrendModel;
import com.shaurya.intraday.strategy.SuperTrendStrategy;

/**
 * @author Shaurya
 *
 */
public class SuperTrendStrategyImpl implements SuperTrendStrategy {

  private List<Candle> candleList;
  private TreeSet<Candle> candle5Set;
  private ATRModel atr30;
  private Candle prevCandle;
  //private ATRModel atr10;
  private SuperTrendModel superTrend30_2;
  //private SuperTrendModel superTrend7_2;
  //private SuperTrendModel superTrend10_3;

  @Override
  public StrategyModel processTrades(Candle candle, StrategyModel openTrade, boolean updateSetup) {
    if (updateSetup) {
      candle5Set.add(candle);
      Candle candle5min = form5MinCandle();
      if (candle5min != null) {
        prevCandle = candle5min;
        updateSetup(candle5min);
        return getTradeCall(candle5min, openTrade);
      }
    } else {
      if (prevCandle != null) {
        return getTradeCall(prevCandle, openTrade);
      }
    }
    return null;
  }

  private Candle form5MinCandle() {
    Candle candle5min = null;
    if (candle5Set.size() == 5) {
      int i = 0;
      Iterator<Candle> cItr = candle5Set.iterator();
      while (cItr.hasNext()) {
        Candle c = cItr.next();
        if (i == 0) {
          candle5min = new Candle(c.getSecurity(), c.getToken(), c.getTime(), c.getOpen(),
              c.getHigh(),
              c.getLow(), c.getClose(), c.getVolume());
        } else {
          candle5min.setClose(c.getClose());
          candle5min.setHigh(Math.max(candle5min.getHigh(), c.getHigh()));
          candle5min.setLow(Math.min(candle5min.getLow(), c.getLow()));
          candle5min.setVolume(candle5min.getVolume() + c.getVolume());
        }
        i++;
      }
      candle5Set.clear();
    }
    return candle5min;
  }

  private StrategyModel getTradeCall(Candle candle, StrategyModel openTrade) {
    StrategyModel tradeCall = null;
    if (openTrade == null) {
      if (longSignal(candle)) {
        double longSl = (candle.getClose() - Math
            .min(superTrend30_2.getSuperTrendMap().lastEntry().getValue()
                .getIndicatorValue(), candle.getLow()));
        tradeCall = new StrategyModel(candle.getToken(), PositionType.LONG, longSl,
            candle.getClose(), candle.getSecurity(), null, 0, false);
      }
      if (shortSignal(candle)) {
        double shortSl = (
            Math.max(superTrend30_2.getSuperTrendMap().lastEntry().getValue().getIndicatorValue(),
                candle.getHigh()) - candle
                .getClose());
        tradeCall = new StrategyModel(candle.getToken(), PositionType.SHORT, shortSl,
            candle.getClose(), candle.getSecurity(), null, 0, false);
      }
    } else if (openTrade != null) {
      // always check for stop loss hit before exiting trade and update
      // reason in db
      if (stopLossReached(candle, openTrade)) {
        tradeCall = new StrategyModel(candle.getToken(), openTrade.getPosition(),
            (double) openTrade.getSl(), candle.getClose(), openTrade.getSecurity(),
            openTrade.getOrderId(), openTrade.getQuantity(), true);
        tradeCall.setSlhit(Boolean.TRUE);
      } else {
        if (openTrade.getPosition() == PositionType.LONG && shortSignal(candle)) {
          tradeCall = new StrategyModel(candle.getToken(), openTrade.getPosition(),
              (double) (candle.getClose() - openTrade.getTradePrice()), candle.getClose(),
              openTrade.getSecurity(),
              openTrade.getOrderId(), openTrade.getQuantity(), true);
        }
        if (openTrade.getPosition() == PositionType.SHORT && longSignal(candle)) {
          tradeCall = new StrategyModel(candle.getToken(), openTrade.getPosition(),
              (double) (openTrade.getTradePrice() - candle.getClose()), candle.getClose(),
              openTrade.getSecurity(),
              openTrade.getOrderId(), openTrade.getQuantity(), true);
        }
      }

    }
    return tradeCall;
  }

  public boolean longSignal(Candle candle) {
    //double st7_2 = superTrend7_2.getSuperTrendMap().lastEntry().getValue().getIndicatorValue();
    double st7_3 = superTrend30_2.getSuperTrendMap().lastEntry().getValue().getIndicatorValue();
    //double st10_3 = superTrend10_3.getSuperTrendMap().lastEntry().getValue().getIndicatorValue();
    return candle.getClose() > st7_3;
    //return (st7_2 < candle.getClose()) && (st7_3 < candle.getClose()) && (st10_3 < candle.getClose());
  }

  public boolean shortSignal(Candle candle) {
    //double st7_2 = superTrend7_2.getSuperTrendMap().lastEntry().getValue().getIndicatorValue();
    double st7_3 = superTrend30_2.getSuperTrendMap().lastEntry().getValue().getIndicatorValue();
    //double st10_3 = superTrend10_3.getSuperTrendMap().lastEntry().getValue().getIndicatorValue();
    return candle.getClose() < st7_3;
    //return (st7_2 > candle.getClose()) && (st7_3 > candle.getClose()) && (st10_3 > candle.getClose());
  }

  @Override
  public void initializeSetup(List<Candle> cList) {
    candleList = cList;
    candle5Set = new TreeSet<>();
    atr30 = ATR.calculateATR(cList, 30);
    //atr10 = ATR.calculateATR(cList, 10);
    superTrend30_2 = SuperTrend.calculateSuperTrend(cList, atr30, 2);
    //superTrend7_2 = SuperTrend.calculateSuperTrend(cList, atr7, 2);
    //superTrend10_3 = SuperTrend.calculateSuperTrend(cList, atr10, 3);
  }

  @Override
  public void updateSetup(Candle candle) {
    candleList.add(candle);
    atr30 = ATR.calculateATR(candleList, 30);
    superTrend30_2 = SuperTrend.calculateSuperTrend(candleList, atr30, 2);
    //ATR.updateATR(candle, atr10, 10);
    //SuperTrend.updateSuperTrend(candle, atr7, superTrend7_2, 2);
    //SuperTrend.updateSuperTrend(candle, atr10, superTrend10_3, 3);
  }

  @Override
  public void destroySetup() {
    /*
     * atr7 = null; atr10 = null; superTrend7_2 = null; superTrend7_3 =
     * null; superTrend10_3 = null;
     */
    candleList = null;
    candle5Set.clear();
    atr30 = null;
    superTrend30_2 = null;
    prevCandle = null;
  }

}

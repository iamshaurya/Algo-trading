/**
 *
 */
package com.shaurya.intraday.strategy.impl;

import static com.shaurya.intraday.util.HelperUtil.stopLossReached;

import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.strategy.OpeningRangeBreakoutStrategy;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Shaurya
 *
 */
@Slf4j
public class OpeningRangeBreakoutStrategyImpl implements OpeningRangeBreakoutStrategy {

  private TreeSet<Candle> candle15Set;
  private TreeSet<Candle> candleSet;
  private Candle first15minCandle;

  /*
   * (non-Javadoc)
   *
   * @see com.shaurya.intraday.strategy.Strategy#processTrades(java.util.List)
   */

  @Override
  public StrategyModel processTrades(Candle candle, StrategyModel openTrade, boolean updateSetup) {
    if (updateSetup) {
      candleSet.add(candle);
      Candle candle5min = form5MinCandle();
      if (candle5min != null) {
        //form the range
        if (first15minCandle == null) {
          candle15Set.add(candle5min);
          Candle candle15min = form15MinCandle();
          first15minCandle = first15minCandle == null ? candle15min : first15minCandle;
        } else {
          return getTradeCall(candle5min, openTrade);
        }
      }
    }
    return null;
  }

  private Candle form5MinCandle() {
    Candle candle5min = null;
    if (candleSet.size() == 5) {
      int i = 0;
      Iterator<Candle> cItr = candleSet.iterator();
      while (cItr.hasNext()) {
        Candle c = cItr.next();
        if (i == 0) {
          candle5min = new Candle(c.getSecurity(), c.getToken(), c.getTime(), c.getOpen(),
              c.getHigh(),
              c.getLow(), c.getClose(), 0);
        } else {
          candle5min.setClose(c.getClose());
          candle5min.setHigh(Math.max(candle5min.getHigh(), c.getHigh()));
          candle5min.setLow(Math.min(candle5min.getLow(), c.getLow()));
        }
        i++;
      }
      candleSet.clear();
    }
    return candle5min;
  }

  private Candle form15MinCandle() {
    Candle candle15min = null;
    if (candle15Set.size() == 3) {
      int i = 0;
      Iterator<Candle> cItr = candle15Set.iterator();
      while (cItr.hasNext()) {
        Candle c = cItr.next();
        if (i == 0) {
          candle15min = new Candle(c.getSecurity(), c.getToken(), c.getTime(), c.getOpen(),
              c.getHigh(),
              c.getLow(), c.getClose(), 0);
        } else {
          candle15min.setClose(c.getClose());
          candle15min.setHigh(Math.max(candle15min.getHigh(), c.getHigh()));
          candle15min.setLow(Math.min(candle15min.getLow(), c.getLow()));
        }
        i++;
      }
      candle15Set.clear();
    }
    return candle15min;
  }

  private StrategyModel getTradeCall(Candle candle, StrategyModel openTrade) {
    StrategyModel tradeCall = null;
    if (openTrade == null) {
      if (candle.getClose() > first15minCandle.getHigh()) {
        double longSl = Math
            .min((candle.getClose() - first15minCandle.getLow()), (0.01 * candle.getClose()));
        tradeCall = new StrategyModel(candle.getToken(), PositionType.LONG, longSl,
            candle.getClose(),
            candle.getSecurity(), null, 0, false);
      }
      if (candle.getClose() < first15minCandle.getLow()) {
        double shortSl = Math.min((first15minCandle.getHigh() - candle.getClose()),
            (0.01 * candle.getClose()));
        tradeCall = new StrategyModel(candle.getToken(), PositionType.SHORT, shortSl,
            candle.getClose(),
            candle.getSecurity(), null, 0, false);
      }
    } else {
      if (stopLossReached(candle, openTrade)) {
        tradeCall = new StrategyModel(candle.getToken(), openTrade.getPosition(),
            openTrade.getSl(), candle.getClose(), openTrade.getSecurity(),
            openTrade.getOrderId(), openTrade.getQuantity(), true);
      }
    }
    return tradeCall;

  }

  @Override
  public void initializeSetup(List<Candle> cList) {
    candle15Set = new TreeSet<>();
    candleSet = new TreeSet<>();
  }

  @Override
  public void destroySetup() {
    first15minCandle = null;
    candle15Set.clear();
    candleSet = null;
  }

  @Override
  public void updateSetup(Candle candle) {
    log.info("updateSetup date :: " + candle.getTime());
  }

}

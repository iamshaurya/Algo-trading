package com.shaurya.intraday.strategy.impl;

import static com.shaurya.intraday.util.HelperUtil.stopLossReached;

import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.strategy.PreOpenStrategy;
import com.shaurya.intraday.util.CandlestickPatternHelper;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class PreOpenStrategyImpl implements PreOpenStrategy {

  //5 orb min
  //range candle count = n min/5 | 5/5 = 1
  private static final int rangeCandleCount = 1;
  private TreeSet<Candle> rangeCandleSet;
  private TreeSet<Candle> candleSet;
  private Candle firstRangeCandle;
  private Candle prevCandle;
  private Boolean fullGapUp;
  private int tradeCount = 0;
  private static final int tradeCountThreshold = 1;

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
        if (firstRangeCandle == null) {
          rangeCandleSet.add(candle5min);
          Candle rangeCandle = formRangeCandle();
          firstRangeCandle = firstRangeCandle == null ? rangeCandle : firstRangeCandle;
        }
        return getTradeCall(candle5min, openTrade);

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

  private Candle formRangeCandle() {
    Candle rangeCandle = null;
    if (rangeCandleSet.size() == rangeCandleCount) {
      int i = 0;
      Iterator<Candle> cItr = rangeCandleSet.iterator();
      while (cItr.hasNext()) {
        Candle c = cItr.next();
        if (i == 0) {
          rangeCandle = new Candle(c.getSecurity(), c.getToken(), c.getTime(), c.getOpen(),
              c.getHigh(),
              c.getLow(), c.getClose(), 0);
        } else {
          rangeCandle.setClose(c.getClose());
          rangeCandle.setHigh(Math.max(rangeCandle.getHigh(), c.getHigh()));
          rangeCandle.setLow(Math.min(rangeCandle.getLow(), c.getLow()));
        }
        i++;
      }
      rangeCandleSet.clear();
    }
    return rangeCandle;
  }

  private StrategyModel getTradeCall(Candle candle, StrategyModel openTrade) {
    log.error((rangeCandleCount * 5) + "min range high {}, low {}", firstRangeCandle.getHigh(),
        firstRangeCandle.getLow());
    log.error("current close {}", candle.getClose());
    StrategyModel tradeCall = null;
    if (openTrade == null) {
      if (CandlestickPatternHelper.dojiOrSpininTop(firstRangeCandle)) {
        return null;
      }
      PositionType positionType = null;
      if (this.fullGapUp) {
        if (isStrongGapUp()) {
          positionType = PositionType.LONG;
        } else {
          positionType = PositionType.SHORT;
        }
      } else {
        if (isStrongGapDown()) {
          positionType = PositionType.SHORT;
        } else {
          positionType = PositionType.LONG;
        }
      }
      if (PositionType.LONG.equals(positionType) && tradePermitted()) {
        double longSl = (firstRangeCandle.getClose() - firstRangeCandle.getLow());
        tradeCall = new StrategyModel(candle.getToken(), PositionType.LONG, longSl,
            firstRangeCandle.getClose(),
            candle.getSecurity(), null, 0, false);
        tradeCount++;
      }
      if (PositionType.SHORT.equals(positionType) && tradePermitted()) {
        double shortSl = (firstRangeCandle.getHigh() - candle.getClose());
        tradeCall = new StrategyModel(candle.getToken(), PositionType.SHORT, shortSl,
            firstRangeCandle.getClose(),
            candle.getSecurity(), null, 0, false);
        tradeCount++;
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

  private boolean tradePermitted() {
    /*return (firstRangeCandle.getHigh() - firstRangeCandle.getLow()) <= (rangePercentage
     * firstRangeCandle.getClose());*/
    return tradeCount < tradeCountThreshold;
  }

  private boolean isStrongGapUp() {
    /*return (firstRangeCandle.getClose() > firstRangeCandle.getOpen()) && (
        Math.abs(firstRangeCandle.getClose() - firstRangeCandle.getOpen()) >= 0.7 * (
            firstRangeCandle.getHigh() - firstRangeCandle.getLow()));*/
    return (firstRangeCandle.getClose() > firstRangeCandle.getOpen());
  }

  private boolean isStrongGapDown() {
    /*return (firstRangeCandle.getClose() < firstRangeCandle.getOpen()) && (
        Math.abs(firstRangeCandle.getClose() - firstRangeCandle.getOpen()) >= 0.7 * (
            firstRangeCandle.getHigh() - firstRangeCandle.getLow()));*/
    return (firstRangeCandle.getClose() < firstRangeCandle.getOpen());
  }

  @Override
  public void initializeSetup(List<Candle> cList) {
    rangeCandleSet = new TreeSet<>();
    candleSet = new TreeSet<>();
    tradeCount = 0;
  }

  @Override
  public void setFullGap(Boolean fullGapUp) {
    this.fullGapUp = fullGapUp;
  }

  @Override
  public void destroySetup() {
    firstRangeCandle = null;
    prevCandle = null;
    rangeCandleSet.clear();
    candleSet.clear();
    rangeCandleSet = null;
    candleSet = null;
    tradeCount = 0;
  }

  @Override
  public void updateSetup(Candle candle) {
    log.info("updateSetup date :: " + candle.getTime());
  }

}

package com.shaurya.intraday.strategy.impl;

import static com.shaurya.intraday.util.HelperUtil.stopLossReached;

import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.strategy.OpeningRangeBreakoutV2Strategy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OpeningRangeBreakoutV2StrategyImpl implements OpeningRangeBreakoutV2Strategy {

  private final double deviationPercentage = 0.005;
  private Double high;
  private Double low;
  private Double maxAuxHigh;
  private Double minAuxLow;
  private ReferenceType reference;
  private Boolean orbHappend;
  //5 orb min
  //range candle count = n min/5 | 5/5 = 1
  private static final int rangeCandleCount = 1;
  private TreeSet<Candle> rangeCandleSet;
  private TreeSet<Candle> candleSet;
  private Candle firstRangeCandle;
  private Candle prevCandle;

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
        } else {
          prevCandle = candle5min;
          updateSetup(candle5min);
          StrategyModel tradeCall = getTradeCall(candle5min, openTrade);
          return tradeCall;
        }
      }
    } else {
      if (firstRangeCandle != null && prevCandle != null) {
        return getTradeCall(prevCandle, openTrade);
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
      high = rangeCandle.getHigh();
      low = rangeCandle.getLow();
    }
    return rangeCandle;
  }

  private StrategyModel getTradeCall(Candle candle, StrategyModel openTrade) {
    log.error("Current high {}, low {}", high, low);
    log.error("current close {}", candle.getClose());
    StrategyModel tradeCall = null;
    if (openTrade == null) {
      if (candle.getClose() > high) {
        double longSl = (candle.getClose() - low);
        tradeCall = new StrategyModel(candle.getToken(), PositionType.LONG, longSl,
            candle.getClose(),
            candle.getSecurity(), null, 0, false);
        if (reference == null && !orbHappend) {
          reference = ReferenceType.LOW;
          orbHappend = Boolean.TRUE;
          updateSetup(candle);
        }
      }
      if (candle.getClose() < low) {
        double shortSl = (high - candle.getClose());
        tradeCall = new StrategyModel(candle.getToken(), PositionType.SHORT, shortSl,
            candle.getClose(),
            candle.getSecurity(), null, 0, false);
        if (reference == null && !orbHappend) {
          reference = ReferenceType.HIGH;
          orbHappend = Boolean.TRUE;
          updateSetup(candle);
        }
      }
    } else {
      if (stopLossReached(candle, openTrade)) {
        tradeCall = new StrategyModel(candle.getToken(), openTrade.getPosition(),
            openTrade.getSl(), candle.getClose(), openTrade.getSecurity(),
            openTrade.getOrderId(), openTrade.getQuantity(), true);
      } else {
        switch (openTrade.getPosition()) {
          case LONG:
            Double currentLongSl = BigDecimal
                .valueOf(openTrade.getTradePrice() - openTrade.getSl())
                .setScale(2, RoundingMode.HALF_UP).doubleValue();
            if (low > currentLongSl) {
              log.error("Current open long trade {}", openTrade);
              log.error("Trailing long sl to {} for canlde {}", low, candle);
              Double newLongSl = BigDecimal
                  .valueOf(openTrade.getTradePrice() - low)
                  .setScale(2, RoundingMode.HALF_UP).doubleValue();
              log.error("Trailing long sl to new long sl {}", newLongSl);
              tradeCall = new StrategyModel(candle.getToken(), openTrade.getPosition(),
                  newLongSl, openTrade.getTradePrice(), openTrade.getSecurity(),
                  openTrade.getOrderId(), openTrade.getQuantity(), false);
              tradeCall.setTrailSl(Boolean.TRUE);
            }
            break;
          case SHORT:
            Double currentShortSl = BigDecimal
                .valueOf(openTrade.getTradePrice() + openTrade.getSl())
                .setScale(2, RoundingMode.HALF_UP).doubleValue();
            if (high < currentShortSl) {
              log.error("Current open short trade {}", openTrade);
              log.error("Trailing short sl to {} for canlde {}", high, candle);
              Double newShortSl = BigDecimal
                  .valueOf(high - openTrade.getTradePrice())
                  .setScale(2, RoundingMode.HALF_UP).doubleValue();
              log.error("Trailing short sl to new short sl {}", newShortSl);
              tradeCall = new StrategyModel(candle.getToken(), openTrade.getPosition(),
                  newShortSl, openTrade.getTradePrice(), openTrade.getSecurity(),
                  openTrade.getOrderId(), openTrade.getQuantity(), false);
              tradeCall.setTrailSl(Boolean.TRUE);
            }
            break;
        }
      }
    }
    return tradeCall;

  }

  @Override
  public void initializeSetup(List<Candle> cList) {
    rangeCandleSet = new TreeSet<>();
    candleSet = new TreeSet<>();
    orbHappend = Boolean.FALSE;
    maxAuxHigh = Double.MIN_VALUE;
    minAuxLow = Double.MAX_VALUE;
  }

  @Override
  public void destroySetup() {
    firstRangeCandle = null;
    prevCandle = null;
    rangeCandleSet.clear();
    candleSet.clear();
    rangeCandleSet = null;
    candleSet = null;

    orbHappend = null;
    maxAuxHigh = Double.MIN_VALUE;
    minAuxLow = Double.MAX_VALUE;
    high = Double.MIN_VALUE;
    low = Double.MAX_VALUE;
    reference = null;
  }

  @Override
  public void updateSetup(Candle candle) {
    log.info("updateSetup date :: " + candle.getTime());
    if (orbHappend) {
      updateSwingHighLow(Boolean.FALSE, candle);
    }
  }

  private void updateSwingHighLow(final Boolean recursive, final Candle candle) {
    if (reference.equals(ReferenceType.LOW)) {
      //check and update auxillary high
      if (candle.getHigh() >= ((1 + deviationPercentage) * low) && candle.getHigh() > maxAuxHigh) {
        maxAuxHigh = Math.max(candle.getHigh(), maxAuxHigh);
      } else {
        //check and udpate if new high confirmed
        if (candle.getLow() <= ((1 - deviationPercentage) * maxAuxHigh) && !recursive) {
          high = maxAuxHigh;
          maxAuxHigh = Double.MIN_VALUE;
          reference = ReferenceType.HIGH;
          updateSwingHighLow(Boolean.TRUE, candle);
          return;
        }
      }
    }

    if (reference.equals(ReferenceType.HIGH)) {
      //check and update auxillary low
      if (candle.getLow() <= ((1 - deviationPercentage) * high) && candle.getLow() < minAuxLow) {
        minAuxLow = Math.min(candle.getLow(), minAuxLow);
      } else {
        //check and udpate if new low confirmed
        if (candle.getHigh() >= ((1 + deviationPercentage) * minAuxLow) && !recursive) {
          low = minAuxLow;
          minAuxLow = Double.MAX_VALUE;
          reference = ReferenceType.LOW;
          updateSwingHighLow(Boolean.TRUE, candle);
          return;
        }
      }
    }
  }

  public enum ReferenceType {
    LOW, HIGH
  }

}

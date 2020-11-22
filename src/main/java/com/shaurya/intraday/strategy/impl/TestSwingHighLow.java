package com.shaurya.intraday.strategy.impl;

import static com.shaurya.intraday.util.HelperUtil.stopLossReached;

import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.strategy.impl.OpeningRangeBreakoutV2StrategyImpl.ReferenceType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class TestSwingHighLow {

  private static final double deviationPercentage = 0.005;
  private static Double high;
  private static Double low;
  private static Double maxAuxHigh = Double.MIN_VALUE;
  private static Double minAuxLow = Double.MAX_VALUE;
  private static ReferenceType reference;
  private static Boolean orbHappend = Boolean.FALSE;
  private static StrategyModel openTrade;

  public static void mainL(String[] args){
    Double a = BigDecimal.valueOf(193.1500 + 1.8).setScale(2, RoundingMode.HALF_UP).doubleValue();
    Double b = BigDecimal
        .valueOf(4.5).divide(BigDecimal.valueOf(500))
        .doubleValue();

    System.out.println("deviation : "+b);
  }

  public static void mainT(String[] args) {
    List<Candle> data = mockCandles();
    high = data.get(0).getHigh();
    low = data.get(0).getLow();

    for (int i = 1; i < data.size(); i++) {
      System.out.println("high " + high + " low" + low);
      Candle currentCandle = data.get(i);
      if (orbHappend) {
        updateSwingHighLow(Boolean.FALSE, currentCandle);
      }
      if (openTrade == null) {
        if (currentCandle.getClose() > high) {
          Double longSlPoint = currentCandle.getClose() - low;
          openTrade = new StrategyModel(currentCandle.getToken(), PositionType.LONG, longSlPoint,
              currentCandle.getClose(),
              currentCandle.getSecurity(), null, 0, false);
          //System.out.println("Buy " + JsonParser.objectToJson(openTrade));
          if (!orbHappend && reference == null) {
            orbHappend = Boolean.TRUE;
            reference = ReferenceType.LOW;
            updateSwingHighLow(Boolean.FALSE, currentCandle);
          }
        }
        if (currentCandle.getClose() < low) {
          Double shortSlPoint = high - currentCandle.getClose();
          openTrade = new StrategyModel(currentCandle.getToken(), PositionType.SHORT, shortSlPoint,
              currentCandle.getClose(),
              currentCandle.getSecurity(), null, 0, false);
          //System.out.println("Sell " + JsonParser.objectToJson(openTrade));
          if (!orbHappend && reference == null) {
            orbHappend = Boolean.TRUE;
            reference = ReferenceType.HIGH;
            updateSwingHighLow(Boolean.FALSE, currentCandle);
          }
        }
      } else {
        if (stopLossReached(currentCandle, openTrade)) {
          openTrade = null;
          i--;
          continue;
        } else {
          switch (openTrade.getPosition()) {
            case LONG:
              Double currentLongSl = openTrade.getTradePrice() - openTrade.getSl();
              if (low > currentLongSl) {
                /*System.out.println("Current open long trade " + JsonParser.objectToJson(openTrade));
                System.out.println("Trailing long sl to " + low + " for canlde {}" +
                    JsonParser.objectToJson(currentCandle));*/
                Double newLongSl = (openTrade.getTradePrice() - low);
                //System.out.println("Trailing long sl to new long sl {}" + newLongSl);
                openTrade = new StrategyModel(currentCandle.getToken(), openTrade.getPosition(),
                    newLongSl, openTrade.getTradePrice(), openTrade.getSecurity(),
                    openTrade.getOrderId(), openTrade.getQuantity(), false);
                openTrade.setTrailSl(Boolean.TRUE);
              }
              break;
            case SHORT:
              Double currentShortSl = openTrade.getTradePrice() + openTrade.getSl();
              if (high < currentShortSl) {
                /*System.out
                    .println("Current open short trade {}" + JsonParser.objectToJson(openTrade));
                System.out.println("Trailing short sl to" + high + " for canlde {}" + JsonParser
                    .objectToJson(currentCandle));*/
                Double newShortSl = high - openTrade.getTradePrice();
                //System.out.println("Trailing long sl to new short sl {}" + newShortSl);
                openTrade = new StrategyModel(currentCandle.getToken(), openTrade.getPosition(),
                    newShortSl, openTrade.getTradePrice(), openTrade.getSecurity(),
                    openTrade.getOrderId(), openTrade.getQuantity(), false);
                openTrade.setTrailSl(Boolean.TRUE);
              }
              break;
          }
        }
      }
    }

  }

  private static List<Candle> mockCandles() {
    List<Candle> candles = new ArrayList<>();
    Candle c1 = new Candle(null, 0, null, 201.05, 201.25, 198.15, 198.25, 0);
    Candle c2 = new Candle(null, 0, null, 198.35, 199.2, 198.05, 198.90, 0);
    Candle c3 = new Candle(null, 0, null, 198.80, 198.95, 198.3, 198.85, 0);
    Candle c4 = new Candle(null, 0, null, 198.95, 199.15, 198.45, 198.85, 0);
    Candle c5 = new Candle(null, 0, null, 198.9, 201, 198.85, 201, 0);
    Candle c6 = new Candle(null, 0, null, 201, 202, 200.95, 201.3, 0);
    Candle c7 = new Candle(null, 0, null, 201.3, 202.5, 201.2, 202.45, 0);
    Candle c8 = new Candle(null, 0, null, 202.5, 202.5, 201.1, 201.45, 0);
    Candle c9 = new Candle(null, 0, null, 201.50, 201.75, 201, 201.7, 0);
    Candle c10 = new Candle(null, 0, null, 201.75, 202.1, 201.35, 201.4, 0);
    Candle c11 = new Candle(null, 0, null, 201.40, 201.55, 201.05, 201.35, 0);
    Candle c12 = new Candle(null, 0, null, 201.35, 201.4, 201.05, 201.15, 0);
    Candle c13 = new Candle(null, 0, null, 201.1, 201.2, 200.7, 200.85, 0);
    Candle c14 = new Candle(null, 0, null, 200.8, 201.2, 200.05, 200.2, 0);
    Candle c15 = new Candle(null, 0, null, 200.25, 200.25, 199.3, 199.5, 0);
    Candle c16 = new Candle(null, 0, null, 199.5, 199.8, 198.8, 199.65, 0);
    Candle c17 = new Candle(null, 0, null, 199.65, 199.75, 199, 199.1, 0);
    Candle c18 = new Candle(null, 0, null, 199.05, 199.3, 198.85, 198.85, 0);
    Candle c19 = new Candle(null, 0, null, 198.95, 199.7, 198.80, 199.7, 0);
    Candle c20 = new Candle(null, 0, null, 199.7, 200, 199.65, 199.7, 0);
    Candle c21 = new Candle(null, 0, null, 199.7, 200, 199.7, 199.9, 0);
    Candle c22 = new Candle(null, 0, null, 199.9, 200.2, 199.85, 199.95, 0);
    Candle c23 = new Candle(null, 0, null, 199.9, 200.15, 199.45, 199.7, 0);
    Candle c24 = new Candle(null, 0, null, 199.6, 199.7, 199.5, 199.65, 0);
    Candle c25 = new Candle(null, 0, null, 199.6, 199.65, 199.3, 199.6, 0);
    Candle c26 = new Candle(null, 0, null, 199.55, 199.65, 199.3, 199.4, 0);
    Candle c27 = new Candle(null, 0, null, 199.4, 199.45, 199.1, 199.35, 0);

    candles.add(c1);
    candles.add(c2);
    candles.add(c3);
    candles.add(c4);
    candles.add(c5);
    candles.add(c6);
    candles.add(c7);
    candles.add(c8);
    candles.add(c9);
    candles.add(c10);
    candles.add(c11);
    candles.add(c12);
    candles.add(c13);
    candles.add(c14);
    candles.add(c15);
    candles.add(c16);
    candles.add(c17);
    candles.add(c18);
    candles.add(c19);
    candles.add(c20);
    candles.add(c21);
    candles.add(c22);
    candles.add(c23);
    candles.add(c24);
    candles.add(c25);
    candles.add(c26);
    candles.add(c27);

    return candles;
  }

  private static void updateSwingHighLow(final Boolean recursive, final Candle candle) {
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
}

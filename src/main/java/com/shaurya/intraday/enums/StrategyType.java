/**
 *
 */
package com.shaurya.intraday.enums;

/**
 * @author Shaurya
 *
 */
public enum StrategyType {
  EMA_RSI(1), EMA_MACD_RSI(2), MACD_RSI(3), MACD_HISTOGRAM(4), OPEN_HIGH_LOW(5), HEIKIN_ASHI_OHL(
      6), OPENING_RANGE_BREAKOUT(7), GANN_SQUARE_9(8), SUPER_TREND(9), OPENING_RANGE_BREAKOUT_V2(
      10), PRE_OPEN_GAPPER(11);
  private int id;

  private StrategyType(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public static StrategyType getEnumById(int id) {
    for (StrategyType e : StrategyType.values()) {
      if (e.getId() == id) {
        return e;
      }
    }
    return null;
  }

}

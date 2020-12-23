/**
 *
 */
package com.shaurya.intraday.model;

import java.util.Date;
import java.util.TreeMap;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Shaurya
 *
 */
@Getter
@Setter
public class ATRModel {

  private TreeMap<Date, IndicatorValue> atrMap;
  private TreeMap<Date, IndicatorValue> atrSignal;
  private TreeMap<Date, IndicatorValue> atrSignalSlow;
  private Candle lastCandle;

  public ATRModel(TreeMap<Date, IndicatorValue> atrMap, Candle lastCandle,
      TreeMap<Date, IndicatorValue> atrSignal, TreeMap<Date, IndicatorValue> atrSignalSlow) {
    super();
    this.atrMap = atrMap;
    this.lastCandle = lastCandle;
    this.atrSignal = atrSignal;
    this.atrSignalSlow = atrSignalSlow;
  }

}

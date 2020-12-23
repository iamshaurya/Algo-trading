package com.shaurya.intraday.model;

import java.util.Date;
import java.util.TreeMap;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ADRModel {

  private TreeMap<Date, IndicatorValue> adrMap;
  private TreeMap<Date, IndicatorValue> adrSignal;
  private Candle lastCandle;

  public ADRModel(TreeMap<Date, IndicatorValue> adrMap, Candle lastCandle,
      TreeMap<Date, IndicatorValue> adrSignal) {
    super();
    this.adrMap = adrMap;
    this.lastCandle = lastCandle;
    this.adrSignal = adrSignal;
  }

}


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
public class MACDModel {
	private TreeMap<Date, IndicatorValue> macdMap;
	private TreeMap<Date, IndicatorValue> signalMap;

	public MACDModel(TreeMap<Date, IndicatorValue> macdMap, TreeMap<Date, IndicatorValue> signalMap) {
		super();
		this.macdMap = macdMap;
		this.signalMap = signalMap;
	}

}

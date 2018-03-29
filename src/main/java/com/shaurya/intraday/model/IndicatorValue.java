package com.shaurya.intraday.model;

import java.util.Date;

import com.shaurya.intraday.enums.IndicatorType;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 */

/**
 * @author Shaurya
 *
 */
@Getter
@Setter
public class IndicatorValue {
	private Date date;
	private double indicatorValue;
	private IndicatorType type;

	public IndicatorValue(Date date, double iv, IndicatorType type) {
		this.date = date;
		this.indicatorValue = iv;
		this.type = type;
	}

	@Override
	public String toString() {
		return "IndicatorValue [date=" + date + ", indicatorValue=" + indicatorValue + ", type=" + type + "]";
	}

}

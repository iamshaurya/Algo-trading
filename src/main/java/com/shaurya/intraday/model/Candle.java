package com.shaurya.intraday.model;

import java.util.Date;

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
public class Candle implements Comparable<Candle> {
	private String security;
	private long token;
	private Date time;
	private double open;
	private double high;
	private double low;
	private double close;
	private double volume;

	public Candle(String security, long token, Date time, double open, double high, double low, double close,
			double volume) {
		this.security = security;
		this.token = token;
		this.time = time;
		this.open = open;
		this.high = high;
		this.low = low;
		this.close = close;
		this.volume = volume;
	}

	@Override
	public int compareTo(Candle o) {
		Date a = this.time;
		Date b = o.time;
		if (a.before(b)) {
			return -1;
		} else if (a.after(b)) {
			return 1;
		} else {
			return 0;
		}
	}

	@Override
	public String toString() {
		return "Candle [security=" + security + ", token=" + token + ", time=" + time + ", open=" + open + ", high="
				+ high + ", low=" + low + ", close=" + close + ", volume=" + volume + "]";
	}

}

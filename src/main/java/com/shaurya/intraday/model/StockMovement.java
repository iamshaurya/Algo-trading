/**
 * 
 */
package com.shaurya.intraday.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Shaurya
 *
 */
@Getter
@Setter
@ToString
public class StockMovement implements Comparable<StockMovement> {
	private String security;
	private double token;
	private double prevDayLtp;
	private double ltp;
	private double move;

	public StockMovement(String security, double token, double prevDayLtp, double ltp) {
		super();
		this.security = security;
		this.token = token;
		this.prevDayLtp = prevDayLtp;
		this.ltp = ltp;
		this.move = (double) ((this.ltp - this.prevDayLtp) / this.prevDayLtp) * 100;
	}

	@Override
	public int compareTo(StockMovement o) {
		if (this.move > o.move) {
			return -1;
		} else if (this.move < o.move) {
			return 1;
		} else {
			return 0;
		}
	}
	
	@Override
	public boolean equals(Object o){
		if(o == null){
			return false;
		}
		StockMovement sm = (StockMovement)o;
		if(this.token == sm.getToken()){
			return true;
		}
		return false;
	}
	
	public void updateLtp(double ltp){
		this.ltp = ltp;
		this.move = (double) ((this.ltp - this.prevDayLtp) / this.prevDayLtp) * 100;
	}

}

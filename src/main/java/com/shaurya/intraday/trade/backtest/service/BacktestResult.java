/**
 * 
 */
package com.shaurya.intraday.trade.backtest.service;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Shaurya
 *
 */
@Getter
@Setter
public class BacktestResult {
	private Integer successfull;
	private Integer unsuccessfull;
	private double pnl;
	
	public BacktestResult(Integer successfull, Integer unsuccessfull, double pnl){
		this.successfull = successfull;
		this.unsuccessfull = unsuccessfull;
		this.pnl = pnl;
	}
}

/**
 * 
 */
package com.shaurya.intraday.trade.service;

import java.util.Calendar;
import java.util.Date;

import com.shaurya.intraday.util.HelperUtil;

/**
 * @author Shaurya
 *
 */
public class TestDate {

	/**
	 * @param args
	 */
	public static void mainTs(String[] args) {
		double pnl1 = ((444.05 - 446.3) * 184) - brokerageCharge(82119.20, 81705.20);
		System.out.println(pnl1);
		double pnl2 = ((443.95 - 445.20) * 184) - brokerageCharge(81916.8 , 81686.8);
		System.out.println(pnl2);
		System.out.println(pnl1+pnl2);
		System.out.println((-664) - brokerageCharge(164036 , 163392));
		
	}

	private static double brokerageCharge(double buyTradePrice, double sellTradePrice) {
		double turnover = (buyTradePrice + sellTradePrice);
		double brokerage = Math.min((buyTradePrice * 0.0001), 20)
				+ Math.min((sellTradePrice * 0.0001), 20);
		double stt = 0.00025 * (sellTradePrice);
		double transactionCharge = (0.0000325 * buyTradePrice) + (0.0000325 * sellTradePrice);
		double gst = 0.18 * (transactionCharge + brokerage);
		double sebiCharge = (0.0000015 * buyTradePrice) + (0.0000015 * sellTradePrice);
		double stampCharge = (0.00003 * buyTradePrice) + (0.00003 * sellTradePrice);

		return brokerage + stt + transactionCharge + gst + sebiCharge + stampCharge;
	}

}

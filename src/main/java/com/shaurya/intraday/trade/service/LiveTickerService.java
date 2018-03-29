/**
 * 
 */
package com.shaurya.intraday.trade.service;

import java.util.ArrayList;

import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

/**
 * @author Shaurya
 *
 */
public interface LiveTickerService {
	public void init(KiteConnect sdkClient) throws KiteException;

	public boolean checkConnection();

	public void disconnect();

	public void unsubscribe(ArrayList<Long> tokens);

	public void subscribe(ArrayList<Long> tokens);

	public void subscribeTradeStock(ArrayList<Long> tokens);

	public void subscribeMonitorStock(ArrayList<Long> tokens);
}

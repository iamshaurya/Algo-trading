/**
 * 
 */
package com.shaurya.intraday.trade.service;

import java.io.IOException;

import org.json.JSONException;

import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.StrategyModel;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

/**
 * @author Shaurya
 *
 */
public interface TradeProcessor {
	public StrategyModel getTradeCall(Candle candle);

	public void initializeStrategyMap() throws IOException, KiteException, JSONException;

	public void destroyStrategyMap();

	public void updateNifty50Ltp(double ltp);

	public void updateTopGainerLoser(double token, double ltp);
}

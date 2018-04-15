/**
 * 
 */
package com.shaurya.intraday.trade.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.shaurya.intraday.enums.StrategyType;
import com.shaurya.intraday.model.StrategyModel;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

/**
 * @author Shaurya
 *
 */
public interface StockScreener {
	public List<String> fetchTopVolatileStock();

	public Map<StrategyType, List<StrategyModel>> getFilteredStocks() throws IOException, KiteException;

	public void updateStrategyStocks();

	public List<String> fetchTopAnnualVolatileStock();

	List<String> fetchTopAnnualVolatileStockForBacktest();
}

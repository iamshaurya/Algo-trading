package com.shaurya.intraday.query.builder;

import java.util.HashMap;

import com.shaurya.intraday.model.CustomQueryHolder;

public class StockScreenerQueryBuilder {
	public static CustomQueryHolder queryToFetchMostVolatileStocks() {
		StringBuilder sb = new StringBuilder();
		sb.append(
				"select prev_nse_volatility.symbol, prev_nse_volatility.daily_vol from prev_nse_volatility inner join nse_100 on nse_100.symbol=prev_nse_volatility.symbol where prev_nse_volatility.yearly_vol >= 35 and prev_nse_volatility.daily_vol >= 1.5");
		CustomQueryHolder cq = new CustomQueryHolder();
		cq.setQueryString(sb.toString());
		cq.setInParamMap(new HashMap<>());
		return cq;
	}
	
	public static CustomQueryHolder queryToFetchMostAnnualVolatileStocks() {
		StringBuilder sb = new StringBuilder();
		sb.append(
				"select prev_nse_volatility.symbol, prev_nse_volatility.daily_vol from prev_nse_volatility inner join nse_100 on nse_100.symbol=prev_nse_volatility.symbol where prev_nse_volatility.yearly_vol >= 35");
		CustomQueryHolder cq = new CustomQueryHolder();
		cq.setQueryString(sb.toString());
		cq.setInParamMap(new HashMap<>());
		return cq;
	}

	public static String queryToFlushStockList() {
		StringBuilder sb = new StringBuilder();
		sb.append("delete from nse_100");
		return sb.toString();
	}

	public static String queryToFlushVolatileStocks() {
		StringBuilder sb = new StringBuilder();
		sb.append("delete from prev_nse_volatility");
		return sb.toString();
	}
	
	public static String queryToFlushTradeStrategy() {
		StringBuilder sb = new StringBuilder();
		sb.append("delete from trade_strategy");
		return sb.toString();
	}

	// 0- unprocessed, 1-processed
	public static CustomQueryHolder queryToFetchVolatileStock() {
		CustomQueryHolder cq = new CustomQueryHolder();
		StringBuilder sb = new StringBuilder();
		sb.append("select vs from VolatileStock vs");
		cq.setQueryString(sb.toString());
		cq.setInParamMap(new HashMap<>());
		return cq;
	}

	// 0- unprocessed, 1-processed
	public static CustomQueryHolder queryToFetchUnprocessedVolatileStock() {
		CustomQueryHolder cq = new CustomQueryHolder();
		StringBuilder sb = new StringBuilder();
		sb.append("select vs from VolatileStock vs where vs.state = 0");
		cq.setQueryString(sb.toString());
		cq.setInParamMap(new HashMap<>());
		return cq;
	}

	public static String cleanVolatileStocks() {
		StringBuilder sb = new StringBuilder();
		sb.append("delete from volatile_stock");
		return sb.toString();
	}
}

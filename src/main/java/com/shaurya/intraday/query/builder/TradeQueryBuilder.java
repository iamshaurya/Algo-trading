/**
 * 
 */
package com.shaurya.intraday.query.builder;

import java.util.HashMap;
import java.util.Map;

import com.shaurya.intraday.model.CustomQueryHolder;

/**
 * @author Shaurya
 *
 */
public class TradeQueryBuilder {

	public static CustomQueryHolder queryToFetchOpenTradeBySecurityName(String securityName) {
		CustomQueryHolder cq = new CustomQueryHolder();
		StringBuilder sb = new StringBuilder();
		sb.append(
				"select trade from Trade trade where trade.securityName = :securityName and trade.tradeExitPrice = null and trade.status = 1 ");
		sb.append(" order by trade.tradeDate desc");
		Map<String, Object> inParamMap = new HashMap<>();
		inParamMap.put("securityName", securityName);
		cq.setInParamMap(inParamMap);
		cq.setQueryString(sb.toString());
		return cq;
	}

	public static CustomQueryHolder queryToFetchSecurityTradeStrategy() {
		CustomQueryHolder cq = new CustomQueryHolder();
		StringBuilder sb = new StringBuilder();
		sb.append("select ts from TradeStrategy ts where ts.day = 2");
		cq.setQueryString(sb.toString());
		cq.setInParamMap(new HashMap<>());
		return cq;
	}

	public static CustomQueryHolder nativeQueryToFetchPrevDayCandles(String securityName) {
		CustomQueryHolder cq = new CustomQueryHolder();
		StringBuilder sb = new StringBuilder();
		sb.append("select hc from HistoricalCandle hc where hc.day = 2 order by hc.timestamp asc");
		cq.setQueryString(sb.toString());
		cq.setInParamMap(new HashMap<>());
		return cq;
	}

	public static String nativeQueryToDeletePrevDayCandles() {
		StringBuilder sb = new StringBuilder();
		sb.append("delete from historical_candles where historical_candles.day = 2");
		return sb.toString();
	}

	public static String nativeQueryToDeletePrevDayStrategy() {
		StringBuilder sb = new StringBuilder();
		sb.append("delete from trade_strategy where trade_strategy.day = 2");
		return sb.toString();
	}

	public static CustomQueryHolder queryToFetchSecurityMonitorStrategy() {
		CustomQueryHolder cq = new CustomQueryHolder();
		StringBuilder sb = new StringBuilder();
		sb.append("select ts from TradeStrategy ts where ts.day = 1");
		cq.setQueryString(sb.toString());
		cq.setInParamMap(new HashMap<>());
		return cq;
	}

	public static String nativeQueryToUpdatePrevDayCandles() {
		StringBuilder sb = new StringBuilder();
		sb.append("update historical_candles set historical_candles.day = 2 where historical_candles.day = 1");
		return sb.toString();
	}

	public static String nativeQueryToUpdatePrevDayStrategy() {
		StringBuilder sb = new StringBuilder();
		sb.append("update trade_strategy set trade_strategy.day = 2 where trade_strategy.day = 1");
		return sb.toString();
	}

	public static String nativeQueryToFetchNameTokenMap() {
		StringBuilder sb = new StringBuilder();
		sb.append("select security_name, security_token from instruments ");
		return sb.toString();
	}

	public static CustomQueryHolder queryToFetchDayTrades(String dayStartTime, String dayEndTime) {
		CustomQueryHolder cq = new CustomQueryHolder();
		StringBuilder sb = new StringBuilder();
		sb.append("select trade from Trade trade where trade.tradeDate between " + "'" + dayStartTime + "'" + " AND "
				+ "'" + dayEndTime + "'");
		cq.setQueryString(sb.toString());
		cq.setInParamMap(new HashMap<>());
		return cq;
	}

	public static CustomQueryHolder queryToFetchSecurityAllTradeStrategy() {
		CustomQueryHolder cq = new CustomQueryHolder();
		StringBuilder sb = new StringBuilder();
		sb.append("select ts from TradeStrategy ts");
		cq.setQueryString(sb.toString());
		cq.setInParamMap(new HashMap<>());
		return cq;
	}

}

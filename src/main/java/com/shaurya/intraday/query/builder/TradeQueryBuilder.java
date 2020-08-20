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

	public static CustomQueryHolder queryToFetchSecurityMonitorStrategy() {
		CustomQueryHolder cq = new CustomQueryHolder();
		StringBuilder sb = new StringBuilder();
		sb.append("select ts from TradeStrategy ts where ts.day = 1");
		cq.setQueryString(sb.toString());
		cq.setInParamMap(new HashMap<>());
		return cq;
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

	public static CustomQueryHolder queryToPerformance() {
		CustomQueryHolder cq = new CustomQueryHolder();
		StringBuilder sb = new StringBuilder();
		sb.append("select performance from Performance performance");
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

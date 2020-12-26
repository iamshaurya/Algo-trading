/**
 * 
 */
package com.shaurya.intraday.trade.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import com.shaurya.intraday.enums.OrderStatusType;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.strategy.Strategy;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

/**
 * @author Shaurya
 *
 */
public interface TradeOrderService {
	public StrategyModel placeEntryCoverOrder(StrategyModel model);

	public StrategyModel placeExitCoverOrder(StrategyModel model) throws JSONException, IOException, KiteException;

	public List<Candle> getPrevDayData(Object obj);

	public OrderStatusType getOrderStatus(StrategyModel model) throws JSONException, IOException, KiteException;
	
	public Double getMarginForSecurity(Map<String, Strategy> strategyMap) throws JSONException, IOException, KiteException;

  StrategyModel placeTrailSlOrder(StrategyModel model)
      throws JSONException, IOException, KiteException;

  public String getOrderId(StrategyModel model) throws JSONException, IOException, KiteException;

	public Double getTotalMargin() throws JSONException, IOException, KiteException;

	Integer getQuantityAsPerRisk(Long fund, final Double slPoints, final Integer lotSize,
			final Double riskPerTradePer);
}

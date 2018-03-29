/**
 * 
 */
package com.shaurya.intraday.trade.service;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.shaurya.intraday.enums.IntervalType;
import com.shaurya.intraday.enums.StrategyType;
import com.shaurya.intraday.enums.TradeExitReason;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.StrategyModel;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Tick;

/**
 * @author Shaurya
 *
 */
public interface TradeService {
	public StrategyModel openTrade(StrategyModel model);

	public StrategyModel closeTrade(StrategyModel model, TradeExitReason reason);

	public StrategyModel fetchOpenTradeBySecurity(String security);

	public Map<StrategyModel, StrategyType> getTradeStrategy();

	public List<Candle> getPrevDayCandles(String securityName);

	public void deletePrevDayCandlesAndStrategy();

	public Map<StrategyModel, StrategyType> getMonitorStrategy();

	public void incrementDayForMonitorStocks();

	public Map<Long, String> getNameTokenMap();

	public Map<String, Long> getTokenNameMap();

	public void createHistoricalCandle(Candle candle);

	public void sendPNLStatement();

	public List<Candle> getPrevDayCandles(Long instrumentToken, Date currentDate) throws IOException, KiteException;

	public void testIndicator() throws IOException, KiteException;

	public void simulation(Long security);

	public List<Candle> getPrevDayCandles(Long instrumentToken, IntervalType interval, Date from, Date to,
			int candleCount);

	public void updateStrategyStocks(List<StrategyModel> smList);

	public Map<StrategyModel, StrategyType> getAllTradeStrategy();
}

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
import java.util.TreeSet;

/**
 * @author Shaurya
 *
 */
public interface TradeService {

  public StrategyModel openTrade(StrategyModel model);

  public StrategyModel closeTrade(StrategyModel model, TradeExitReason reason);

  public StrategyModel fetchOpenTradeBySecurity(String security);

  public Map<StrategyModel, StrategyType> getTradeStrategy();

  public Map<StrategyModel, StrategyType> getMonitorStrategy();

  public Map<Long, String> getNameTokenMap();

  public Map<String, Long> getTokenNameMap();

  public void sendPNLStatement() throws IOException, KiteException;

  public List<Candle> getPrevDayCandles(Long instrumentToken, Date currentDate)
      throws IOException, KiteException;

  public void testIndicator() throws IOException, KiteException;

  public void simulation(Long security);

  public List<Candle> getPrevDayCandles(Long instrumentToken, IntervalType interval, Date from,
      Date to,
      int candleCount);

  public void updateStrategyStocks(List<StrategyModel> smList);

  public Map<StrategyModel, StrategyType> getAllTradeStrategy();

  Double checkBalance() throws IOException, KiteException;

  void recordMonitorStock(Candle candle);

  Map<Long, TreeSet<Candle>> getMonitorStockMap();

  void cleanUpMonitorStockMap();

  void updateAllStockToMonitorStock();

  void updateTradeStocks(List<Long> eligibleStocks, Double marginPortion);
}

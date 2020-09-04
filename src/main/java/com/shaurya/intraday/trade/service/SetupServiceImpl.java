/**
 *
 */
package com.shaurya.intraday.trade.service;

import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.util.HelperUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.shaurya.intraday.enums.StrategyType;
import com.shaurya.intraday.model.StrategyModel;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

/**
 * @author Shaurya
 *
 */
@Slf4j
@Service
public class SetupServiceImpl {

  @Autowired
  private LoginService loginService;
  @Autowired
  private TradeService tradeService;
  @Autowired
  private TradeProcessor processor;
  @Autowired
  private LiveTickerService liveTickerService;

  public void startup() throws KiteException, IOException, JSONException {
    KiteConnect sdkClient = loginService.getSdkClient();
    if (sdkClient != null) {
      ArrayList<Long> tradeToken = new ArrayList<Long>();
      ArrayList<Long> monitorToken = new ArrayList<Long>();
      liveTickerService.init(sdkClient);
      Map<StrategyModel, StrategyType> tradeStock = tradeService.getTradeStrategy();
      Map<StrategyModel, StrategyType> monitorStock = tradeService.getMonitorStrategy();
      for (Entry<StrategyModel, StrategyType> e : tradeStock.entrySet()) {
        tradeToken.add(e.getKey().getSecurityToken());
      }
      for (Entry<StrategyModel, StrategyType> e : monitorStock.entrySet()) {
        monitorToken.add(e.getKey().getSecurityToken());
      }
      liveTickerService.subscribeTradeStock(tradeToken);
      liveTickerService.subscribeMonitorStock(monitorToken);
      if (!tradeToken.isEmpty()) {
        processor.initializeStrategyMap();
      }
    }
  }

  public void shutdown() throws IOException, KiteException {
    updateNextDayStocks();
    tradeService.sendPNLStatement();
    liveTickerService.disconnect();
    processor.destroyStrategyMap();
    KiteConnect sdkClient = loginService.getSdkClient();
    if (sdkClient != null) {
      sdkClient.logout();
      loginService.destroySdkClient();
    }
    // tradeService.incrementDayForMonitorStocks();
  }

  private void updateNextDayStocks() {
    tradeService.updateAllStockToMonitorStock();
    Map<Long, TreeSet<Candle>> monitorStocks = tradeService.getMonitorStockMap();
    if(monitorStocks == null){
      return;
    }
    List<Long> eligibleStocks = new ArrayList<>();
    for (Entry<Long, TreeSet<Candle>> e : monitorStocks.entrySet()) {
      Candle dayCandle = HelperUtil.formDayCandle(e.getValue());
      Double cprWidth = HelperUtil.cprRange(dayCandle);
      log.error("cpr width for {}, is {}", dayCandle.getSecurity(), cprWidth);
      if (cprWidth <= 0.10) {
        eligibleStocks.add(dayCandle.getToken());
      }
    }
    if (eligibleStocks.size() > 0) {
      Double marginPortion = Math.min(0.05 / eligibleStocks.size(), 0.01);
      tradeService.updateTradeStocks(eligibleStocks, marginPortion);
    }
    tradeService.cleanUpMonitorStockMap();
  }

  public void startupLogin() {
    loginService.getSdkClient();
  }
}

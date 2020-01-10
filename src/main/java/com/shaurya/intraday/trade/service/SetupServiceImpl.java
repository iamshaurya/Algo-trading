/**
 * 
 */
package com.shaurya.intraday.trade.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

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
		liveTickerService.disconnect();
		processor.destroyStrategyMap();
		KiteConnect sdkClient = loginService.getSdkClient();
		if (sdkClient != null) {
			sdkClient.logout();
			loginService.destroySdkClient();
		}
		tradeService.sendPNLStatement();
		// tradeService.incrementDayForMonitorStocks();
	}

	public void startupLogin() {
		loginService.getSdkClient();
	}
}

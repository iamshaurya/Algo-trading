/**
 * 
 */
package com.shaurya.intraday.trade.service;

import static com.shaurya.intraday.util.HelperUtil.isBetweenTradingWindow;
import static com.shaurya.intraday.util.HelperUtil.isTimeDiff1Min;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.shaurya.intraday.model.LiveTickCandle;
import com.shaurya.intraday.util.HelperUtil;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Tick;
import com.zerodhatech.ticker.KiteTicker;
import com.zerodhatech.ticker.OnConnect;
import com.zerodhatech.ticker.OnDisconnect;
import com.zerodhatech.ticker.OnError;
import com.zerodhatech.ticker.OnTicks;

/**
 * @author Shaurya
 *
 */
@Service
public class LiveTickerServiceImpl implements LiveTickerService {
	private KiteTicker tickerProvider = null;
	private Map<Long, LiveTickCandle> tradeStock;
	private Map<Long, LiveTickCandle> monitorStock;
	private Map<Long, String> nameTokenMap;
	@Autowired
	private TradeProcessor tradeProcessor;
	@Autowired
	private TradeService tradeService;

	@Override
	public void init(KiteConnect kiteConnect) throws KiteException {
		/**
		 * To get live price use websocket connection. It is recommended to use
		 * only one websocket connection at any point of time and make sure you
		 * stop connection, once user goes out of app. custom url points to new
		 * endpoint which can be used till complete Kite Connect 3 migration is
		 * done.
		 */
		if (tickerProvider == null) {
			tickerProvider = new KiteTicker(kiteConnect.getAccessToken(), kiteConnect.getApiKey());
			nameTokenMap = tradeService.getNameTokenMap();
		}

		tickerProvider.setOnConnectedListener(new OnConnect() {
			@Override
			public void onConnected() {
				/**
				 * Subscribe ticks for token. By default, all tokens are
				 * subscribed for modeQuote.
				 */
				System.out.println("Live ticker connected");
				if (tradeStock != null && !tradeStock.isEmpty()) {
					ArrayList<Long> tradeTokens = new ArrayList<>();
					for (Entry<Long, LiveTickCandle> e : tradeStock.entrySet()) {
						tradeTokens.add(e.getKey());
					}
					tradeTokens.add(256265l); // adding nifty 50 token
					tickerProvider.subscribe(tradeTokens);
					tickerProvider.setMode(tradeTokens, KiteTicker.modeLTP);
				}

				if (monitorStock != null && !monitorStock.isEmpty()) {
					ArrayList<Long> monitorTokens = new ArrayList<>();
					for (Entry<Long, LiveTickCandle> e : monitorStock.entrySet()) {
						monitorTokens.add(e.getKey());
					}
					tickerProvider.subscribe(monitorTokens);
					tickerProvider.setMode(monitorTokens, KiteTicker.modeLTP);
				}
			}
		});

		tickerProvider.setOnDisconnectedListener(new OnDisconnect() {
			@Override
			public void onDisconnected() {
				System.out.println("Live ticker disconnected");
			}
		});

		tickerProvider.setOnTickerArrivalListener(new OnTicks() {
			@Override
			public void onTicks(ArrayList<Tick> ticks) {
				for (Tick t : ticks) {
					t.setTickTimestamp(new Date());
					if (isBetweenTradingWindow(t.getTickTimestamp())) {
						handleNifty50Tick(t);
						handleTradeStock(t);
						handleMonitorStock(t);
					}
				}
			}

			private void handleNifty50Tick(Tick t) {
				if(t.getInstrumentToken() == 256265l){
					tradeProcessor.updateNifty50Ltp(t.getLastTradedPrice());
				}
			}

			private void handleMonitorStock(Tick t) {
				if (monitorStock.containsKey(t.getInstrumentToken())) {
					// if (isTimeAfterNoon(t.getTickTimestamp())) {
					if (monitorStock.get(t.getInstrumentToken()).getCandle().getOpen() == 0) {
						initialiseNewLiveCandle(monitorStock, t);
					} else {
						monitorStock.get(t.getInstrumentToken()).update(t.getLastTradedPrice(),
								t.getTickTimestamp());
					}
					if (isTimeDiff1Min(monitorStock.get(t.getInstrumentToken()).getPrevCandleCreationTime(),
							monitorStock.get(t.getInstrumentToken()).getLastTime())) {
						// tradeService
						// .createHistoricalCandle(monitorStock.get(t.getInstrumentToken()).getCandle());
						initialiseBlankLiveCandle(monitorStock, t);
					}
					// }
				}
			}

			private void handleTradeStock(Tick t) {
				if (tradeStock.containsKey(t.getInstrumentToken())) {
					if (tradeStock.get(t.getInstrumentToken()).getCandle().getOpen() == 0) {
						initialiseNewLiveCandle(tradeStock, t);
					} else {
						tradeStock.get(t.getInstrumentToken()).update(t.getLastTradedPrice(),
								t.getTickTimestamp());
					}
					if (isTimeDiff1Min(tradeStock.get(t.getInstrumentToken()).getPrevCandleCreationTime(),
							tradeStock.get(t.getInstrumentToken()).getLastTime())) {
						tradeProcessor.getTradeCall(tradeStock.get(t.getInstrumentToken()).getCandle());
						initialiseBlankLiveCandle(tradeStock, t);
					}
				}
			}

			private void initialiseBlankLiveCandle(Map<Long, LiveTickCandle> stockMap, Tick t) {
				stockMap.put(t.getInstrumentToken(), new LiveTickCandle(0, nameTokenMap.get(t), t.getTickTimestamp()));
			}

			private void initialiseNewLiveCandle(Map<Long, LiveTickCandle> stockMap, Tick t) {
				stockMap.put(t.getInstrumentToken(), new LiveTickCandle(t.getLastTradedPrice(),
						nameTokenMap.get(t.getInstrumentToken()), t.getTickTimestamp()));
			}
		});
		
		tickerProvider.setOnErrorListener(new OnError() {
			@Override
			public void onError(Exception exception) {
				// handle here.
			}

			@Override
			public void onError(KiteException kiteException) {
				// handle here.
			}
		});

		tickerProvider.setTryReconnection(true);
		// maximum retries and should be greater than 0
		tickerProvider.setMaximumRetries(50);
		// set maximum retry interval in seconds
		tickerProvider.setMaximumRetryInterval(30);

		/**
		 * connects to com.zerodhatech.com.zerodhatech.ticker server for getting
		 * live quotes
		 */
		tickerProvider.connect();
	}

	@Override
	public boolean checkConnection() {
		return tickerProvider == null ? false : tickerProvider.isConnectionOpen();
	}

	@Override
	public void unsubscribe(ArrayList<Long> tokens) {
		// Unsubscribe for a token.
		System.out.println("unsubscribing for :: " + tokens.toString());
		tickerProvider.unsubscribe(tokens);
	}

	@Override
	public void subscribe(ArrayList<Long> tokens) {
		System.out.println("subscribing for :: " + tokens.toString());
		tickerProvider.subscribe(tokens);
		tickerProvider.setMode(tokens, KiteTicker.modeLTP);
	}

	@Override
	public void subscribeTradeStock(ArrayList<Long> tokens) {
		System.out.println("subscribing for trade :: " + tokens.toString());
		tradeStock = new HashMap<Long, LiveTickCandle>();
		for (Long t : tokens) {
			tradeStock.put(t, new LiveTickCandle(0, nameTokenMap.get(t), HelperUtil.getDayStartTime().getTime()));
		}
		tokens.add(256265l); // adding nifty 50 token
		tickerProvider.subscribe(tokens);
		tickerProvider.setMode(tokens, KiteTicker.modeLTP);
	}

	@Override
	public void subscribeMonitorStock(ArrayList<Long> tokens) {
		System.out.println("subscribing for monitor :: " + tokens.toString());
		monitorStock = new HashMap<Long, LiveTickCandle>();
		for (Long t : tokens) {
			monitorStock.put(t, new LiveTickCandle(0, nameTokenMap.get(t), HelperUtil.getNoonTime().getTime()));
		}
		tickerProvider.subscribe(tokens);
		tickerProvider.setMode(tokens, KiteTicker.modeLTP);
	}

	@Override
	public void disconnect() {
		if (tickerProvider != null) {
			tickerProvider.disconnect();
			tickerProvider = null;
		}
		tradeStock = null;
		monitorStock = null;
		nameTokenMap = null;
	}

}

/**
 * 
 */
package com.shaurya.intraday.trade.service;

import static com.shaurya.intraday.util.HelperUtil.getTradeQuantity;
import static com.shaurya.intraday.util.HelperUtil.isIntradayClosingTime;
import static com.shaurya.intraday.util.HelperUtil.rollDayOfYearByN;
import static com.shaurya.intraday.util.CandlestickPatternHelper.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.shaurya.intraday.constant.Constants;
import com.shaurya.intraday.enums.IntervalType;
import com.shaurya.intraday.enums.OrderStatusType;
import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.enums.StrategyType;
import com.shaurya.intraday.enums.TradeExitReason;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.MailAccount;
import com.shaurya.intraday.model.StockMovement;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.strategy.EMAMacdAndRSIStrategy;
import com.shaurya.intraday.strategy.EMAandRSIStrategy;
import com.shaurya.intraday.strategy.GannSquare9Strategy;
import com.shaurya.intraday.strategy.HeikinAshiOHLStrategy;
import com.shaurya.intraday.strategy.MacdHistogramStrategy;
import com.shaurya.intraday.strategy.ModifiedMacdAndRSIStrategy;
import com.shaurya.intraday.strategy.OpenHighLowStrategy;
import com.shaurya.intraday.strategy.OpeningRangeBreakoutStrategy;
import com.shaurya.intraday.strategy.Strategy;
import com.shaurya.intraday.strategy.impl.EMAMacdAndRSIStrategyImpl;
import com.shaurya.intraday.strategy.impl.EMAandRSIStrategyImpl;
import com.shaurya.intraday.strategy.impl.GannSquare9StrategyImpl;
import com.shaurya.intraday.strategy.impl.HeikinAshiOHLStrategyImpl;
import com.shaurya.intraday.strategy.impl.MacdHistogramStrategyImpl;
import com.shaurya.intraday.strategy.impl.ModifiedMacdAndRSIStrategyImpl;
import com.shaurya.intraday.strategy.impl.OpenHighLowStrategyImpl;
import com.shaurya.intraday.strategy.impl.OpeningRangeBreakoutStrategyImpl;
import com.shaurya.intraday.util.HelperUtil;
import com.shaurya.intraday.util.MailSender;
import com.shaurya.intraday.util.StringUtil;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

/**
 * @author Shaurya
 *
 */
@Service
public class TradeProcessorImpl implements TradeProcessor {
	private Map<String, Strategy> strategyMap;
	private Map<String, StrategyModel> metadatMap;
	private List<StockMovement> topGainer;
	private List<StockMovement> topLoser;
	private Candle nifty50Candle;
	@Autowired
	private TradeService tradeService;
	@Autowired
	private TradeOrderService tradeOrderService;
	@Autowired
	private MailAccount mailAccount;

	@Override
	public synchronized StrategyModel getTradeCall(Candle candle) {
		System.out.println("candle data " + candle.toString());
		try {
			int numberOdTradesForDay = tradeService.fetchNumberOfTradesForTheDay();
			StrategyModel openTrade = tradeService.fetchOpenTradeBySecurity(candle.getSecurity());
			StrategyModel tradeCall = strategyMap.get(candle.getSecurity()).processTrades(candle, openTrade, true);

			if (isIntradayClosingTime(candle.getTime())) {
				// square off all position
				if (openTrade != null) {
					if (tradeOrderService.getOrderStatus(openTrade) == OrderStatusType.OPEN) {
						tradeOrderService.placeExitCoverOrder(openTrade);
					}
					openTrade.setTradePrice(candle.getClose());
					tradeService.closeTrade(openTrade, TradeExitReason.CLOSING_TIME);
				}
			} else {
				if (tradeCall != null && tradeCall.isExitOrder()) {
					TradeExitReason reason = null;
					if (tradeOrderService.getOrderStatus(tradeCall) == OrderStatusType.OPEN) {
						tradeOrderService.placeExitCoverOrder(tradeCall);
						reason = HelperUtil.takeProfitReached(candle, openTrade) ? TradeExitReason.TAKE_PROFIT_REACHED
								: HelperUtil.stopLossReached(candle, openTrade) ? TradeExitReason.STOP_LOSS_REACHED
										: TradeExitReason.STRATEGY_EXIT_CRITERIA_MET;
					} else {
						reason = TradeExitReason.HARD_STOP_LOSS_HIT;
					}
					tradeService.closeTrade(tradeCall, reason);

					tradeCall = (tradeCall = strategyMap.get(candle.getSecurity()).processTrades(candle, null,
							false)) != null ? tradeCall : null;
				}
				if (tradeCall != null && numberOdTradesForDay < 4) {
					switch (tradeCall.getPosition()) {
					case LONG:
						if (isPreferedPosition(tradeCall) && greenCandle(nifty50Candle) && topGainerStock(tradeCall)) {
							// make call for long cover order
							tradeCall.setQuantity(getTradeQuantity(
									metadatMap.get(tradeCall.getSecurity()).getTradeMargin(), tradeCall.getTradePrice(),
									metadatMap.get(tradeCall.getSecurity()).getMarginMultiplier()));
							tradeCall = tradeOrderService.placeEntryCoverOrder(tradeCall);
							if (tradeCall.getOrderId() == null) { // handling
																	// failure
																	// case
								String orderId = retryToFetchOrderId(tradeCall);
								if (orderId == null) {
									System.out.println("######### Place entry order failed ##############");
									break;
								} else {
									System.out.println(
											"Open trade call passed but response not received, order id fetched later: "
													+ orderId);
									tradeCall.setOrderId(orderId);
								}
							}
							tradeCall = tradeService.openTrade(tradeCall);
							System.out.println("Taking long position :: " + tradeCall.toString());

						}
						break;
					case SHORT:
						if (isPreferedPosition(tradeCall) && redCandle(nifty50Candle) && topLoserStock(tradeCall)) {
							// make call for short cover order
							tradeCall.setQuantity(getTradeQuantity(
									metadatMap.get(tradeCall.getSecurity()).getTradeMargin(), tradeCall.getTradePrice(),
									metadatMap.get(tradeCall.getSecurity()).getMarginMultiplier()));
							tradeCall = tradeOrderService.placeEntryCoverOrder(tradeCall);
							if (tradeCall.getOrderId() == null) { // handling
																	// failure
																	// case
								String orderId = retryToFetchOrderId(tradeCall);
								if (orderId == null) {
									System.out.println("######### Place entry order failed ##############");
									break;
								} else {
									System.out.println(
											"Open trade call passed but response not received, order id fetched later: "
													+ orderId);
									tradeCall.setOrderId(orderId);
								}
							}
							tradeCall = tradeService.openTrade(tradeCall);
							System.out.println("Taking short position :: " + tradeCall.toString());
						}
						break;
					default:
						break;
					}
				}
			}
		} catch (KiteException e) {
			System.out.println("Some exception occured due to : " + e.getCause());
			MailSender
					.sendMail(Constants.TO_MAIL, Constants.TO_NAME,
							Constants.KITE_EXCEPTION_TRADE_PROCESSOR, "TradeProcessorImpl.getTradeCall :: candle : "
									+ candle.toString() + " :: Some exception occured due to : " + e.getCause(),
							mailAccount);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Some exception occured due to : " + e.getCause());
		}

		return null;
	}

	private boolean isPreferedPosition(StrategyModel tradeCall) {
		boolean isPreferedPosition = false;
		switch (metadatMap.get(tradeCall.getSecurity()).getPreferedPosition()) {
		case LONG:
			isPreferedPosition = tradeCall.getPosition() == PositionType.LONG;
			break;
		case SHORT:
			isPreferedPosition = tradeCall.getPosition() == PositionType.SHORT;
			break;
		case BOTH:
			isPreferedPosition = true;
			break;
		default:
			break;
		}
		return isPreferedPosition;
	}

	private String retryToFetchOrderId(StrategyModel model) throws JSONException, IOException, KiteException {
		OrderStatusType status = tradeOrderService.getOrderStatus(model);
		if (status != null && status == OrderStatusType.OPEN) {
			return tradeOrderService.getOrderId(model);
		}
		return null;
	}

	private boolean topGainerStock(StrategyModel tradeCall) {
		StockMovement aux = new StockMovement(tradeCall.getSecurity(), tradeCall.getSecurityToken(), 0, 0);
		return topGainer.contains(aux) && (topGainer.indexOf(aux) < 10);
	}

	private boolean topLoserStock(StrategyModel tradeCall) {
		StockMovement aux = new StockMovement(tradeCall.getSecurity(), tradeCall.getSecurityToken(), 0, 0);
		return topLoser.contains(aux) && (topLoser.indexOf(aux) < 10);
	}

	@Override
	public void initializeStrategyMap() throws IOException, KiteException, JSONException {
		Calendar cal = Calendar.getInstance();
		strategyMap = new HashMap<>();
		metadatMap = new HashMap<>();
		topGainer = new ArrayList<>();
		topLoser = new ArrayList<>();
		Map<StrategyModel, StrategyType> strategyTypeMap = tradeService.getTradeStrategy();
		for (Entry<StrategyModel, StrategyType> e : strategyTypeMap.entrySet()) {
			try {
				List<Candle> cList = null;
				e.getKey().setTradeMargin(e.getKey().getMarginPortion());
				switch (e.getValue()) {
				case EMA_MACD_RSI:
					cList = tradeService.getPrevDayCandles(e.getKey().getSecurityToken(), cal.getTime());
					EMAMacdAndRSIStrategy emaMacdRsi = new EMAMacdAndRSIStrategyImpl();
					emaMacdRsi.initializeSetup(cList);
					strategyMap.put(e.getKey().getSecurity(), emaMacdRsi);
					break;
				case EMA_RSI:
					cList = tradeService.getPrevDayCandles(e.getKey().getSecurityToken(), cal.getTime());
					EMAandRSIStrategy emaRsi = new EMAandRSIStrategyImpl();
					emaRsi.initializeSetup(cList);
					strategyMap.put(e.getKey().getSecurity(), emaRsi);
					break;
				case MACD_RSI:
					cList = tradeService.getPrevDayCandles(e.getKey().getSecurityToken(), cal.getTime());
					ModifiedMacdAndRSIStrategy macdRsi = new ModifiedMacdAndRSIStrategyImpl();
					macdRsi.initializeSetup(cList);
					strategyMap.put(e.getKey().getSecurity(), macdRsi);
					break;
				case MACD_HISTOGRAM:
					cList = tradeService.getPrevDayCandles(e.getKey().getSecurityToken(), IntervalType.MINUTE_5,
							rollDayOfYearByN(cal.getTime(), -4), cal.getTime(), 200);
					MacdHistogramStrategy macdHistogram = new MacdHistogramStrategyImpl();
					macdHistogram.initializeSetup(cList);
					strategyMap.put(e.getKey().getSecurity(), macdHistogram);
					break;
				case OPEN_HIGH_LOW:
					cList = new ArrayList<>();
					OpenHighLowStrategy ohl = new OpenHighLowStrategyImpl();
					ohl.initializeSetup(cList);
					strategyMap.put(e.getKey().getSecurity(), ohl);
					break;
				case HEIKIN_ASHI_OHL:
					cList = tradeService.getPrevDayCandles(e.getKey().getSecurityToken(), IntervalType.MINUTE_15,
							rollDayOfYearByN(cal.getTime(), -6), cal.getTime(), 100);
					HeikinAshiOHLStrategy haOhl = new HeikinAshiOHLStrategyImpl();
					haOhl.initializeSetup(cList);
					strategyMap.put(e.getKey().getSecurity(), haOhl);
					break;
				case OPENING_RANGE_BREAKOUT:
					cList = tradeService.getPrevDayCandles(e.getKey().getSecurityToken(), IntervalType.DAY,
							rollDayOfYearByN(cal.getTime(), -4), cal.getTime(), 2);
					OpeningRangeBreakoutStrategy orb = new OpeningRangeBreakoutStrategyImpl();
					orb.initializeSetup(cList);
					strategyMap.put(e.getKey().getSecurity(), orb);
					double orbLtp = cList.get(cList.size() - 1).getClose();
					if (orbLtp <= 1500) {
						topGainer.add(new StockMovement(e.getKey().getSecurity(), e.getKey().getSecurityToken(), orbLtp,
								orbLtp));
						topLoser.add(new StockMovement(e.getKey().getSecurity(), e.getKey().getSecurityToken(), orbLtp,
								orbLtp));
					}
					break;
				case GANN_SQUARE_9:
					cList = tradeService.getPrevDayCandles(e.getKey().getSecurityToken(), IntervalType.MINUTE_15,
							rollDayOfYearByN(cal.getTime(), -6), cal.getTime(), 100);
					GannSquare9Strategy gann = new GannSquare9StrategyImpl();
					gann.initializeSetup(cList);
					strategyMap.put(e.getKey().getSecurity(), gann);
					double gannLtp = cList.get(cList.size() - 1).getClose();
					if (gannLtp <= 1500) {
						topGainer.add(new StockMovement(e.getKey().getSecurity(), e.getKey().getSecurityToken(),
								gannLtp, gannLtp));
						topLoser.add(new StockMovement(e.getKey().getSecurity(), e.getKey().getSecurityToken(), gannLtp,
								gannLtp));
					}
					break;
				default:
					break;
				}
				metadatMap.put(e.getKey().getSecurity(), e.getKey());

			} catch (Exception ex) {
				System.out.println("Error initializing historical data :: " + StringUtil.getStackTraceInStringFmt(ex));
				MailSender.sendMail(Constants.TO_MAIL, Constants.TO_NAME, Constants.KITE_EXCEPTION_TRADE_PROCESSOR,
						"Error initialiing historical data :: " + ex.getMessage() + "\n for : "
								+ e.getKey().getSecurity(),
						mailAccount);
			}
		}
		// tradeService.deletePrevDayCandlesAndStrategy();
		initializeNifty50Candle();
	}

	private void initializeNifty50Candle() throws IOException, KiteException {
		Calendar cal = Calendar.getInstance();
		List<Candle> niftyClist = tradeService.getPrevDayCandles(256265l, IntervalType.DAY,
				rollDayOfYearByN(cal.getTime(), -1), cal.getTime(), 1);
		Collections.sort(niftyClist);
		Candle auxC = niftyClist.get(niftyClist.size() - 1);
		nifty50Candle = new Candle("Nifty 50", 256265l, new Date(), auxC.getClose(), 0, 0, 0, 0);
		System.out.println("Nifty 50 :: last day closing :: " + nifty50Candle.getOpen());
	}

	@Override
	public synchronized void updateNifty50Ltp(double ltp) {
		if (this.nifty50Candle.getOpen() == 0) {
			this.nifty50Candle.setOpen(ltp);
		} else {
			this.nifty50Candle.setClose(ltp);
		}
	}

	@Override
	public synchronized void updateTopGainerLoser(double token, double ltp) {
		StockMovement aux = new StockMovement("", token, ltp, ltp);
		if (topGainer.contains(aux)) {
			topGainer.get(topGainer.indexOf(aux)).updateLtp(ltp);
			Collections.sort(topGainer);
		}
		if (topLoser.contains(aux)) {
			topLoser.get(topLoser.indexOf(aux)).updateLtp(ltp);
			Collections.sort(topLoser);
			Collections.reverse(topLoser);
		}
	}

	@Override
	public void destroyStrategyMap() {
		if (strategyMap != null) {
			for (Entry<String, Strategy> e : strategyMap.entrySet()) {
				e.getValue().destroySetup();
			}
			strategyMap = null;
		}
		metadatMap = null;
		nifty50Candle = null;
		topGainer = null;
		topLoser = null;
	}

}

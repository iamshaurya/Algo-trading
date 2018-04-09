/**
 * 
 */
package com.shaurya.intraday.trade.backtest.service;

import static com.shaurya.intraday.util.HelperUtil.rollDayOfYearByN;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.shaurya.intraday.constant.Constants;
import com.shaurya.intraday.enums.IntervalType;
import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.enums.StrategyType;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.MailAccount;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.strategy.Strategy;
import com.shaurya.intraday.strategy.impl.EMAandRSIStrategyImpl;
import com.shaurya.intraday.strategy.impl.GannSquare9StrategyImpl;
import com.shaurya.intraday.strategy.impl.HeikinAshiOHLStrategyImpl;
import com.shaurya.intraday.strategy.impl.OpenHighLowStrategyImpl;
import com.shaurya.intraday.strategy.impl.OpeningRangeBreakoutStrategyImpl;
import com.shaurya.intraday.strategy.impl.SuperTrendStrategyImpl;
import com.shaurya.intraday.trade.service.StockScreener;
import com.shaurya.intraday.trade.service.TradeService;
import com.shaurya.intraday.util.MailSender;
import com.shaurya.intraday.util.StringUtil;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

/**
 * @author Shaurya
 *
 */
@Service
public class BackTestService {
	//assumed 1.15% for 2500, 0.85 for 25000
	@Autowired
	private TradeService tradeService;
	@Autowired
	private StockScreener stockScreener;
	@Autowired
	private MailAccount mailAccount;
	private Strategy strategy;
	private Long token;
	private double niftyLastClose;
	private Map<String, List<BacktestResult>> resultMap;
	
	public void start(PositionType position, StrategyType strategyType, int quantity, int duration)
			throws IOException, KiteException, InterruptedException {
		int itrCount = duration/30;
		resultMap = new HashMap<String, List<BacktestResult>>();
		List<String> stocks = stockScreener.fetchTopAnnualVolatileStock();
		for (String s : stocks) {
			try {
				backtest(position, strategyType, s, quantity, duration);
			} catch (Exception e) {
				System.out.println("Some error occured : "+StringUtil.getStackTraceInStringFmt(e)+" for : "+s);
			}
		}
		Calendar header = Calendar.getInstance();
		StringBuilder sb = new StringBuilder("<html><body><table border=\"1\">");
		sb.append("<tr>");
		sb.append("<th>").append("Security").append("</th>");
		while (itrCount > 0) {
			sb.append("<th>").append(header.getDisplayName(Calendar.MONTH, Calendar.SHORT_FORMAT, Locale.ENGLISH))
					.append("</th>");
			itrCount--;
			if (header.get(Calendar.MONTH) == Calendar.DECEMBER) {
				header.roll(Calendar.YEAR, true);
			}
			header.roll(Calendar.MONTH, true);
		}
		sb.append("<th><b>").append("Consolidated").append("<b></th>");
		sb.append("</tr>");
		for (Entry<String, List<BacktestResult>> e : resultMap.entrySet()) {
			int totalSuccess = 0;
			int totalFailure = 0;
			double totalPnl = 0;
			for (BacktestResult res : e.getValue()) {
				totalSuccess += res.getSuccessfull();
				totalFailure += res.getUnsuccessfull();
				totalPnl += res.getPnl();
			}
			double winPer = ((double) totalSuccess / (totalSuccess + totalFailure) * 100);
			double avgPnl = (double) (totalPnl / (totalSuccess + totalFailure));
			sb.append(constructMailBody(e.getKey(), e.getValue(), winPer, avgPnl, totalPnl));
		}
		sb.append("</table></body></html>");
		MailSender.sendMail(Constants.TO_MAIL, Constants.TO_NAME, Constants.BACKTEST, writeToFile(sb.toString(), strategyType), mailAccount);
	}
	
	private String writeToFile(String body, StrategyType strategyType) throws IOException {
		String path = "/Users/helpchat/Desktop/result_" + strategyType.name() + "_" + System.currentTimeMillis()
				+ ".html";
		/*String path = "/home/ec2-user/result_" + strategyType.name() + "_" + System.currentTimeMillis()
		+ ".html";*/
		File file = new File(path);
		FileWriter writer = new FileWriter(file);
		writer.write(body);
		writer.close();
		return path;
	}
	
	
	private String constructMailBody(String security, List<BacktestResult> resList, double winPer, double avgPnl,
			double totalPnl) {
		StringBuilder sb = new StringBuilder("<tr>");
		sb.append("<td>").append(security).append("</td>");
		for (BacktestResult res : resList) {
			sb.append("<td>");
			sb.append(constructMonthlyString(res));
			sb.append("</td>");
		}
		sb.append("<td>").append(
				"Win percentage : " + winPer + "\n" + "Avg pnl : " + avgPnl + "%" + "\n" + "Total pnl : " + totalPnl)
				.append("</td>");
		sb.append("</tr>");
		return sb.toString();
	}
	
	private String constructMonthlyString(BacktestResult res){
		return "Successfull trades: " + res.getSuccessfull() + "\n" + "Unsuccessfull trades: " + res.getUnsuccessfull() + "\n"
				+ "PnL : " + res.getPnl() + "%";
	}

	private void init(Date toDate, String security, StrategyType strategyType) throws IOException, KiteException {
		token = tradeService.getTokenNameMap().get(security);
		Calendar toDateCal = Calendar.getInstance();
		toDateCal.setTime(toDate);
		toDateCal.set(Calendar.HOUR_OF_DAY, 15);
		toDateCal.set(Calendar.MINUTE, 30);
		toDateCal.set(Calendar.SECOND, 0);
		toDateCal.set(Calendar.MILLISECOND, 0);
		toDateCal.add(Calendar.DAY_OF_YEAR, -1);
		Date toDateInit = toDateCal.getTime();
		Calendar fromDateCal = Calendar.getInstance();
		fromDateCal.setTime(toDateInit);
		fromDateCal.set(Calendar.HOUR_OF_DAY, 9);
		fromDateCal.set(Calendar.MINUTE, 15);
		fromDateCal.set(Calendar.SECOND, 0);
		fromDateCal.set(Calendar.MILLISECOND, 0);
		Date fromDate = rollDayOfYearByN(fromDateCal.getTime(), -10);
		List<Candle> niftyClist = null;
		List<Candle> cList = null;
		switch (strategyType) {
		case EMA_RSI:
			niftyClist = new ArrayList<>();
			cList = tradeService.getPrevDayCandles(token, IntervalType.MINUTE_15, fromDate, toDateInit,200);
			strategy = new EMAandRSIStrategyImpl();
			strategy.initializeSetup(cList);
			break;
		case OPEN_HIGH_LOW:
			niftyClist = new ArrayList<>();
			cList = tradeService.getPrevDayCandles(token, IntervalType.MINUTE_5, fromDate, toDateInit, 200);
			strategy = new OpenHighLowStrategyImpl();
			strategy.initializeSetup(cList);
			break;
		case HEIKIN_ASHI_OHL:
			niftyClist = new ArrayList<>();
			cList = tradeService.getPrevDayCandles(token, IntervalType.MINUTE_5, fromDate, toDateInit,90);
			strategy = new HeikinAshiOHLStrategyImpl();
			strategy.initializeSetup(cList);
			break;
		case OPENING_RANGE_BREAKOUT:
			niftyClist = new ArrayList<>();
			cList = tradeService.getPrevDayCandles(token, IntervalType.MINUTE_5, fromDate, toDateInit, 200);
			strategy = new OpeningRangeBreakoutStrategyImpl();
			strategy.initializeSetup(cList);
			break;
		case GANN_SQUARE_9:
			niftyClist = new ArrayList<>();
			cList = tradeService.getPrevDayCandles(token, IntervalType.MINUTE_5, fromDate, toDateInit, 200);
			strategy = new GannSquare9StrategyImpl();
			strategy.initializeSetup(cList);
			break;
		case SUPER_TREND:
			niftyClist = new ArrayList<>();
			cList = tradeService.getPrevDayCandles(token, IntervalType.MINUTE_5, fromDate, toDateInit, 200);
			strategy = new SuperTrendStrategyImpl();
			strategy.initializeSetup(cList);
			break;
		default:
			break;
		}
//		niftyLastClose = niftyClist.get(niftyClist.size() - 1).getClose();
	}

	private void destroy() {
		strategy = null;
		token = null;
	}

	public void backtest(PositionType position, StrategyType strategyType, String security, int quantity, int duration)
			throws IOException, KiteException, InterruptedException {
		List<BacktestResult> monthlyResultList = new LinkedList<>();
		int itrCount = duration/30;
		Calendar fromDateCal = Calendar.getInstance();
		fromDateCal.set(Calendar.HOUR_OF_DAY, 9);
		fromDateCal.set(Calendar.MINUTE, 15);
		fromDateCal.set(Calendar.SECOND, 0);
		fromDateCal.set(Calendar.MILLISECOND, 0);
		fromDateCal.add(Calendar.MONTH, -1 * itrCount);
		fromDateCal.set(Calendar.DAY_OF_MONTH, 1);
		Date fromDate = fromDateCal.getTime();
		init(fromDate, security, strategyType);
		while(itrCount > 0){
			Calendar toDateCal = Calendar.getInstance();
			toDateCal.setTime(fromDate);
			toDateCal.set(Calendar.HOUR_OF_DAY, 15);
			toDateCal.set(Calendar.MINUTE, 30);
			toDateCal.set(Calendar.SECOND, 0);
			toDateCal.set(Calendar.MILLISECOND, 0);
			if(fromDateCal.get(Calendar.MONTH) == Calendar.FEBRUARY){
				toDateCal.set(Calendar.DAY_OF_MONTH, toDateCal.getActualMaximum(Calendar.DAY_OF_MONTH));
			}else{
				toDateCal.add(Calendar.DAY_OF_MONTH, 29);
			}
			Date toDate = toDateCal.getTime(); 
			List<Candle> cList = tradeService.getPrevDayCandles(token, IntervalType.MINUTE_1, fromDate, toDate, 200);
			if(fromDateCal.get(Calendar.MONTH) == Calendar.DECEMBER){
				fromDateCal.roll(Calendar.YEAR, true);
			}
			fromDateCal.roll(Calendar.MONTH, true);
			fromDate = fromDateCal.getTime();
			itrCount--;
			
			StrategyModel openTrade = null;
			double initialBalance = 100000;
			int successfullTrade = 0;
			int unsuccessfullTrade = 0;
			double initailPrice = cList.size() > 0 ?cList.get(0).getOpen(): -1;
			double monthlyTurnover = 0;
			double pnl = 0;
			for (int i = 0; i < cList.size(); i++) {
				StrategyModel tradeCall = strategy.processTrades(cList.get(i), openTrade, true);
				if (isIntradayClosingTime(cList.get(i).getTime())) {
					if (openTrade != null) {
						System.out.println("Exit after 3 pm : closing price " + cList.get(i).getClose());
						if (openTrade.getPosition() == PositionType.LONG) {
							pnl += cList.get(i).getClose() - openTrade.getTradePrice();
							if (cList.get(i).getClose() - openTrade.getTradePrice() > 0) {
								successfullTrade++;
							} else {
								unsuccessfullTrade++;
							}
						} else {
							pnl += openTrade.getTradePrice() - cList.get(i).getClose();
							if (openTrade.getTradePrice() - cList.get(i).getClose() > 0) {
								successfullTrade++;
							} else {
								unsuccessfullTrade++;
							}
						}
						monthlyTurnover += openTrade.getQuantity()*cList.get(i).getClose();
						openTrade = null;
					}
					//niftyLastClose = niftyClist.get(i).getClose();
					strategy.destroySetup();
				} else if (tradeCall != null) {
					if (tradeCall.isExitOrder()) {
						System.out.println("Exit : " + tradeCall.toString());
						if (openTrade.getPosition() == PositionType.LONG) {
							pnl += tradeCall.getTradePrice() - openTrade.getTradePrice();
							if (tradeCall.getTradePrice() - openTrade.getTradePrice() > 0) {
								successfullTrade++;
							} else {
								unsuccessfullTrade++;
							}
						} else {
							pnl += openTrade.getTradePrice() - tradeCall.getTradePrice();
							if (openTrade.getTradePrice() - tradeCall.getTradePrice() > 0) {
								successfullTrade++;
							} else {
								unsuccessfullTrade++;
							}
						}
						monthlyTurnover += openTrade.getQuantity()*tradeCall.getTradePrice();
						openTrade = null;
					} else {
						openTrade = tradeCall;
						openTrade.setQuantity((int) (initialBalance/openTrade.getTradePrice()));
						monthlyTurnover += openTrade.getQuantity()*openTrade.getTradePrice();
						switch (tradeCall.getPosition()) {
						case LONG:
							System.out.println("Long Entry at : "+cList.get(i).getTime()+" values : " + tradeCall.toString());
							break;
						case SHORT:
							System.out.println("Short Entry : "+cList.get(i).getTime()+" values : " + tradeCall.toString());
							break;
						default:
							break;
						}
					}
				}
			}
			//System.out.println("Successfull trades: " + successfullTrade);
			//System.out.println("Unsuccessfull trades: " + unsuccessfullTrade);
			double pnlPer = (pnl / initailPrice) * 100;
			//System.out.println("PnL : " + pnlPer + "%");
			double brokerageCharge = brokerageCharge(monthlyTurnover);
			pnlPer = pnlPer - ((double)(brokerageCharge/initialBalance)*100);
			monthlyResultList.add(new BacktestResult(successfullTrade, unsuccessfullTrade, pnlPer));
		}
		resultMap.put(security, monthlyResultList);
		destroy();
	}
	
	private double brokerageCharge(double turnover) {
		double brokerage = Math.min((turnover * 0.0001), 20);
		double stt = 0.00025 * ((double)turnover/2);
		double transactionCharge = (0.0000325 * turnover);
		double gst = 0.18 * (transactionCharge + brokerage);
		double sebiCharge = (0.0000015 * turnover);
		double stampCharge = (0.00003 * turnover);

		return brokerage + stt + transactionCharge + gst + sebiCharge + stampCharge;
	}

	private boolean isIntradayClosingTime(Date time) {
		Calendar closeTime = Calendar.getInstance();
		closeTime.setTime(time);
		closeTime.set(Calendar.HOUR_OF_DAY, 15);
		closeTime.set(Calendar.MINUTE, 18);
		Calendar currTime = Calendar.getInstance();
		currTime.setTime(time);

		return currTime.getTimeInMillis() >= closeTime.getTimeInMillis();
	}
	
	private boolean isBefore1130Time(Date time) {
		Calendar closeTime = Calendar.getInstance();
		closeTime.setTime(time);
		closeTime.set(Calendar.HOUR_OF_DAY, 11);
		closeTime.set(Calendar.MINUTE, 30);
		Calendar currTime = Calendar.getInstance();
		currTime.setTime(time);

		return currTime.getTimeInMillis() >= closeTime.getTimeInMillis();
	}
}

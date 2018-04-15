/**
 * 
 */
package com.shaurya.intraday.trade.service;

import static com.shaurya.intraday.util.HelperUtil.getNthLastKeyEntry;
import static com.shaurya.intraday.util.HelperUtil.parseCsvFileIntoStockList;
import static com.shaurya.intraday.util.HelperUtil.parseCsvFileIntoVolatileStockList;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.shaurya.intraday.builder.TradeBuilder;
import com.shaurya.intraday.constant.Constants;
import com.shaurya.intraday.entity.DailyVolatility;
import com.shaurya.intraday.entity.NseStock;
import com.shaurya.intraday.entity.VolatileStock;
import com.shaurya.intraday.enums.IntervalType;
import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.enums.StrategyType;
import com.shaurya.intraday.indicator.ADX;
import com.shaurya.intraday.indicator.ATR;
import com.shaurya.intraday.indicator.EMA;
import com.shaurya.intraday.indicator.MACD;
import com.shaurya.intraday.indicator.RSI;
import com.shaurya.intraday.model.ADXModel;
import com.shaurya.intraday.model.ATRModel;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.EquityMargin;
import com.shaurya.intraday.model.IndicatorValue;
import com.shaurya.intraday.model.MACDModel;
import com.shaurya.intraday.model.MailAccount;
import com.shaurya.intraday.model.RSIModel;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.query.builder.StockScreenerQueryBuilder;
import com.shaurya.intraday.repo.JpaRepo;
import com.shaurya.intraday.util.MailSender;
import com.shaurya.intraday.util.WebHelperUtil;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

/**
 * @author Shaurya
 *
 */
@Service
public class StockScreenerImpl implements StockScreener {
	@Value("${nse.stock.list.url}")
	private String stockListUrl;
	@Value("${nse.daily.volatilty.report.url}")
	private String dailyVolatilityUrl;
	@Value("${margin.multiplier.url}")
	private String marginMultiplierUrl;
	@Autowired
	private JpaRepo<NseStock> nsRepo;
	@Autowired
	private JpaRepo<DailyVolatility> dvRepo;
	@Autowired
	private JpaRepo<VolatileStock> vsRepo;
	@Autowired
	private TradeService tradeService;
	@Autowired
	private MailAccount mailAccount;

	@Override
	public List<String> fetchTopVolatileStock() {
		cleanUp();
		List<String> finalStockList = new ArrayList<>();
		String dvUrl = new String(dailyVolatilityUrl);
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
		dvUrl = dvUrl.replaceFirst("DATE", sdf.format(new Date()));
		List<NseStock> stocks = null;
		List<DailyVolatility> volatileStocks = null;
		File stockFile;
		File dailyVolatilityFile = null;
		try {
			stockFile = WebHelperUtil.downloadCSV(stockListUrl, new ArrayList<>());
			dailyVolatilityFile = WebHelperUtil.downloadCSV(dvUrl, new ArrayList<>());
			if (stockFile != null && dailyVolatilityFile != null) {
				stocks = parseCsvFileIntoStockList(stockFile);
				volatileStocks = parseCsvFileIntoVolatileStockList(dailyVolatilityFile);
				for (NseStock ns : stocks) {
					nsRepo.update(ns);
				}
				for (DailyVolatility dv : volatileStocks) {
					dvRepo.update(dv);
				}
			}
		} catch (ParseException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		List<Object[]> objList = nsRepo.runNativeQuery(StockScreenerQueryBuilder.queryToFetchMostVolatileStocks());
		for (Object[] o : objList) {
			finalStockList.add((String) o[0]);
		}
		return finalStockList;
	}
	
	@Override
	public List<String> fetchTopAnnualVolatileStock() {
		cleanUp();
		List<String> finalStockList = new ArrayList<>();
		String dvUrl = new String(dailyVolatilityUrl);
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
		dvUrl = dvUrl.replaceFirst("DATE", sdf.format(new Date()));
		List<NseStock> stocks = null;
		//List<DailyVolatility> volatileStocks = null;
		File stockFile;
		//File dailyVolatilityFile = null;
		try {
			stockFile = WebHelperUtil.downloadCSV(stockListUrl, new ArrayList<>());
			//dailyVolatilityFile = WebHelperUtil.downloadCSV(dvUrl, new ArrayList<>());
			if (stockFile != null/* && dailyVolatilityFile != null*/) {
				stocks = parseCsvFileIntoStockList(stockFile);
				//volatileStocks = parseCsvFileIntoVolatileStockList(dailyVolatilityFile);
				for (NseStock ns : stocks) {
					nsRepo.update(ns);
				}
				/*for (DailyVolatility dv : volatileStocks) {
					dvRepo.update(dv);
				}*/
			}
		} catch (ParseException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		List<Object[]> objList = nsRepo.runNativeQuery(StockScreenerQueryBuilder.queryToFetchNse100Stocks());
		for (Object[] o : objList) {
			finalStockList.add((String) o[0]);
		}
		return finalStockList;
	}
	
	@Override
	public List<String> fetchTopAnnualVolatileStockForBacktest() {
		cleanUp();
		List<String> finalStockList = new ArrayList<>();
		String dvUrl = new String(dailyVolatilityUrl);
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
		dvUrl = dvUrl.replaceFirst("DATE", sdf.format(new Date()));
		List<NseStock> stocks = null;
		List<DailyVolatility> volatileStocks = null;
		File stockFile;
		File dailyVolatilityFile = null;
		try {
			stockFile = WebHelperUtil.downloadCSV(stockListUrl, new ArrayList<>());
			dailyVolatilityFile = WebHelperUtil.downloadCSV(dvUrl, new ArrayList<>());
			if (stockFile != null && dailyVolatilityFile != null) {
				stocks = parseCsvFileIntoStockList(stockFile);
				volatileStocks = parseCsvFileIntoVolatileStockList(dailyVolatilityFile);
				for (NseStock ns : stocks) {
					nsRepo.update(ns);
				}
				for (DailyVolatility dv : volatileStocks) {
					dvRepo.update(dv);
				}
			}
		} catch (ParseException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		List<Object[]> objList = nsRepo.runNativeQuery(StockScreenerQueryBuilder.queryToFetchMostAnnualVolatileStocks());
		for (Object[] o : objList) {
			finalStockList.add((String) o[0]);
		}
		return finalStockList;
	}

	private void cleanUp() {
		nsRepo.runNativeQueryForUpdate(StockScreenerQueryBuilder.queryToFlushStockList());
		nsRepo.runNativeQueryForUpdate(StockScreenerQueryBuilder.queryToFlushVolatileStocks());
		nsRepo.runNativeQueryForUpdate(StockScreenerQueryBuilder.queryToFlushTradeStrategy());
	}

	@Override
	public void updateStrategyStocks() {
		List<StrategyModel> stocksToUpdate = new ArrayList<>();
		Map<StrategyType, List<StrategyModel>> sStocks = getFilteredStocks();
		Map<StrategyModel, StrategyType> existingStrategyeStock = tradeService.getAllTradeStrategy();
		for (Entry<StrategyType, List<StrategyModel>> e : sStocks.entrySet()) {
			for (StrategyModel sm : e.getValue()) {
				if (!existingStrategyeStock.containsKey(sm)) {
					stocksToUpdate.add(sm);
				}
			}
		}

		tradeService.updateStrategyStocks(stocksToUpdate);
		List<VolatileStock> unprocessedVsList = vsRepo
				.fetchByQuery(StockScreenerQueryBuilder.queryToFetchUnprocessedVolatileStock());
		if (unprocessedVsList == null || unprocessedVsList.isEmpty()) {
			vsRepo.runNativeQueryForUpdate(StockScreenerQueryBuilder.cleanVolatileStocks());
		}
		MailSender.sendMail(Constants.TO_MAIL, Constants.TO_NAME, Constants.STOCK_SCREENER,
				"Trade stretegy stocks updated :: added these :" + stocksToUpdate.toString(), mailAccount);
	}

	@Override
	public Map<StrategyType, List<StrategyModel>> getFilteredStocks() {
		Map<StrategyType, List<StrategyModel>> strategyMap = new HashMap<>();
		Calendar fromCal = Calendar.getInstance();
		fromCal.roll(Calendar.YEAR, false);
		Calendar toCal = Calendar.getInstance();
		Map<String, Long> tokenNameMap = tradeService.getTokenNameMap();
		Map<String, EquityMargin> margins = null;
		try {
			margins = convertToMap(WebHelperUtil.executeGetRequest(marginMultiplierUrl, new ArrayList<NameValuePair>(),
					EquityMargin.class));
		} catch (ParseException | IOException e1) {
			// e1.printStackTrace();
			System.out.println("error fetching margin data");
			MailSender.sendMail(Constants.TO_MAIL, Constants.TO_NAME, Constants.STOCK_SCREENER,
					"error fetching margin data ", mailAccount);
		}
		List<VolatileStock> stocks = vsRepo.fetchByQuery(StockScreenerQueryBuilder.queryToFetchVolatileStock());
		if (stocks == null || stocks.isEmpty()) {
			stocks = downloadVolatileStock();
		}

		for (VolatileStock vs : stocks) {
			if (vs.getState() == (byte) 0) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(margins.get(vs.getSymbol()) != null){
					List<Candle> cList = tradeService.getPrevDayCandles(tokenNameMap.get(vs.getSymbol()), IntervalType.DAY,
							fromCal.getTime(), toCal.getTime(), 20);
					if (strategyMap.get(StrategyType.OPENING_RANGE_BREAKOUT) == null) {
						strategyMap.put(StrategyType.OPENING_RANGE_BREAKOUT, new ArrayList<>());
					}
					StrategyModel sm  = new StrategyModel();
					sm.setSecurity(vs.getSymbol());
					sm.setMarginMultiplier(calculateEquityMargin(margins.get(vs.getSymbol()), cList.get(cList.size() - 1),
							0.005 * cList.get(cList.size() - 1).getClose(), PositionType.LONG));
					sm.setPreferedPosition(PositionType.BOTH);
					sm.setSecurityToken(tokenNameMap.get(vs.getSymbol()));
					if (sm != null) {
						strategyMap.get(StrategyType.OPENING_RANGE_BREAKOUT).add(sm);
					}
					vs.setState((byte) 1);
					vsRepo.update(vs);
				}
			}
		}

		return strategyMap;
	}

	private List<VolatileStock> downloadVolatileStock() {
		List<VolatileStock> vsEntityList = new ArrayList<>();
		List<String> vsList = fetchTopAnnualVolatileStock();
		for (String s : vsList) {
			VolatileStock vs = TradeBuilder.convertToVolatileStock(s);
			vs = vsRepo.update(vs);
			vsEntityList.add(vs);
		}
		return vsEntityList;
	}

	private Map<String, EquityMargin> convertToMap(List<EquityMargin> margins) {
		Map<String, EquityMargin> map = new HashMap<>();
		for (EquityMargin m : margins) {
			map.put(m.getSymbol(), m);
		}
		return map;
	}

	private StrategyModel getDailyChartStrategy(List<Candle> cList, Map<String, EquityMargin> margins,
			Map<String, Long> tokenNameMap) {
		StrategyModel sm = null;
		Candle lastCandle = cList.get(cList.size() - 1);
		try {
			TreeMap<Date, IndicatorValue> ema9 = EMA.calculateEMA(9, cList);
			TreeMap<Date, IndicatorValue> ema21 = EMA.calculateEMA(21, cList);
			TreeMap<Date, IndicatorValue> ema12 = EMA.calculateEMA(12, cList);
			TreeMap<Date, IndicatorValue> ema26 = EMA.calculateEMA(26, cList);
			MACDModel macd = MACD.calculateMACD(ema12, ema26, 12);
			RSIModel rsi = RSI.calculateRSI(cList);
			ATRModel atr = ATR.calculateATR(cList, 14);
			double rsiValue = rsi.getRsiMap().lastEntry().getValue().getIndicatorValue();
			double atrValue = atr.getAtrMap().lastEntry().getValue().getIndicatorValue();
			// uptrend
			if ((ema9.lastEntry().getValue().getIndicatorValue() > ema21.lastEntry().getValue().getIndicatorValue())
					&& (macd.getMacdMap().lastEntry().getValue().getIndicatorValue() > macd.getSignalMap().lastEntry()
							.getValue().getIndicatorValue())
					&& rsiValue < 80 && ((double) atrValue / lastCandle.getClose() > 0.004)) {
					sm = new StrategyModel();
					sm.setSecurity(lastCandle.getSecurity());
					sm.setMarginMultiplier(calculateEquityMargin(margins.get(lastCandle.getSecurity()), lastCandle,
							atrValue * 0.2, PositionType.LONG));
					sm.setPreferedPosition(PositionType.BOTH);
					sm.setSecurityToken(tokenNameMap.get(lastCandle.getSecurity()));

			}
			// downtrend
			if ((ema9.lastEntry().getValue().getIndicatorValue() < ema21.lastEntry().getValue().getIndicatorValue())
					&& (macd.getMacdMap().lastEntry().getValue().getIndicatorValue() < macd.getSignalMap().lastEntry()
							.getValue().getIndicatorValue())
					&& rsiValue > 25 && ((double) atrValue / lastCandle.getClose() > 0.004)) {
					sm = new StrategyModel();
					sm.setSecurity(lastCandle.getSecurity());
					sm.setMarginMultiplier(calculateEquityMargin(margins.get(lastCandle.getSecurity()), lastCandle,
							atrValue, PositionType.SHORT));
					sm.setPreferedPosition(PositionType.BOTH);
					sm.setSecurityToken(tokenNameMap.get(lastCandle.getSecurity()));
			}
		} catch (Exception e) {
			System.out.println("getDailyChartStrategy :: failed for :: " + lastCandle.getSecurity() + " reason :: "
					+ e.getMessage());
		}

		return sm;
	}

	private double calculateEquityMargin(EquityMargin margin, Candle candle, double atr, PositionType postion) {
		double coLower = margin.getCoLower() / 100;
		double coUpper = margin.getCoUpper() / 100;
		double trigger = candle.getClose() - (coUpper * candle.getClose());

		double stoploss;
		if (postion == PositionType.LONG) {
			stoploss = candle.getClose() - atr;
		} else {
			stoploss = candle.getClose() + atr;
		}

		if (stoploss < trigger) {
			stoploss = trigger;
		} else {
			trigger = stoploss;
		}

		double x;

		if (postion == PositionType.LONG) {
			x = (candle.getClose() - trigger) * 500;
		} else {
			x = (trigger - candle.getClose()) * 500;
		}

		double y = coLower * candle.getClose() * 500;

		double m = Math.max(x, y);
		m = m + (m * 0.2);
		return (500 * candle.getClose()) / m;
	}

	private boolean bullishMacdCorrection(MACDModel macd) {
		TreeMap<Date, IndicatorValue> macdMap = macd.getMacdMap();
		TreeMap<Date, IndicatorValue> signalMap = macd.getSignalMap();
		return macdMap.lastEntry().getValue().getIndicatorValue() <= signalMap.lastEntry().getValue()
				.getIndicatorValue();
	}

	private boolean bearishMacdCorrection(MACDModel macd) {
		TreeMap<Date, IndicatorValue> macdMap = macd.getMacdMap();
		TreeMap<Date, IndicatorValue> signalMap = macd.getSignalMap();
		return macdMap.lastEntry().getValue().getIndicatorValue() >= signalMap.lastEntry().getValue()
				.getIndicatorValue();
	}

	private boolean bullishMacdCrossover(MACDModel macd) {
		Date prevTime = getNthLastKeyEntry(macd.getMacdMap(), 2);
		Date currentTime = getNthLastKeyEntry(macd.getMacdMap(), 1);
		Map<Date, IndicatorValue> macdMap = macd.getMacdMap();
		Map<Date, IndicatorValue> signalMap = macd.getSignalMap();
		return macdMap.get(prevTime).getIndicatorValue() <= signalMap.get(prevTime).getIndicatorValue()
				&& macdMap.get(currentTime).getIndicatorValue() > signalMap.get(currentTime).getIndicatorValue();
	}

	private boolean bearishMacdCrossover(MACDModel macd) {
		Date prevTime = getNthLastKeyEntry(macd.getMacdMap(), 2);
		Date currentTime = getNthLastKeyEntry(macd.getMacdMap(), 1);
		Map<Date, IndicatorValue> macdMap = macd.getMacdMap();
		Map<Date, IndicatorValue> signalMap = macd.getSignalMap();
		return macdMap.get(prevTime).getIndicatorValue() >= signalMap.get(prevTime).getIndicatorValue()
				&& macdMap.get(currentTime).getIndicatorValue() < signalMap.get(currentTime).getIndicatorValue();
	}
}

/**
 * 
 */
package com.shaurya.intraday.strategy.impl;

import static com.shaurya.intraday.util.CandlestickPatternHelper.greenCandle;
import static com.shaurya.intraday.util.CandlestickPatternHelper.redCandle;
import static com.shaurya.intraday.util.HelperUtil.getNthLastKeyEntry;
import static com.shaurya.intraday.util.HelperUtil.stopLossReached;
import static com.shaurya.intraday.util.HelperUtil.takeProfitReached;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import com.shaurya.intraday.enums.IndicatorType;
import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.indicator.ADX;
import com.shaurya.intraday.indicator.ATR;
import com.shaurya.intraday.indicator.EMA;
import com.shaurya.intraday.indicator.GannSquare9;
import com.shaurya.intraday.indicator.MACD;
import com.shaurya.intraday.indicator.RSI;
import com.shaurya.intraday.model.ADXModel;
import com.shaurya.intraday.model.ATRModel;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.HeikinAshiCandle;
import com.shaurya.intraday.model.IndicatorValue;
import com.shaurya.intraday.model.Level;
import com.shaurya.intraday.model.MACDModel;
import com.shaurya.intraday.model.RSIModel;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.strategy.HeikinAshiOHLStrategy;
import com.shaurya.intraday.util.HeikinAshiBuilder;

/**
 * @author Shaurya
 *
 */
public class HeikinAshiOHLStrategyImpl implements HeikinAshiOHLStrategy {
	// Modified macd of 12,26,9
	private ATRModel atr;
	private RSIModel rsi;
	private MACDModel macd;
	private ADXModel adx;
	private TreeMap<Date, IndicatorValue> fastEmaMap;
	private TreeMap<Date, IndicatorValue> slowEmaMap;
	private TreeMap<Date, IndicatorValue> ema5Map;
	private TreeMap<Date, IndicatorValue> ema8Map;
	private TreeMap<Date, IndicatorValue> ema13Map;
	private TreeSet<Candle> candleSet;
	private HeikinAshiCandle prevCandle;
	private List<Level> levels;
	private boolean dayTradeDone;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.shaurya.intraday.strategy.Strategy#processTrades(java.util.List)
	 */

	/*
	 * case 1) if no open trade, then proceed sub case 1) if prev macd < prev
	 * signal && current macd > current signal && RSI < 75 && RSI > 40 then take
	 * a long position with BO sub case 2) if prev macd > prev signal && current
	 * macd < current signal && RSI > 25 && RSI <60 then take a short position
	 * with BO case 2) if a trade is open, then proceed sub case 1) long trade
	 * is open && (Take profit is reached {fetch order detail and update in db
	 * and close the order} || (prev macd > prev signal && current macd <
	 * current signal) || RSI > 75) then close trade sub case 2) short trade is
	 * open && (Take profit is reached {fetch order detail and update in db and
	 * close the order} || (prev macd < prev signal && current macd > current
	 * signal) || RSI < 25) then close trade
	 */
	@Override
	public StrategyModel processTrades(Candle candle, StrategyModel openTrade, boolean updateSetup) {
		if (updateSetup) {
			candleSet.add(candle);
			HeikinAshiCandle candle15min = form15MinCandle();
			if (candle15min != null) {
				updateSetup(candle15min.getHaCandle());
				if (levels == null || levels.isEmpty()) {
					levels = GannSquare9.getLevels(candle15min.getCandle().getClose());
				}
				return getTradeCall(candle15min, openTrade);
			}
		}
		return null;
	}

	private HeikinAshiCandle form15MinCandle() {
		HeikinAshiCandle haCandle = null;
		Candle candle15min = null;
		if (candleSet.size() == 15) {
			int i = 0;
			Iterator<Candle> cItr = candleSet.iterator();
			while (cItr.hasNext()) {
				Candle c = cItr.next();
				if (i == 0) {
					candle15min = new Candle(c.getSecurity(), c.getToken(), c.getTime(), c.getOpen(), c.getHigh(),
							c.getLow(), c.getClose(), 0);
				} else {
					candle15min.setClose(c.getClose());
					candle15min.setHigh(Math.max(candle15min.getHigh(), c.getHigh()));
					candle15min.setLow(Math.min(candle15min.getLow(), c.getLow()));
				}
				i++;
			}
			candleSet.clear();
			haCandle = HeikinAshiBuilder.convert(candle15min, prevCandle);
		}
		return haCandle;
	}

	private StrategyModel getTradeCall(HeikinAshiCandle haCandle, StrategyModel openTrade) {
		Candle candle = haCandle.getHaCandle();
		StrategyModel tradeCall = null;
		Date currentTime = candle.getTime();
		double rsiValue = rsi.getRsiMap().get(currentTime).getIndicatorValue();
		if (openTrade == null) {
			if (bullishMarubozu(candle) && maUptrend(candle) && rsiValue < 70) {
				dayTradeDone = true;
				tradeCall = new StrategyModel(candle.getToken(), PositionType.LONG,
						(0.0015 * haCandle.getCandle().getClose()), haCandle.getCandle().getClose(),
						candle.getSecurity(), null, 0, false);
			}
			if (bearishMarubozu(candle) && maDowntrend(candle) && rsiValue > 30) {
				dayTradeDone = true;
				tradeCall = new StrategyModel(candle.getToken(), PositionType.SHORT,
						(0.0015 * haCandle.getCandle().getClose()), haCandle.getCandle().getClose(),
						candle.getSecurity(), null, 0, false);
			}
		} else {
			// always check for stop loss hit before exiting trade and update
			// reason in db
			if (takeProfitReached(haCandle.getCandle(), openTrade)) {
				tradeCall = new StrategyModel(candle.getToken(), openTrade.getPosition(), openTrade.getSl(),
						candle.getClose(), openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(),
						true);
			}
			if (stopLossReached(haCandle.getCandle(), openTrade)) {
				tradeCall = new StrategyModel(candle.getToken(), openTrade.getPosition(), openTrade.getSl(),
						candle.getClose(), openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(),
						true);
			}
			if (openTrade.getPosition() == PositionType.LONG && (!maUptrend(candle) || redCandle(candle))) {
				tradeCall = new StrategyModel(candle.getToken(), openTrade.getPosition(), openTrade.getSl(),
						candle.getClose(), openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(),
						true);
			}
			if (openTrade.getPosition() == PositionType.SHORT && (!maDowntrend(candle) || greenCandle(candle))) {
				tradeCall = new StrategyModel(candle.getToken(), openTrade.getPosition(), openTrade.getSl(),
						candle.getClose(), openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(),
						true);
			}
		}
		prevCandle = haCandle;
		return tradeCall;

	}

	private boolean bullishBreakout(Candle candle) {
		boolean breakout = false;
		Level currPriceLevel = new Level(candle.getClose(), false);
		List<Level> auxList = new ArrayList<>(levels);
		auxList.add(currPriceLevel);
		Collections.sort(auxList);
		int index = auxList.indexOf(currPriceLevel);
		if (index > 0 && index < (auxList.size() - 1)) {
			double supportVal = auxList.get(index - 1).getValue();
			double resistanceVal = auxList.get(index + 1).getValue();
			breakout = (resistanceVal - candle.getClose()) > (candle.getClose() - supportVal);
		}
		return breakout;
	}

	private boolean bearishBreakout(Candle candle) {
		boolean breakout = false;
		Level currPriceLevel = new Level(candle.getClose(), false);
		List<Level> auxList = new ArrayList<>(levels);
		auxList.add(currPriceLevel);
		Collections.sort(auxList);
		int index = auxList.indexOf(currPriceLevel);
		if (index > 0 && index < (auxList.size() - 1)) {
			double supportVal = auxList.get(index - 1).getValue();
			double resistanceVal = auxList.get(index + 1).getValue();
			breakout = (candle.getClose() - supportVal) > (resistanceVal - candle.getClose());
		}
		return breakout;
	}

	private boolean maUptrendExit(Candle candle) {
		Date currentTime = getNthLastKeyEntry(ema8Map, 1);
		return (candle.getClose() < ema8Map.get(currentTime).getIndicatorValue())
				|| (ema5Map.get(currentTime).getIndicatorValue() < ema8Map.get(currentTime).getIndicatorValue());
	}

	private boolean maDowntrendExit(Candle candle) {
		Date currentTime = getNthLastKeyEntry(ema8Map, 1);
		return (candle.getClose() > ema8Map.get(currentTime).getIndicatorValue())
				|| (ema5Map.get(currentTime).getIndicatorValue() > ema8Map.get(currentTime).getIndicatorValue());
	}

	private boolean maUptrend(Candle candle) {
		Date currentTime = getNthLastKeyEntry(ema13Map, 1);
		return (ema5Map.get(currentTime).getIndicatorValue() > ema8Map.get(currentTime).getIndicatorValue())
				&& (ema8Map.get(currentTime).getIndicatorValue() > ema13Map.get(currentTime).getIndicatorValue());
	}

	private boolean maDowntrend(Candle candle) {
		Date currentTime = getNthLastKeyEntry(ema13Map, 1);
		return (ema5Map.get(currentTime).getIndicatorValue() < ema8Map.get(currentTime).getIndicatorValue())
				&& (ema8Map.get(currentTime).getIndicatorValue() < ema13Map.get(currentTime).getIndicatorValue());
	}

	private boolean entryBearishMacd() {
		Date currentTime = getNthLastKeyEntry(macd.getMacdMap(), 1);
		Map<Date, IndicatorValue> macdMap = macd.getMacdMap();
		Map<Date, IndicatorValue> signalMap = macd.getSignalMap();
		return (macdMap.get(currentTime).getIndicatorValue() < signalMap.get(currentTime).getIndicatorValue());
	}

	private boolean exitBearishMacd() {
		Date prevTime = getNthLastKeyEntry(macd.getMacdMap(), 2);
		Date currentTime = getNthLastKeyEntry(macd.getMacdMap(), 1);
		Map<Date, IndicatorValue> macdMap = macd.getMacdMap();
		Map<Date, IndicatorValue> signalMap = macd.getSignalMap();
		double prevHistogramValue = macdMap.get(prevTime).getIndicatorValue()
				- signalMap.get(prevTime).getIndicatorValue();
		double currentHistogramValue = macdMap.get(currentTime).getIndicatorValue()
				- signalMap.get(currentTime).getIndicatorValue();
		return (prevHistogramValue > currentHistogramValue);
	}

	private boolean entryBullishMacd() {
		Date currentTime = getNthLastKeyEntry(macd.getMacdMap(), 1);
		Map<Date, IndicatorValue> macdMap = macd.getMacdMap();
		Map<Date, IndicatorValue> signalMap = macd.getSignalMap();
		return (macdMap.get(currentTime).getIndicatorValue() > signalMap.get(currentTime).getIndicatorValue());
	}

	private boolean exitBullishMacd() {
		Date prevTime = getNthLastKeyEntry(macd.getMacdMap(), 2);
		Date currentTime = getNthLastKeyEntry(macd.getMacdMap(), 1);
		Map<Date, IndicatorValue> macdMap = macd.getMacdMap();
		Map<Date, IndicatorValue> signalMap = macd.getSignalMap();
		double prevHistogramValue = macdMap.get(prevTime).getIndicatorValue()
				- signalMap.get(prevTime).getIndicatorValue();
		double currentHistogramValue = macdMap.get(currentTime).getIndicatorValue()
				- signalMap.get(currentTime).getIndicatorValue();
		System.out.println("prev macd histogram : " + prevHistogramValue);
		System.out.println("current macd histogram : " + currentHistogramValue);
		return (prevHistogramValue < currentHistogramValue);
	}

	private boolean bearishMarubozu(Candle candle) {
		if ((prevCandle.getHaCandle().getOpen() > prevCandle.getHaCandle().getClose())
				&& (candle.getOpen() > candle.getClose())
				&& (prevCandle.getHaCandle().getClose() > candle.getClose())) {
			return true;
		} else {
			return false;
		}
	}

	private boolean bullishMarubozu(Candle candle) {
		if ((prevCandle.getHaCandle().getOpen() < prevCandle.getHaCandle().getClose())
				&& (candle.getOpen() < candle.getClose())
				&& (prevCandle.getHaCandle().getClose() < candle.getClose())) {
			return true;
		} else {
			return false;
		}
	}

	private boolean isStrongSignal() {
		return adx.getAdx().getIndicatorValue() > 20;
	}

	private boolean isTradableRange() {
		double stockPrice = prevCandle.getHaCandle().getClose();
		Date currentTime = getNthLastKeyEntry(atr.getAtrMap(), 1);
		Date prevTime = getNthLastKeyEntry(atr.getAtrMap(), 2);
		double range = atr.getAtrMap().lastEntry().getValue().getIndicatorValue();
		return (range >= (0.004 * stockPrice)) && (range <= (0.02 * stockPrice));
	}

	private boolean highVolatility() {
		Date currentTime = getNthLastKeyEntry(atr.getAtrMap(), 1);
		double atrValue = atr.getAtrMap().get(currentTime).getIndicatorValue();
		double atrSignalValue = atr.getAtrSignal().get(currentTime).getIndicatorValue();
		return atrValue > atrSignalValue;
	}

	private boolean isUptrend(Candle candle) {
		double lastEma9 = ema5Map.lastEntry().getValue().getIndicatorValue();
		return candle.getClose() >= lastEma9;
	}

	private boolean isDowntrend(Candle candle) {
		double lastEma9 = ema5Map.lastEntry().getValue().getIndicatorValue();
		return candle.getClose() <= lastEma9;
	}

	@Override
	public void initializeSetup(List<Candle> cList) {
		List<HeikinAshiCandle> haClist = HeikinAshiBuilder.convertList(cList);
		List<Candle> haList = new ArrayList<>();
		for (HeikinAshiCandle hac : haClist) {
			haList.add(hac.getHaCandle());
		}
		candleSet = new TreeSet<>();
		atr = ATR.calculateATR(haList, 14);
		rsi = RSI.calculateRSI(haList);
		adx = ADX.calculateADX(haList);
		fastEmaMap = EMA.calculateEMA(12, haList);
		slowEmaMap = EMA.calculateEMA(26, haList);
		macd = MACD.calculateMACD(fastEmaMap, slowEmaMap, 9);
		ema5Map = EMA.calculateEMA(5, haList);
		ema8Map = EMA.calculateEMA(8, haList);
		ema13Map = EMA.calculateEMA(13, haList);
		prevCandle = HeikinAshiBuilder.convert(cList.get(cList.size() - 1), null);
		dayTradeDone = false;
		sendInitSetupDataMail();
	}

	private void sendInitSetupDataMail() {
		Date lastDataDate = getNthLastKeyEntry(macd.getMacdMap(), 1);
		System.out.println("sendInitSetupDataMail last date :: " + lastDataDate);
		IndicatorValue atrIv = atr.getAtrMap().lastEntry().getValue();
		IndicatorValue adxIv = adx.getAdx();
		IndicatorValue rsiIv = rsi.getRsiMap().lastEntry().getValue();
		IndicatorValue fastEma = fastEmaMap.lastEntry().getValue();
		IndicatorValue slowEma = slowEmaMap.lastEntry().getValue();
		IndicatorValue macdIv = macd.getMacdMap().lastEntry().getValue();
		IndicatorValue macdSignalIv = macd.getSignalMap().lastEntry().getValue();
		IndicatorValue ema9 = ema5Map.lastEntry().getValue();
		IndicatorValue ema21 = ema8Map.lastEntry().getValue();
		System.out.println("sendInitSetupDataMail atr :: " + atrIv.toString());
		System.out.println("sendInitSetupDataMail adx :: " + adxIv.toString());
		System.out.println("sendInitSetupDataMail rsi :: " + rsiIv.toString());
		System.out.println("sendInitSetupDataMail fast ema :: " + fastEma.toString());
		System.out.println("sendInitSetupDataMail slow ema :: " + slowEma.toString());
		System.out.println("sendInitSetupDataMail macd :: " + macdIv.toString());
		System.out.println("sendInitSetupDataMail macd signal :: " + macdSignalIv.toString());
		System.out.println("sendInitSetupDataMail 9 ema :: " + ema9.toString());
		System.out.println("sendInitSetupDataMail 21 ema :: " + ema21.toString());
		String mailbody = "ATR : " + atrIv.toString() + "\n" + "ADX : " + adxIv.toString() + "\n" + "RSI : "
				+ rsiIv.toString() + "\n" + "fast ema : " + fastEma.toString() + "\n" + "slow ema : "
				+ slowEma.toString() + "\n" + "macd : " + macdIv.toString() + "\n" + "macd signal : "
				+ macdSignalIv.toString() + "\n" + "9 ema : " + ema9.toString() + "\n" + "21 ema : " + ema21.toString();
		// MailSender.sendMail(Constants.TO_MAIL, Constants.TO_NAME,
		// Constants.MACD_RSI_STRATEGY_SETUP_DATA, mailbody);
	}

	@Override
	public void destroySetup() {

		candleSet = null;
		atr = null;
		rsi = null;
		macd = null;
		adx = null;
		fastEmaMap = null;
		slowEmaMap = null;
		ema5Map = null;
		ema8Map = null;
		ema13Map = null;
		dayTradeDone = false;
		prevCandle = null;
		levels = null;

		/*
		 * candleSet.clear(); dayTradeDone = false;
		 */
	}

	@Override
	public void updateSetup(Candle candle) {
		System.out.println("updateSetup date :: " + candle.getTime());
		double newfastEma = EMA.calculateEMA(12, candle, fastEmaMap.lastEntry().getValue().getIndicatorValue());
		double newSlowEma = EMA.calculateEMA(26, candle, slowEmaMap.lastEntry().getValue().getIndicatorValue());
		double new5Ema = EMA.calculateEMA(5, candle, ema5Map.lastEntry().getValue().getIndicatorValue());
		double new8Ema = EMA.calculateEMA(8, candle, ema8Map.lastEntry().getValue().getIndicatorValue());
		double new13Ema = EMA.calculateEMA(13, candle, ema13Map.lastEntry().getValue().getIndicatorValue());
		fastEmaMap.put(candle.getTime(), new IndicatorValue(candle.getTime(), newfastEma, IndicatorType.EMA));
		slowEmaMap.put(candle.getTime(), new IndicatorValue(candle.getTime(), newSlowEma, IndicatorType.EMA));
		ema5Map.put(candle.getTime(), new IndicatorValue(candle.getTime(), new5Ema, IndicatorType.EMA));
		ema8Map.put(candle.getTime(), new IndicatorValue(candle.getTime(), new8Ema, IndicatorType.EMA));
		ema13Map.put(candle.getTime(), new IndicatorValue(candle.getTime(), new13Ema, IndicatorType.EMA));
		RSI.updateRSI(candle, rsi);
		ATR.updateATR(candle, atr, 14);
		ADX.updateADX(candle, adx);
		MACD.updateMacdModel(this.macd, candle, newfastEma, newSlowEma, 9);

	}

}

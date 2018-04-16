/**
 * 
 */
package com.shaurya.intraday.strategy.impl;

import static com.shaurya.intraday.util.HelperUtil.getNthLastKeyEntry;
import static com.shaurya.intraday.util.HelperUtil.takeProfitReached;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import com.shaurya.intraday.constant.Constants;
import com.shaurya.intraday.enums.IndicatorType;
import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.indicator.ADX;
import com.shaurya.intraday.indicator.ATR;
import com.shaurya.intraday.indicator.EMA;
import com.shaurya.intraday.indicator.MACD;
import com.shaurya.intraday.indicator.RSI;
import com.shaurya.intraday.model.ADXModel;
import com.shaurya.intraday.model.ATRModel;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.IndicatorValue;
import com.shaurya.intraday.model.MACDModel;
import com.shaurya.intraday.model.RSIModel;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.strategy.EMAMacdAndRSIStrategy;
import com.shaurya.intraday.util.MailSender;

/**
 * @author Shaurya
 *
 */
public class EMAMacdAndRSIStrategyImpl implements EMAMacdAndRSIStrategy {
	// Modified macd of 12,26,9
	private ATRModel atr;
	private RSIModel rsi;
	private MACDModel macd;
	private ADXModel adx;
	private TreeMap<Date, IndicatorValue> fastEmaMap;
	private TreeMap<Date, IndicatorValue> slowEmaMap;
	private TreeMap<Date, IndicatorValue> ema200Map;
	private TreeSet<Candle> candleSet;

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
			Candle candle5min = form5MinCandle();
			if (candle5min != null) {
				updateSetup(candle5min);
				return getTradeCall(candle5min, openTrade);
			}
		}
		return null;
	}

	private Candle form5MinCandle() {
		Candle candle5min = null;
		if (candleSet.size() == 5) {
			int i = 0;
			Iterator<Candle> cItr = candleSet.iterator();
			while (cItr.hasNext()) {
				Candle c = cItr.next();
				if (i == 0) {
					candle5min = new Candle(c.getSecurity(), c.getToken(), c.getTime(), c.getOpen(), c.getHigh(),
							c.getLow(), c.getClose(), 0);
				} else {
					candle5min.setClose(c.getClose());
					candle5min.setHigh(Math.max(candle5min.getHigh(), c.getHigh()));
					candle5min.setLow(Math.min(candle5min.getLow(), c.getLow()));
				}
				i++;
			}
			candleSet.clear();
		}
		return candle5min;
	}

	private StrategyModel getTradeCall(Candle candle, StrategyModel openTrade) {
		Date currentTime = candle.getTime();
		double rsiValue = rsi.getRsiMap().get(currentTime).getIndicatorValue();
		double atrValue = atr.getAtrMap().get(currentTime).getIndicatorValue();
		if (openTrade == null) {
			if (isStrongSignal() && isUptrend(candle) && entryBullishMA() && rsiValue < 70) {
				return new StrategyModel(candle.getToken(), PositionType.LONG, atrValue, candle.getClose(),
						candle.getSecurity(), null, 0, false);
			}
			if (isStrongSignal() && isDowntrend(candle) && entryBearishMA() && rsiValue > 30) {
				return new StrategyModel(candle.getToken(), PositionType.SHORT, atrValue, candle.getClose(),
						candle.getSecurity(), null, 0, false);
			}
		} else {
			// always check for stop loss hit before exiting trade and update
			// reason in db
			if (takeProfitReached(candle, openTrade)) {
				openTrade.setTradePrice(candle.getClose());
				openTrade.setExitOrder(true);
				return openTrade;
			}
			if (openTrade.getPosition() == PositionType.LONG
					&& (exitBearishMacdHistogram() || isDowntrend(candle) || rsiValue > 80)) {
				openTrade.setTradePrice(candle.getClose());
				openTrade.setExitOrder(true);
				return openTrade;
			}
			if (openTrade.getPosition() == PositionType.SHORT
					&& (exitBullishMacdHistogram() || isUptrend(candle) || rsiValue < 20)) {
				openTrade.setTradePrice(candle.getClose());
				openTrade.setExitOrder(true);
				return openTrade;
			}
		}
		return null;

	}

	private boolean isStrongSignal() {
		return adx.getAdx().getIndicatorValue() > 20;
	}

	private boolean isUptrend(Candle candle) {
		double lastEma200 = ema200Map.lastEntry().getValue().getIndicatorValue();
		return candle.getClose() >= lastEma200;
	}

	private boolean isDowntrend(Candle candle) {
		double lastEma200 = ema200Map.lastEntry().getValue().getIndicatorValue();
		return candle.getClose() <= lastEma200;
	}

	private boolean entryBearishMA() {
		Date prevTime = getNthLastKeyEntry(fastEmaMap, 2);
		Date currentTime = getNthLastKeyEntry(fastEmaMap, 1);
		return (fastEmaMap.get(prevTime).getIndicatorValue() >= fastEmaMap.get(prevTime).getIndicatorValue())
				&& (fastEmaMap.get(currentTime).getIndicatorValue() < fastEmaMap.get(currentTime).getIndicatorValue());
	}

	private boolean exitBearishMacdHistogram() {
		Date prevTime = getNthLastKeyEntry(macd.getMacdMap(), 2);
		Date currentTime = getNthLastKeyEntry(macd.getMacdMap(), 1);
		Map<Date, IndicatorValue> macdMap = macd.getMacdMap();
		Map<Date, IndicatorValue> signalMap = macd.getSignalMap();
		double prevHistogramValue = macdMap.get(prevTime).getIndicatorValue()
				- signalMap.get(prevTime).getIndicatorValue();
		double currentHistogramValue = macdMap.get(currentTime).getIndicatorValue()
				- signalMap.get(currentTime).getIndicatorValue();
		// return (prevHistogramValue > currentHistogramValue);
		return (macdMap.get(prevTime).getIndicatorValue() >= signalMap.get(prevTime).getIndicatorValue())
				&& (macdMap.get(currentTime).getIndicatorValue() < signalMap.get(currentTime).getIndicatorValue());
	}

	private boolean entryBullishMA() {
		Date prevTime = getNthLastKeyEntry(fastEmaMap, 2);
		Date currentTime = getNthLastKeyEntry(fastEmaMap, 1);
		return (fastEmaMap.get(prevTime).getIndicatorValue() <= fastEmaMap.get(prevTime).getIndicatorValue())
				&& (fastEmaMap.get(currentTime).getIndicatorValue() > fastEmaMap.get(currentTime).getIndicatorValue());
	}

	private boolean exitBullishMacdHistogram() {
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
		// return (prevHistogramValue < currentHistogramValue);
		return (macdMap.get(prevTime).getIndicatorValue() <= signalMap.get(prevTime).getIndicatorValue())
				&& (macdMap.get(currentTime).getIndicatorValue() > signalMap.get(currentTime).getIndicatorValue());
	}

	@Override
	public void initializeSetup(List<Candle> cList) {
		candleSet = new TreeSet<>();
		atr = ATR.calculateATR(cList, 14);
		rsi = RSI.calculateRSI(cList);
		adx = ADX.calculateADX(cList);
		fastEmaMap = EMA.calculateEMA(12, cList);
		slowEmaMap = EMA.calculateEMA(26, cList);
		macd = MACD.calculateMACD(fastEmaMap, slowEmaMap, 12);
		ema200Map = EMA.calculateEMA(200, cList);

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
		IndicatorValue ema200 = ema200Map.lastEntry().getValue();
		System.out.println("sendInitSetupDataMail atr :: " + atrIv.toString());
		System.out.println("sendInitSetupDataMail adx :: " + adxIv.toString());
		System.out.println("sendInitSetupDataMail rsi :: " + rsiIv.toString());
		System.out.println("sendInitSetupDataMail fast ema :: " + fastEma.toString());
		System.out.println("sendInitSetupDataMail slow ema :: " + slowEma.toString());
		System.out.println("sendInitSetupDataMail macd :: " + macdIv.toString());
		System.out.println("sendInitSetupDataMail macd signal :: " + macdSignalIv.toString());
		System.out.println("sendInitSetupDataMail 200 ema :: " + ema200.toString());
		String mailbody = "ATR : " + atrIv.toString() + "\n" + "ADX : " + adxIv.toString() + "\n" + "RSI : "
				+ rsiIv.toString() + "\n" + "fast ema : " + fastEma.toString() + "\n" + "slow ema : "
				+ slowEma.toString() + "\n" + "macd : " + macdIv.toString() + "\n" + "macd signal : "
				+ macdSignalIv.toString() + "\n" + "200 ema : " + ema200.toString();
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
		ema200Map = null;
	}

	@Override
	public void updateSetup(Candle candle) {
		System.out.println("updateSetup date :: " + candle.getTime());
		double newfastEma = EMA.calculateEMA(12, candle, fastEmaMap.lastEntry().getValue().getIndicatorValue());
		double newSlowEma = EMA.calculateEMA(26, candle, slowEmaMap.lastEntry().getValue().getIndicatorValue());
		double new200Ema = EMA.calculateEMA(200, candle, ema200Map.lastEntry().getValue().getIndicatorValue());
		fastEmaMap.put(candle.getTime(), new IndicatorValue(candle.getTime(), newfastEma, IndicatorType.EMA));
		slowEmaMap.put(candle.getTime(), new IndicatorValue(candle.getTime(), newSlowEma, IndicatorType.EMA));
		ema200Map.put(candle.getTime(), new IndicatorValue(candle.getTime(), new200Ema, IndicatorType.EMA));
		RSI.updateRSI(candle, rsi);
		ATR.updateATR(candle, atr, 14);
		ADX.updateADX(candle, adx);
		MACD.updateMacdModel(this.macd, candle, newfastEma, newSlowEma, 12);
	}

}

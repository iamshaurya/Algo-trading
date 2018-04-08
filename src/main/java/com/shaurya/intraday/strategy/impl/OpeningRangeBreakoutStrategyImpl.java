/**
 * 
 */
package com.shaurya.intraday.strategy.impl;

import static com.shaurya.intraday.util.HelperUtil.getNthLastKeyEntry;
import static com.shaurya.intraday.util.HelperUtil.stopLossReached;
import static com.shaurya.intraday.util.HelperUtil.takeProfitReached;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
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
import com.shaurya.intraday.strategy.OpeningRangeBreakoutStrategy;
import com.shaurya.intraday.util.MailSender;

/**
 * @author Shaurya
 *
 */
public class OpeningRangeBreakoutStrategyImpl implements OpeningRangeBreakoutStrategy {
	// Modified macd of 12,26,9
	private ATRModel atr;
	private RSIModel rsi;
	private MACDModel macd;
	private ADXModel adx;
	private TreeMap<Date, IndicatorValue> fastEmaMap;
	private TreeMap<Date, IndicatorValue> slowEmaMap;
	private TreeMap<Date, IndicatorValue> ema200Map;
	private TreeMap<Date, IndicatorValue> ema90Map;
	private TreeSet<Candle> candle30Set;
	private TreeSet<Candle> candle5Set;
	private Candle first30minCandle;
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
			candle5Set.add(candle);
			Candle candle5min = form5MinCandle();
			if (candle5min != null) {
				updateSetup(candle5min);
				if (first30minCandle == null) {
					candle30Set.add(candle5min);
					first30minCandle = form30MinCandle();
				}
				if (first30minCandle != null) {
					return getTradeCall(candle5min, openTrade);
				}
			}
		}
		return null;
	}

	private Candle form30MinCandle() {
		Candle candle30min = null;
		if (candle30Set.size() == 6) {
			int i = 0;
			Iterator<Candle> cItr = candle30Set.iterator();
			while (cItr.hasNext()) {
				Candle c = cItr.next();
				if (i == 0) {
					candle30min = new Candle(c.getSecurity(), c.getTime(), c.getOpen(), c.getHigh(), c.getLow(),
							c.getClose(), 0);
				} else {
					candle30min.setClose(c.getClose());
					candle30min.setHigh(Math.max(candle30min.getHigh(), c.getHigh()));
					candle30min.setLow(Math.min(candle30min.getLow(), c.getLow()));
				}
				i++;
			}
			candle30Set.clear();
		}
		return candle30min;
	}

	private Candle form5MinCandle() {
		Candle candle5min = null;
		if (candle5Set.size() == 5) {
			int i = 0;
			Iterator<Candle> cItr = candle5Set.iterator();
			while (cItr.hasNext()) {
				Candle c = cItr.next();
				if (i == 0) {
					candle5min = new Candle(c.getSecurity(), c.getTime(), c.getOpen(), c.getHigh(), c.getLow(),
							c.getClose(), 0);
				} else {
					candle5min.setClose(c.getClose());
					candle5min.setHigh(Math.max(candle5min.getHigh(), c.getHigh()));
					candle5min.setLow(Math.min(candle5min.getLow(), c.getLow()));
				}
				i++;
			}
			candle5Set.clear();
		}
		return candle5min;
	}

	private StrategyModel getTradeCall(Candle candle, StrategyModel openTrade) {
		StrategyModel tradeCall = null;
		Date currentTime = candle.getTime();
		double rsiValue = rsi.getRsiMap().get(currentTime).getIndicatorValue();
		if (openTrade == null) {
			if (isTradableRange() && !dayTradeDone && candle.getClose() > first30minCandle.getHigh() && isStrongSignal()
					&& rsiValue < 70) {
				dayTradeDone = true;
				tradeCall = new StrategyModel(PositionType.LONG, (0.0050 * first30minCandle.getLow()),
						candle.getClose(), candle.getSecurity(), null, 0, false);
			}
			if (isTradableRange() && !dayTradeDone && candle.getClose() < first30minCandle.getLow() && isStrongSignal()
					&& rsiValue > 30) {
				dayTradeDone = true;
				tradeCall = new StrategyModel(PositionType.SHORT, (0.0050 * first30minCandle.getHigh()),
						candle.getClose(), candle.getSecurity(), null, 0, false);
			}
		} else {
			// always check for stop loss hit before exiting trade and update
			// reason in db
			if (takeProfitReached(candle, openTrade)) {
				tradeCall = new StrategyModel(openTrade.getPosition(), openTrade.getAtr(), candle.getClose(),
						openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(), true);
			}
			if (stopLossReached(candle, openTrade)) {
				tradeCall = new StrategyModel(openTrade.getPosition(), openTrade.getAtr(), candle.getClose(),
						openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(), true);
			}
		}
		return tradeCall;

	}

	private boolean isTradableRange() {
		double stockPrice = first30minCandle.getOpen();
		double range = first30minCandle.getHigh() - first30minCandle.getLow();
		return (range >= (0.004 * stockPrice)) && (range <= (0.02 * stockPrice));
	}

	private boolean isStrongSignal() {
		return true;
		// return adx.getAdx().getIndicatorValue() > 20;
	}

	private boolean highVolatility() {
		Date currentTime = getNthLastKeyEntry(atr.getAtrMap(), 1);
		double atrValue = atr.getAtrMap().get(currentTime).getIndicatorValue();
		double atrSignalValue = atr.getAtrSignal().get(currentTime).getIndicatorValue();
		return atrValue > atrSignalValue;
	}

	private boolean maUptrend() {
		Date currentTime = getNthLastKeyEntry(fastEmaMap, 1);
		return fastEmaMap.get(currentTime).getIndicatorValue() > slowEmaMap.get(currentTime).getIndicatorValue();
	}

	private boolean maDowntrend() {
		Date currentTime = getNthLastKeyEntry(fastEmaMap, 1);
		return fastEmaMap.get(currentTime).getIndicatorValue() < slowEmaMap.get(currentTime).getIndicatorValue();
	}

	private boolean isUptrend(Candle candle) {
		double lastEma90 = ema90Map.lastEntry().getValue().getIndicatorValue();
		return candle.getClose() >= lastEma90;
	}

	private boolean isDowntrend(Candle candle) {
		double lastEma90 = ema90Map.lastEntry().getValue().getIndicatorValue();
		return candle.getClose() <= lastEma90;
	}

	@Override
	public void initializeSetup(List<Candle> cList) {
		candle30Set = new TreeSet<>();
		candle5Set = new TreeSet<>();
		atr = ATR.calculateATR(cList, 14);
		rsi = RSI.calculateRSI(cList);
		adx = ADX.calculateADX(cList);
		fastEmaMap = EMA.calculateEMA(12, cList);
		slowEmaMap = EMA.calculateEMA(26, cList);
		macd = MACD.calculateMACD(fastEmaMap, slowEmaMap, 12);
		ema200Map = EMA.calculateEMA(200, cList);
		ema90Map = EMA.calculateEMA(90, cList);
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
		IndicatorValue ema200 = ema200Map.lastEntry().getValue();
		IndicatorValue ema90 = ema90Map.lastEntry().getValue();
		System.out.println("sendInitSetupDataMail atr :: " + atrIv.toString());
		System.out.println("sendInitSetupDataMail adx :: " + adxIv.toString());
		System.out.println("sendInitSetupDataMail rsi :: " + rsiIv.toString());
		System.out.println("sendInitSetupDataMail fast ema :: " + fastEma.toString());
		System.out.println("sendInitSetupDataMail slow ema :: " + slowEma.toString());
		System.out.println("sendInitSetupDataMail macd :: " + macdIv.toString());
		System.out.println("sendInitSetupDataMail macd signal :: " + macdSignalIv.toString());
		System.out.println("sendInitSetupDataMail 200 ema :: " + ema200.toString());
		System.out.println("sendInitSetupDataMail 90 ema :: " + ema90.toString());
		String mailbody = "ATR : " + atrIv.toString() + "\n" + "ADX : " + adxIv.toString() + "\n" + "RSI : "
				+ rsiIv.toString() + "\n" + "fast ema : " + fastEma.toString() + "\n" + "slow ema : "
				+ slowEma.toString() + "\n" + "macd : " + macdIv.toString() + "\n" + "macd signal : "
				+ macdSignalIv.toString() + "\n" + "200 ema : " + ema200.toString() + "\n" + "90 ema : "
				+ ema90.toString();
		//MailSender.sendMail(Constants.TO_MAIL, Constants.TO_NAME, Constants.MACD_RSI_STRATEGY_SETUP_DATA, mailbody);
	}

	@Override
	public void destroySetup() {
		candle5Set = null;
		candle30Set = null;
		atr = null;
		rsi = null;
		macd = null;
		adx = null;
		fastEmaMap = null;
		slowEmaMap = null;
		ema200Map = null;
		first30minCandle = null;
		dayTradeDone = false;
		
		/*candle5Set.clear();
		candle30Set.clear();*/
		 
	}

	@Override
	public void updateSetup(Candle candle) {
		System.out.println("updateSetup date :: " + candle.getTime());
		double newfastEma = EMA.calculateEMA(12, candle, fastEmaMap.lastEntry().getValue().getIndicatorValue());
		double newSlowEma = EMA.calculateEMA(26, candle, slowEmaMap.lastEntry().getValue().getIndicatorValue());
		double new200Ema = EMA.calculateEMA(200, candle, ema200Map.lastEntry().getValue().getIndicatorValue());
		double new90Ema = EMA.calculateEMA(90, candle, ema90Map.lastEntry().getValue().getIndicatorValue());
		fastEmaMap.put(candle.getTime(), new IndicatorValue(candle.getTime(), newfastEma, IndicatorType.EMA));
		slowEmaMap.put(candle.getTime(), new IndicatorValue(candle.getTime(), newSlowEma, IndicatorType.EMA));
		ema200Map.put(candle.getTime(), new IndicatorValue(candle.getTime(), new200Ema, IndicatorType.EMA));
		ema90Map.put(candle.getTime(), new IndicatorValue(candle.getTime(), new90Ema, IndicatorType.EMA));
		RSI.updateRSI(candle, rsi);
		ATR.updateATR(candle, atr, 14);
		ADX.updateADX(candle, adx);
		MACD.updateMacdModel(this.macd, candle, newfastEma, newSlowEma, 12);
	}

}

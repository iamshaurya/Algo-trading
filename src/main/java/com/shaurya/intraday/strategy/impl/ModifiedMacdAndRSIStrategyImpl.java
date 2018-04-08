/**
 * 
 */
package com.shaurya.intraday.strategy.impl;

import static com.shaurya.intraday.util.HelperUtil.getNthLastKeyEntry;
import static com.shaurya.intraday.util.HelperUtil.takeProfitReached;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import com.shaurya.intraday.strategy.ModifiedMacdAndRSIStrategy;
import com.shaurya.intraday.util.MailSender;

/**
 * @author Shaurya
 *
 */
public class ModifiedMacdAndRSIStrategyImpl implements ModifiedMacdAndRSIStrategy {
	// Modified macd of 12,26,12
	private ATRModel atr;
	private RSIModel rsi;
	private MACDModel macd;
	private ADXModel adx;
	private TreeMap<Date, IndicatorValue> fastEmaMap;
	private TreeMap<Date, IndicatorValue> slowEmaMap;
	private TreeMap<Date, IndicatorValue> ema200Map;

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
			updateSetup(candle);
		}
		Date currentTime = candle.getTime();
		double rsiValue = rsi.getRsiMap().get(currentTime).getIndicatorValue();
		double atrValue = atr.getAtrMap().get(currentTime).getIndicatorValue();
		if (openTrade == null) {
			if (isStrongSignal() && isUptrend(candle) && bullishMacdCrossover() && rsiValue > 40 && rsiValue < 80) {
				return new StrategyModel(PositionType.LONG, atrValue, candle.getClose(), candle.getSecurity(), null, 0,
						false);
			}
			if (isStrongSignal() && isDowntrend(candle) && bearishMacdCrossover() && rsiValue > 20 && rsiValue < 60) {
				return new StrategyModel(PositionType.SHORT, atrValue, candle.getClose(), candle.getSecurity(), null, 0,
						false);
			}
		} else {
			// always check for stop loss hit before exiting trade and update
			// reason in db
			if (takeProfitReached(candle, openTrade)) {
				return new StrategyModel(openTrade.getPosition(), openTrade.getAtr(), candle.getClose(),
						openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(), true);
			}
			if (openTrade.getPosition() == PositionType.LONG
					&& (bearishMacdCrossover() || isDowntrend(candle) || rsiValue >= 80)) {
				return new StrategyModel(openTrade.getPosition(), openTrade.getAtr(), candle.getClose(),
						openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(), true);
			}
			if (openTrade.getPosition() == PositionType.SHORT
					&& (bullishMacdCrossover() || isUptrend(candle) || rsiValue <= 20)) {
				return new StrategyModel(openTrade.getPosition(), openTrade.getAtr(), candle.getClose(),
						openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(), true);
			}
		}
		return null;
	}

	private boolean isStrongSignal() {
		// commenting as of now, need to backtest
		return true;
		// return adx.getAdx().getIndicatorValue() >= 20;
	}

	private boolean isUptrend(Candle candle) {
		double lastEma200 = ema200Map.lastEntry().getValue().getIndicatorValue();
		return candle.getClose() >= lastEma200;
	}

	private boolean isDowntrend(Candle candle) {
		double lastEma200 = ema200Map.lastEntry().getValue().getIndicatorValue();
		return candle.getClose() <= lastEma200;
	}

	private boolean bearishMacdCrossover() {
		Date prevTime = getNthLastKeyEntry(macd.getMacdMap(), 2);
		Date currentTime = getNthLastKeyEntry(macd.getMacdMap(), 1);
		Map<Date, IndicatorValue> macdMap = macd.getMacdMap();
		Map<Date, IndicatorValue> signalMap = macd.getSignalMap();
		return macdMap.get(prevTime).getIndicatorValue() >= signalMap.get(prevTime).getIndicatorValue()
				&& macdMap.get(currentTime).getIndicatorValue() < signalMap.get(currentTime).getIndicatorValue();
	}

	private boolean bullishMacdCrossover() {
		Date prevTime = getNthLastKeyEntry(macd.getMacdMap(), 2);
		Date currentTime = getNthLastKeyEntry(macd.getMacdMap(), 1);
		Map<Date, IndicatorValue> macdMap = macd.getMacdMap();
		Map<Date, IndicatorValue> signalMap = macd.getSignalMap();
		System.out.println("prev macd : " + macdMap.get(prevTime).getIndicatorValue() + " and prev signal : "
				+ signalMap.get(prevTime).getIndicatorValue());
		;
		System.out.println("current macd : " + macdMap.get(currentTime).getIndicatorValue() + " and current signal : "
				+ signalMap.get(currentTime).getIndicatorValue());
		return macdMap.get(prevTime).getIndicatorValue() <= signalMap.get(prevTime).getIndicatorValue()
				&& macdMap.get(currentTime).getIndicatorValue() > signalMap.get(currentTime).getIndicatorValue();
	}

	@Override
	public void initializeSetup(List<Candle> cList) {
		atr = ATR.calculateATR(cList, 14);
		rsi = RSI.calculateRSI(cList);
		adx = ADX.calculateADX(cList);
		fastEmaMap = EMA.calculateEMA(20, cList);
		slowEmaMap = EMA.calculateEMA(50, cList);
		macd = MACD.calculateMACD(fastEmaMap, slowEmaMap, 20);
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
		//MailSender.sendMail(Constants.TO_MAIL, Constants.TO_NAME, Constants.MACD_RSI_STRATEGY_SETUP_DATA, mailbody);
	}

	@Override
	public void destroySetup() {
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
		double newfastEma = EMA.calculateEMA(20, candle, fastEmaMap.lastEntry().getValue().getIndicatorValue());
		double newSlowEma = EMA.calculateEMA(50, candle, slowEmaMap.lastEntry().getValue().getIndicatorValue());
		double new200Ema = EMA.calculateEMA(200, candle, ema200Map.lastEntry().getValue().getIndicatorValue());
		fastEmaMap.put(candle.getTime(), new IndicatorValue(candle.getTime(), newfastEma, IndicatorType.EMA));
		slowEmaMap.put(candle.getTime(), new IndicatorValue(candle.getTime(), newSlowEma, IndicatorType.EMA));
		ema200Map.put(candle.getTime(), new IndicatorValue(candle.getTime(), new200Ema, IndicatorType.EMA));
		RSI.updateRSI(candle, rsi);
		ATR.updateATR(candle, atr, 14);
		ADX.updateADX(candle, adx);
		MACD.updateMacdModel(this.macd, candle, newfastEma, newSlowEma, 20);
	}

}

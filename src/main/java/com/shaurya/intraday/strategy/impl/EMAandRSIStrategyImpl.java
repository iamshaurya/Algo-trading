/**
 * 
 */
package com.shaurya.intraday.strategy.impl;

import static com.shaurya.intraday.util.HelperUtil.getLastDayEndDate;
import static com.shaurya.intraday.util.HelperUtil.getPrevDateInMinute;
import static com.shaurya.intraday.util.HelperUtil.takeProfitReached;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import com.shaurya.intraday.constant.Constants;
import com.shaurya.intraday.enums.IndicatorType;
import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.indicator.ATR;
import com.shaurya.intraday.indicator.EMA;
import com.shaurya.intraday.indicator.RSI;
import com.shaurya.intraday.model.ATRModel;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.IndicatorValue;
import com.shaurya.intraday.model.RSIModel;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.strategy.EMAandRSIStrategy;
import com.shaurya.intraday.util.MailSender;

/**
 * @author Shaurya
 *
 */
public class EMAandRSIStrategyImpl implements EMAandRSIStrategy {
	// Taking 20-EMA and 50-EMA
	private ATRModel atr;
	private TreeMap<Date, IndicatorValue> fastEmaMap;
	private TreeMap<Date, IndicatorValue> slowEmaMap;
	private RSIModel rsi;

	/*
	 * case 1) if no open trade, then proceed sub case 1) if prev 20EMA < prev
	 * 50EMA && current 20EMA > current 50EMA && RSI < 75 && RSI > 40 then take
	 * a long position with BO sub case 2) if prev 20EMA > prev 50EMA && current
	 * 20EMA < current 50EMA && RSI > 25 && RSI <60 then take a short position
	 * with BO case 2) if a trade is open, then proceed sub case 1) long trade
	 * is open && (Take profit is reached {fetch order detail and update in db
	 * and close the order} || (20EMA > prev 50EMA && current 20EMA < current
	 * 50EMA) || RSI > 75) then close trade sub case 2) short trade is open &&
	 * (Take profit is reached {fetch order detail and update in db and close
	 * the order} || (20EMA < prev 50EMA && current 20EMA > current 50EMA) ||
	 * RSI < 25) then close trade
	 */
	@Override
	public StrategyModel processTrades(Candle candle, StrategyModel openTrade, boolean updateSetup) {
		if (updateSetup) {
			updateSetup(candle);
		}
		Date prevTime = getPrevDateInMinute(candle.getTime());
		Date currentTime = candle.getTime();
		double rsiValue = rsi.getRsiMap().get(currentTime).getIndicatorValue();
		double atrValue = atr.getAtrMap().get(currentTime).getIndicatorValue();
		if (openTrade == null) {
			if (bullishCrossover(prevTime, currentTime) && rsiValue > 40 && rsiValue < 75) {
				return new StrategyModel(PositionType.LONG, atrValue, candle.getClose(), candle.getSecurity(), null, 0,
						false);
			}
			if (bearishCrossover(prevTime, currentTime) && rsiValue > 25 && rsiValue < 60) {
				return new StrategyModel(PositionType.SHORT, atrValue, candle.getClose(), candle.getSecurity(), null, 0,
						false);
			}
		} else {
			// always check for stop loss hit before exiting trade and update
			// reason in db
			if (takeProfitReached(candle, openTrade)) {
				openTrade.setExitOrder(true);
				return openTrade;
			}
			if (openTrade.getPosition() == PositionType.LONG
					&& (bearishCrossover(prevTime, currentTime) || rsiValue > 75)) {
				openTrade.setExitOrder(true);
				return openTrade;
			}
			if (openTrade.getPosition() == PositionType.SHORT
					&& (bullishCrossover(prevTime, currentTime) || rsiValue < 25)) {
				openTrade.setExitOrder(true);
				return openTrade;
			}
		}
		return null;
	}

	private boolean bearishCrossover(Date prevTime, Date currentTime) {
		return fastEmaMap.get(prevTime).getIndicatorValue() > slowEmaMap.get(prevTime).getIndicatorValue()
				&& fastEmaMap.get(currentTime).getIndicatorValue() < slowEmaMap.get(currentTime).getIndicatorValue();
	}

	private boolean bullishCrossover(Date prevTime, Date currentTime) {
		return fastEmaMap.get(prevTime).getIndicatorValue() < slowEmaMap.get(prevTime).getIndicatorValue()
				&& fastEmaMap.get(currentTime).getIndicatorValue() > slowEmaMap.get(currentTime).getIndicatorValue();
	}

	@Override
	public void initializeSetup(List<Candle> cList) {
		atr = ATR.calculateATR(cList);
		rsi = RSI.calculateRSI(cList);
		fastEmaMap = EMA.calculateEMA(20, cList);
		slowEmaMap = EMA.calculateEMA(50, cList);
		
		sendInitSetupDataMail();
	}

	private void sendInitSetupDataMail() {
		IndicatorValue atrIv = atr.getAtrMap().lastEntry().getValue();
		IndicatorValue rsiIv = rsi.getRsiMap().lastEntry().getValue();
		IndicatorValue fastEma = fastEmaMap.lastEntry().getValue();
		IndicatorValue slowEma = slowEmaMap.lastEntry().getValue();
		String mailbody = "ATR : " + atrIv.toString() + "\n" + "RSI : " + rsiIv.toString() + "\n" + "fast ema : "
				+ fastEma.toString() + "\n" + "slow ema : " + slowEma.toString();
		//MailSender.sendMail(Constants.TO_MAIL, Constants.TO_NAME, Constants.EMA_RSI_STRATEGY_SETUP_DATA, mailbody);
	}

	@Override
	public void updateSetup(Candle candle) {
		double newfastEma = EMA.calculateEMA(20, candle, fastEmaMap.lastEntry().getValue().getIndicatorValue());
		double newSlowEma = EMA.calculateEMA(50, candle, slowEmaMap.lastEntry().getValue().getIndicatorValue());
		fastEmaMap.put(candle.getTime(), new IndicatorValue(candle.getTime(), newfastEma, IndicatorType.EMA));
		slowEmaMap.put(candle.getTime(), new IndicatorValue(candle.getTime(), newSlowEma, IndicatorType.EMA));
		RSI.updateRSI(candle, rsi);
		ATR.updateATR(candle, atr);
	}

	@Override
	public void destroySetup() {
		atr = null;
		rsi = null;
		fastEmaMap = null;
		slowEmaMap = null;
	}

}

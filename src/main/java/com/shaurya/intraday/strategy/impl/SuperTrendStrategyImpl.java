/**
 * 
 */
package com.shaurya.intraday.strategy.impl;

import static com.shaurya.intraday.util.HelperUtil.stopLossReached;

import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.indicator.ATR;
import com.shaurya.intraday.indicator.SuperTrend;
import com.shaurya.intraday.model.ATRModel;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.model.SuperTrendModel;
import com.shaurya.intraday.strategy.SuperTrendStrategy;

/**
 * @author Shaurya
 *
 */
public class SuperTrendStrategyImpl implements SuperTrendStrategy {
	private TreeSet<Candle> candle5Set;
	private ATRModel atr7;
	private ATRModel atr10;
	private SuperTrendModel superTrend7_3;
	private SuperTrendModel superTrend7_2;
	private SuperTrendModel superTrend10_3;

	@Override
	public StrategyModel processTrades(Candle candle, StrategyModel openTrade, boolean updateSetup) {
		if (updateSetup) {
			candle5Set.add(candle);
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
		if (candle5Set.size() == 5) {
			int i = 0;
			Iterator<Candle> cItr = candle5Set.iterator();
			while (cItr.hasNext()) {
				Candle c = cItr.next();
				if (i == 0) {
					candle5min = new Candle(c.getSecurity(), c.getTime(), c.getOpen(), c.getHigh(), c.getLow(),
							c.getClose(), c.getVolume());
				} else {
					candle5min.setClose(c.getClose());
					candle5min.setHigh(Math.max(candle5min.getHigh(), c.getHigh()));
					candle5min.setLow(Math.min(candle5min.getLow(), c.getLow()));
					candle5min.setVolume(candle5min.getVolume() + c.getVolume());
				}
				i++;
			}
			candle5Set.clear();
		}
		return candle5min;
	}

	private StrategyModel getTradeCall(Candle candle, StrategyModel openTrade) {
		StrategyModel tradeCall = null;
		if (openTrade == null) {
			if (longSignal(candle)) {
				tradeCall = new StrategyModel(PositionType.LONG, (0.0015 * candle.getClose()), candle.getClose(),
						candle.getSecurity(), null, 0, false);
			}
			if (shortSignal(candle)) {
				tradeCall = new StrategyModel(PositionType.SHORT, (0.0015 * candle.getClose()), candle.getClose(),
						candle.getSecurity(), null, 0, false);
			}
		} else if (openTrade != null) {
			// always check for stop loss hit before exiting trade and update
			// reason in db
			if (stopLossReached(candle, openTrade)) {
				tradeCall = new StrategyModel(openTrade.getPosition(), (double) (openTrade.getSl() / 2),
						candle.getClose(), openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(),
						true);
			}
			if(openTrade.getPosition() == PositionType.LONG && shortSignal(candle)){
				tradeCall = new StrategyModel(openTrade.getPosition(), (double) (openTrade.getSl() / 2),
						candle.getClose(), openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(),
						true);
			}
			if(openTrade.getPosition() == PositionType.SHORT && longSignal(candle)){
				tradeCall = new StrategyModel(openTrade.getPosition(), (double) (openTrade.getSl() / 2),
						candle.getClose(), openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(),
						true);
			}
		}
		return tradeCall;
	}
	
	public boolean longSignal(Candle candle){
		double st7_2 = superTrend7_2.getSuperTrendMap().lastEntry().getValue().getIndicatorValue();
		double st7_3 = superTrend7_3.getSuperTrendMap().lastEntry().getValue().getIndicatorValue();
		double st10_3 = superTrend10_3.getSuperTrendMap().lastEntry().getValue().getIndicatorValue();
		return (st7_2 < candle.getClose()) && (st7_3 < candle.getClose()) && (st10_3 < candle.getClose());
	}
	
	public boolean shortSignal(Candle candle){
		double st7_2 = superTrend7_2.getSuperTrendMap().lastEntry().getValue().getIndicatorValue();
		double st7_3 = superTrend7_3.getSuperTrendMap().lastEntry().getValue().getIndicatorValue();
		double st10_3 = superTrend10_3.getSuperTrendMap().lastEntry().getValue().getIndicatorValue();
		return (st7_2 > candle.getClose()) && (st7_3 > candle.getClose()) && (st10_3 > candle.getClose());
	}

	@Override
	public void initializeSetup(List<Candle> cList) {
		candle5Set = new TreeSet<>();
		atr7 = ATR.calculateATR(cList, 7);
		atr10 = ATR.calculateATR(cList, 10);
		superTrend7_3 = SuperTrend.calculateSuperTrend(cList, atr7, 3);
		superTrend7_2 = SuperTrend.calculateSuperTrend(cList, atr7, 2);
		superTrend10_3 = SuperTrend.calculateSuperTrend(cList, atr10, 3);
	}

	@Override
	public void updateSetup(Candle candle) {
		ATR.updateATR(candle, atr7, 7);
		ATR.updateATR(candle, atr10, 10);
		SuperTrend.updateSuperTrend(candle, atr7, superTrend7_2, 2);
		SuperTrend.updateSuperTrend(candle, atr7, superTrend7_3, 3);
		SuperTrend.updateSuperTrend(candle, atr10, superTrend10_3, 3);
	}

	@Override
	public void destroySetup() {
		/*atr7 = null;
		atr10 = null;
		superTrend7_2 = null;
		superTrend7_3 = null;
		superTrend10_3 = null;*/
		candle5Set.clear();
	}

}

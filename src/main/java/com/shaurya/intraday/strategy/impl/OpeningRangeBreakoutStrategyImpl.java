/**
 * 
 */
package com.shaurya.intraday.strategy.impl;

import static com.shaurya.intraday.util.HelperUtil.stopLossReached;

import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.strategy.OpeningRangeBreakoutStrategy;

/**
 * @author Shaurya
 *
 */
public class OpeningRangeBreakoutStrategyImpl implements OpeningRangeBreakoutStrategy {
	// Modified macd of 12,26,9
	private TreeSet<Candle> candle5Set;
	private TreeSet<Candle> candle15Set;
	private Candle first15minCandle;
	private boolean dayTradeDone;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.shaurya.intraday.strategy.Strategy#processTrades(java.util.List)
	 */

	@Override
	public StrategyModel processTrades(Candle candle, StrategyModel openTrade, boolean updateSetup) {
		if (updateSetup) {
			candle5Set.add(candle);
			Candle candle5min = form5MinCandle();
			if(candle5min != null){
				updateSetup(candle5min);
				if (first15minCandle == null) {
					candle15Set.add(candle5min);
					first15minCandle = form15MinCandle();
				}
				if (first15minCandle != null) {
					return getTradeCall(candle5min, openTrade);
				}
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
					candle5min = new Candle(c.getSecurity(), c.getToken(), c.getTime(), c.getOpen(), c.getHigh(),
							c.getLow(), c.getClose(), 0);
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
	
	private Candle form15MinCandle() {
		Candle candle15min = null;
		if (candle15Set.size() == 3) {
			int i = 0;
			Iterator<Candle> cItr = candle15Set.iterator();
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
			candle15Set.clear();
		}
		return candle15min;
	}

	private StrategyModel getTradeCall(Candle candle, StrategyModel openTrade) {
		StrategyModel tradeCall = null;
		if (openTrade == null) {
			if (!dayTradeDone && isTradeableRange() && candle.getClose() > first15minCandle.getHigh()) {
				dayTradeDone = true;
				tradeCall = new StrategyModel(candle.getToken(), PositionType.LONG,
						(0.5 * (candle.getClose() - first15minCandle.getLow())), candle.getClose(),
						candle.getSecurity(), null, 0, false);
			}
			if (!dayTradeDone && isTradeableRange() && candle.getClose() < first15minCandle.getLow()) {
				dayTradeDone = true;
				tradeCall = new StrategyModel(candle.getToken(), PositionType.SHORT,
						(0.5 * (first15minCandle.getHigh() - candle.getClose())), candle.getClose(),
						candle.getSecurity(), null, 0, false);
			}
		} else {
			// always check for stop loss hit before exiting trade and update
			// reason in db
			/*if (takeProfitReached(candle, openTrade)) {
				tradeCall = new StrategyModel(candle.getToken(), openTrade.getPosition(),
						(double) (openTrade.getSl() / 2), candle.getClose(), openTrade.getSecurity(),
						openTrade.getOrderId(), openTrade.getQuantity(), true);
			}*/
			if (stopLossReached(candle, openTrade)) {
				tradeCall = new StrategyModel(candle.getToken(), openTrade.getPosition(),
						(double) (openTrade.getSl() / 2), candle.getClose(), openTrade.getSecurity(),
						openTrade.getOrderId(), openTrade.getQuantity(), true);
			}
		}
		return tradeCall;

	}
	
	private boolean isTradeableRange(){
		double range = first15minCandle.getHigh() - first15minCandle.getLow();
		return range <= (0.015 * first15minCandle.getClose());
	}

	private boolean takeProfitReached(Candle candle, StrategyModel openTrade) {
		switch (openTrade.getPosition()) {
		case LONG:
			return candle.getClose() >= (1.005 * openTrade.getTradePrice());
		case SHORT:
			return candle.getClose() <= (0.995 * openTrade.getTradePrice());
		default:
			break;
		}
		return false;
	}

	@Override
	public void initializeSetup(List<Candle> cList) {
		candle15Set = new TreeSet<>();
		candle5Set = new TreeSet<>();
		dayTradeDone = false;
	}

	@Override
	public void destroySetup() {
		candle15Set = null;
		candle5Set = null;
		first15minCandle = null;
		dayTradeDone = false;

		/*
		 * candle5Set.clear(); candle30Set.clear();
		 */

	}

	@Override
	public void updateSetup(Candle candle) {
		System.out.println("updateSetup date :: " + candle.getTime());
	}

}

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
			updateSetup(candle);
			if (first15minCandle == null) {
				candle15Set.add(candle);
				first15minCandle = form15MinCandle();
			}
			if (first15minCandle != null) {
				return getTradeCall(candle, openTrade);
			}
		}
		return null;
	}

	private Candle form15MinCandle() {
		Candle candle15min = null;
		if (candle15Set.size() == 15) {
			int i = 0;
			Iterator<Candle> cItr = candle15Set.iterator();
			while (cItr.hasNext()) {
				Candle c = cItr.next();
				if (i == 0) {
					candle15min = new Candle(c.getSecurity(), c.getTime(), c.getOpen(), c.getHigh(), c.getLow(),
							c.getClose(), 0);
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
			if (!dayTradeDone && candle.getClose() > first15minCandle.getHigh()) {
				dayTradeDone = true;
				tradeCall = new StrategyModel(PositionType.LONG, 0.0015*candle.getClose(), candle.getClose(),
						candle.getSecurity(), null, 0, false);
			}
			if (!dayTradeDone && candle.getClose() < first15minCandle.getLow()) {
				dayTradeDone = true;
				tradeCall = new StrategyModel(PositionType.SHORT, 0.0015*candle.getClose(), candle.getClose(),
						candle.getSecurity(), null, 0, false);
			}
		} else {
			// always check for stop loss hit before exiting trade and update
			// reason in db
			if (takeProfitReached(candle, openTrade)) {
				tradeCall = new StrategyModel(openTrade.getPosition(), (double) (openTrade.getSl() / 2),
						candle.getClose(), openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(),
						true);
			}
			if (stopLossReached(candle, openTrade)) {
				tradeCall = new StrategyModel(openTrade.getPosition(), (double) (openTrade.getSl() / 2),
						candle.getClose(), openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(),
						true);
			}
		}
		return tradeCall;

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
		dayTradeDone = false;
	}


	@Override
	public void destroySetup() {
		candle15Set = null;
		first15minCandle = null;
		dayTradeDone = false;
		
		/*candle5Set.clear();
		candle30Set.clear();*/
		 
	}

	@Override
	public void updateSetup(Candle candle) {
		System.out.println("updateSetup date :: " + candle.getTime());
	}

}

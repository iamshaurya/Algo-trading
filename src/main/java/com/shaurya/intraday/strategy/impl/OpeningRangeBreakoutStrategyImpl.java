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
			candle15Set.add(candle);
			Candle candle15min = form15MinCandle();
			if (candle15min != null) {
				updateSetup(candle15min);
				first15minCandle = first15minCandle == null ? candle15min : first15minCandle;
				return getTradeCall(candle15min, openTrade);
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
				double longSl = Math.min((candle.getClose() - first15minCandle.getLow()), (0.015 * candle.getClose()));
				tradeCall = new StrategyModel(candle.getToken(), PositionType.LONG, (0.5 * longSl), candle.getClose(),
						candle.getSecurity(), null, 0, false);
			}
			if (!dayTradeDone && isTradeableRange() && candle.getClose() < first15minCandle.getLow()) {
				dayTradeDone = true;
				double shortSl = Math.min((first15minCandle.getHigh() - candle.getClose()),
						(0.015 * candle.getClose()));
				tradeCall = new StrategyModel(candle.getToken(), PositionType.SHORT, (0.5 * shortSl), candle.getClose(),
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
		dayTradeDone = false;
	}

	@Override
	public void destroySetup() {
		/*candle15Set = null;*/
		first15minCandle = null;
		dayTradeDone = false;

		
		candle15Set.clear();
		 

	}

	@Override
	public void updateSetup(Candle candle) {
		System.out.println("updateSetup date :: " + candle.getTime());
	}

}

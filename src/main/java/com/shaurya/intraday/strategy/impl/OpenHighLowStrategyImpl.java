/**
 * 
 */
package com.shaurya.intraday.strategy.impl;

import static com.shaurya.intraday.util.HelperUtil.stopLossReached;
import static com.shaurya.intraday.util.HelperUtil.takeProfitReached;

import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.strategy.OpenHighLowStrategy;
import com.shaurya.intraday.util.CandlestickPatternHelper;

/**
 * @author Shaurya
 *
 */
public class OpenHighLowStrategyImpl implements OpenHighLowStrategy {
	// Modified macd of 12,26,9
	private TreeSet<Candle> candleSet;
	private Candle current30Candle;
	private boolean dayTradeDone;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.shaurya.intraday.strategy.Strategy#processTrades(java.util.List)
	 */

	@Override
	public StrategyModel processTrades(Candle candle, StrategyModel openTrade, boolean updateSetup) {
		if (updateSetup) {
			/*
			 * if(current30Candle == null){ candleSet.add(candle);
			 * form30MinCandle(); }
			 */
			candleSet.add(candle);
			Candle candle5min = form30MinCandle();
			if (candle5min != null) {
				return getTradeCall(candle5min, openTrade);
			}
		}
		return null;
	}

	private Candle form30MinCandle() {
		if (candleSet.size() == 5) {
			int i = 0;
			Iterator<Candle> cItr = candleSet.iterator();
			while (cItr.hasNext()) {
				Candle c = cItr.next();
				if (i == 0) {
					current30Candle = new Candle(c.getSecurity(), c.getToken(), c.getTime(), c.getOpen(), c.getHigh(),
							c.getLow(), c.getClose(), 0);
				} else {
					current30Candle.setClose(c.getClose());
					current30Candle.setHigh(Math.max(current30Candle.getHigh(), c.getHigh()));
					current30Candle.setLow(Math.min(current30Candle.getLow(), c.getLow()));
				}
				i++;
			}
			candleSet.clear();
		}
		return current30Candle;
	}

	private StrategyModel getTradeCall(Candle candle, StrategyModel openTrade) {
		StrategyModel tradeCall = null;
		if (openTrade == null) {
			if (CandlestickPatternHelper.bullishMarubozu(candle) && (candle.getClose() >= (1.003 * candle.getLow()))) {
				dayTradeDone = true;
				tradeCall = new StrategyModel(candle.getToken(), PositionType.LONG, (0.0015 * candle.getClose()),
						candle.getClose(), candle.getSecurity(), null, 0, false);
			}
			if (CandlestickPatternHelper.bearishMarubozu(candle) && (candle.getClose() <= (0.997 * candle.getHigh()))) {
				dayTradeDone = true;
				tradeCall = new StrategyModel(candle.getToken(), PositionType.SHORT, (0.0015 * candle.getClose()),
						candle.getClose(), candle.getSecurity(), null, 0, false);
			}
		} else {
			// always check for stop loss hit before exiting trade and update
			// reason in db
			if (targetProfitReached(candle, openTrade)) {
				tradeCall = new StrategyModel(candle.getToken(), openTrade.getPosition(), openTrade.getAtr(),
						candle.getClose(), openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(),
						true);
			}
			if (stopLossReached(candle, openTrade)) {
				tradeCall = new StrategyModel(candle.getToken(), openTrade.getPosition(), openTrade.getAtr(),
						candle.getClose(), openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(),
						true);
			}
		}
		return tradeCall;

	}

	private boolean targetProfitReached(Candle candle, StrategyModel openTrade) {
		boolean targetReached = false;
		switch (openTrade.getPosition()) {
		case LONG:
			targetReached = candle.getClose() >= (1.003 * openTrade.getTradePrice());
			break;
		case SHORT:
			targetReached = candle.getClose() <= (0.997 * openTrade.getTradePrice());
			break;
		default:
			break;
		}
		return targetReached;
	}

	@Override
	public void initializeSetup(List<Candle> cList) {
		candleSet = new TreeSet<>();
		dayTradeDone = false;
	}

	@Override
	public void destroySetup() {
		/*
		 * candleSet = null; current30Candle = null;
		 */
		dayTradeDone = false;
		current30Candle = null;
		candleSet.clear();
	}

	@Override
	public void updateSetup(Candle candle) {

	}

}

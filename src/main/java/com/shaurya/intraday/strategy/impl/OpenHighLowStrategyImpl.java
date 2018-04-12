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
			if(current30Candle == null){
				candleSet.add(candle);
				form30MinCandle();
			}
			if (current30Candle != null) {
				return getTradeCall(candle, openTrade);
			}
		}
		return null;
	}

	private Candle form30MinCandle() {
		if (candleSet.size() == 30) {
			int i = 0;
			Iterator<Candle> cItr = candleSet.iterator();
			while (cItr.hasNext()) {
				Candle c = cItr.next();
				if (i == 0) {
					current30Candle = new Candle(c.getSecurity(), c.getTime(), c.getOpen(), c.getHigh(), c.getLow(),
							c.getClose(), 0);
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
			if (!dayTradeDone && CandlestickPatternHelper.bullishMarubozu(current30Candle)) {
				dayTradeDone = true;
				tradeCall = new StrategyModel(PositionType.LONG, (0.0015 * candle.getClose()), candle.getClose(),
						candle.getSecurity(), null, 0, false);
			}
			if (!dayTradeDone && CandlestickPatternHelper.bearishMarubozu(current30Candle)) {
				dayTradeDone = true;
				tradeCall = new StrategyModel(PositionType.SHORT, (0.0015 * candle.getClose()), candle.getClose(),
						candle.getSecurity(), null, 0, false);
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


	@Override
	public void initializeSetup(List<Candle> cList) {
		candleSet = new TreeSet<>();
		dayTradeDone = false;
	}


	@Override
	public void destroySetup() {
		candleSet = null;
		current30Candle = null;
		dayTradeDone = false;
	}

	@Override
	public void updateSetup(Candle candle) {
		
	}

}

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
import com.shaurya.intraday.util.CandlestickPatternHelper;

/**
 * @author Shaurya
 *
 */
public class EMAandRSIStrategyImpl implements EMAandRSIStrategy {
	// Taking 20-EMA and 50-EMA
	private ATRModel atr;
	private TreeMap<Date, IndicatorValue> fastEmaMap;
	private TreeMap<Date, IndicatorValue> slowEmaMap;
	private TreeMap<Date, IndicatorValue> ema9Map;
	private TreeMap<Date, IndicatorValue> ema21Map;
	private TreeMap<Date, IndicatorValue> ema90Map;
	private RSIModel rsi;
	private TreeSet<Candle> candle5Set;
	private Candle prevCandle;

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
		if (candle5Set.size() == 15) {
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

	private StrategyModel getTradeCall(Candle candle, StrategyModel openTrade) {
		StrategyModel tradeCall = null;
		double rsiValue = rsi.getRsiMap().lastEntry().getValue().getIndicatorValue();
		double atrValue = atr.getAtrMap().lastEntry().getValue().getIndicatorValue();
		if (openTrade == null) {
			if (bullishBreakout(prevCandle, candle) && highVolatility()) {
				tradeCall = new StrategyModel(candle.getToken(),PositionType.LONG, (0.0025 * candle.getClose()), candle.getClose(),
						candle.getSecurity(), null, 0, false);
			}
			if (bearishBreakout(prevCandle, candle) && highVolatility()) {
				tradeCall = new StrategyModel(candle.getToken(),PositionType.SHORT, (0.0025 * candle.getClose()), candle.getClose(),
						candle.getSecurity(), null, 0, false);
			}
		} else {
			// always check for stop loss hit before exiting trade and update
			// reason in db
			if (takeProfitReached(candle, openTrade)) {
				tradeCall = new StrategyModel(candle.getToken(),openTrade.getPosition(), openTrade.getAtr(), candle.getClose(),
						openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(), true);
			}
			if (stopLossReached(candle, openTrade)) {
				tradeCall = new StrategyModel(candle.getToken(),openTrade.getPosition(), openTrade.getAtr(), candle.getClose(),
						openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(), true);
			}
			if (openTrade.getPosition() == PositionType.LONG && (bearishBreakout(prevCandle, candle))) {
				tradeCall = new StrategyModel(candle.getToken(),openTrade.getPosition(), openTrade.getAtr(), candle.getClose(),
						openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(), true);
			}
			if (openTrade.getPosition() == PositionType.SHORT && (bullishBreakout(prevCandle, candle))) {
				tradeCall = new StrategyModel(candle.getToken(),openTrade.getPosition(), openTrade.getAtr(), candle.getClose(),
						openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(), true);
			}
		}
		prevCandle = candle;
		return tradeCall;
	}

	private boolean highVolatility() {
		Date currentTime = getNthLastKeyEntry(atr.getAtrMap(), 1);
		double atrValue = atr.getAtrMap().get(currentTime).getIndicatorValue();
		double atrSignalValue = atr.getAtrSignal().get(currentTime).getIndicatorValue();
		return atrValue > atrSignalValue;
	}

	private boolean bearishEntry(Candle candle) {
		Date currentTime = getNthLastKeyEntry(ema9Map, 1);
		return (CandlestickPatternHelper.redCandle(candle))
				&& (candle.getClose() < ema9Map.get(currentTime).getIndicatorValue())
				&& (ema9Map.get(currentTime).getIndicatorValue() < ema21Map.get(currentTime).getIndicatorValue())
				&& (ema21Map.get(currentTime).getIndicatorValue() < ema90Map.get(currentTime).getIndicatorValue());
	}

	private boolean bearishExit(Candle candle) {
		Date currentTime = getNthLastKeyEntry(fastEmaMap, 1);
		return (candle.getClose() >= ema21Map.get(currentTime).getIndicatorValue());
	}

	private boolean bullishEntry(Candle candle) {
		Date currentTime = getNthLastKeyEntry(ema9Map, 1);
		return (CandlestickPatternHelper.greenCandle(candle))
				&& (candle.getClose() > ema9Map.get(currentTime).getIndicatorValue())
				&& (ema9Map.get(currentTime).getIndicatorValue() > ema21Map.get(currentTime).getIndicatorValue())
				&& (ema21Map.get(currentTime).getIndicatorValue() > ema90Map.get(currentTime).getIndicatorValue());
	}

	private boolean bullishExit(Candle candle) {
		Date currentTime = getNthLastKeyEntry(fastEmaMap, 1);
		return (candle.getClose() <= ema21Map.get(currentTime).getIndicatorValue());
	}

	private boolean bullishBreakout(Candle prevCandle, Candle candle) {
		boolean breakout = false;
		Date currentTime = getNthLastKeyEntry(ema21Map, 1);
		double breakoutLevel = ema21Map.get(currentTime).getIndicatorValue();
		if (prevCandle != null) {
			breakout = ((prevCandle.getClose() < breakoutLevel) && (candle.getClose() > breakoutLevel))
					|| ((candle.getOpen() < breakoutLevel) && (candle.getClose() > breakoutLevel));
		}
		return breakout && CandlestickPatternHelper.greenCandle(candle);
	}

	private boolean bearishBreakout(Candle prevCandle, Candle candle) {
		boolean breakout = false;
		Date currentTime = getNthLastKeyEntry(ema21Map, 1);
		double breakoutLevel = ema21Map.get(currentTime).getIndicatorValue();
		if (prevCandle != null) {
			breakout = ((prevCandle.getClose() > breakoutLevel) && (candle.getClose() < breakoutLevel))
					|| ((candle.getOpen() > breakoutLevel) && (candle.getClose() < breakoutLevel));
		}
		return breakout && CandlestickPatternHelper.redCandle(candle);
	}

	@Override
	public void initializeSetup(List<Candle> cList) {
		candle5Set = new TreeSet<>();
		atr = ATR.calculateATR(cList, 14);
		rsi = RSI.calculateRSI(cList);
		fastEmaMap = EMA.calculateEMA(20, cList);
		slowEmaMap = EMA.calculateEMA(50, cList);
		ema9Map = EMA.calculateEMA(9, cList);
		ema21Map = EMA.calculateEMA(21, cList);
		ema90Map = EMA.calculateEMA(90, cList);

		sendInitSetupDataMail();
	}

	private void sendInitSetupDataMail() {
		IndicatorValue atrIv = atr.getAtrMap().lastEntry().getValue();
		IndicatorValue rsiIv = rsi.getRsiMap().lastEntry().getValue();
		IndicatorValue fastEma = fastEmaMap.lastEntry().getValue();
		IndicatorValue slowEma = slowEmaMap.lastEntry().getValue();
		String mailbody = "ATR : " + atrIv.toString() + "\n" + "RSI : " + rsiIv.toString() + "\n" + "fast ema : "
				+ fastEma.toString() + "\n" + "slow ema : " + slowEma.toString();
		// MailSender.sendMail(Constants.TO_MAIL, Constants.TO_NAME,
		// Constants.EMA_RSI_STRATEGY_SETUP_DATA, mailbody);
	}

	@Override
	public void updateSetup(Candle candle) {
		double newfastEma = EMA.calculateEMA(20, candle, fastEmaMap.lastEntry().getValue().getIndicatorValue());
		double newSlowEma = EMA.calculateEMA(50, candle, slowEmaMap.lastEntry().getValue().getIndicatorValue());
		double new9Ema = EMA.calculateEMA(9, candle, ema9Map.lastEntry().getValue().getIndicatorValue());
		double new21Ema = EMA.calculateEMA(21, candle, ema21Map.lastEntry().getValue().getIndicatorValue());
		double new90Ema = EMA.calculateEMA(90, candle, ema90Map.lastEntry().getValue().getIndicatorValue());
		fastEmaMap.put(candle.getTime(), new IndicatorValue(candle.getTime(), newfastEma, IndicatorType.EMA));
		slowEmaMap.put(candle.getTime(), new IndicatorValue(candle.getTime(), newSlowEma, IndicatorType.EMA));
		ema9Map.put(candle.getTime(), new IndicatorValue(candle.getTime(), new9Ema, IndicatorType.EMA));
		ema21Map.put(candle.getTime(), new IndicatorValue(candle.getTime(), new21Ema, IndicatorType.EMA));
		ema90Map.put(candle.getTime(), new IndicatorValue(candle.getTime(), new90Ema, IndicatorType.EMA));
		RSI.updateRSI(candle, rsi);
		ATR.updateATR(candle, atr, 14);
	}

	@Override
	public void destroySetup() {
		/*
		 * atr = null; rsi = null; fastEmaMap = null; slowEmaMap = null;
		 */
		candle5Set.clear();
		prevCandle = null;
	}

}

/**
 * 
 */
package com.shaurya.intraday.strategy.impl;

import static com.shaurya.intraday.util.HelperUtil.getNthLastKeyEntry;
import static com.shaurya.intraday.util.HelperUtil.stopLossReached;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import com.shaurya.intraday.enums.IndicatorType;
import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.indicator.ATR;
import com.shaurya.intraday.indicator.EMA;
import com.shaurya.intraday.indicator.GannSquare9;
import com.shaurya.intraday.indicator.MACD;
import com.shaurya.intraday.indicator.RSI;
import com.shaurya.intraday.indicator.SMA;
import com.shaurya.intraday.model.ATRModel;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.IndicatorValue;
import com.shaurya.intraday.model.Level;
import com.shaurya.intraday.model.MACDModel;
import com.shaurya.intraday.model.RSIModel;
import com.shaurya.intraday.model.SMAModel;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.strategy.GannSquare9Strategy;

/**
 * @author Shaurya
 *
 */
public class GannSquare9StrategyImpl implements GannSquare9Strategy {
	// Taking 9-EMA and 21-EMA
	private ATRModel atr;
	private TreeMap<Date, IndicatorValue> ema5Map;
	private TreeMap<Date, IndicatorValue> ema8Map;
	private TreeMap<Date, IndicatorValue> ema12Map;
	private TreeMap<Date, IndicatorValue> ema26Map;
	private TreeMap<Date, IndicatorValue> ema13Map;
	private SMAModel sma90;
	private RSIModel rsi;
	private MACDModel macd;
	private TreeSet<Candle> candle5Set;
	private List<Level> levels;
	private Date nextLevelsCalTime;
	private boolean slotTradeDone;
	private boolean isOpenTrade;
	private Candle prevCandle;

	@Override
	public StrategyModel processTrades(Candle candle, StrategyModel openTrade, boolean updateSetup) {
		if (updateSetup) {
			candle5Set.add(candle);
			Candle candle5min = form5MinCandle();
			if (candle5min != null) {
				updateSetup(candle5min);
				return getTradeCall(candle, openTrade);
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
		if (openTrade == null && levels != null) {
			if (prevCandle != null && maUptrend(candle) && bullishBreakout(candle)) {
				isOpenTrade = true;
				tradeCall = new StrategyModel(PositionType.LONG, (0.0025 * (candle.getClose())), candle.getClose(),
						candle.getSecurity(), null, 0, false);
			}
			if (prevCandle != null && maDowntrend(candle) && bearishBreakout(candle)) {
				isOpenTrade = true;
				tradeCall = new StrategyModel(PositionType.SHORT, (0.0025 * (candle.getClose())), candle.getClose(),
						candle.getSecurity(), null, 0, false);
			}
		} else if (openTrade != null) {
			// always check for stop loss hit before exiting trade and update
			// reason in db
			if (takeTargetReached(candle, openTrade)) {
				// openTrade.setTrailSl(true);
				//openTrade.trailSl(0.9995 * getTrailingStoplossValue(candle, openTrade));
			}
			if (stopLossReached(candle, openTrade)) {
				// slotTradeDone = true;
				resetAcheivedFlags();
				isOpenTrade = false;
				tradeCall = new StrategyModel(openTrade.getPosition(), openTrade.getAtr(), candle.getClose(),
						openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(), true);
			}
			
			if (openTrade.getPosition() == PositionType.LONG && (!maUptrend(candle))) {
				tradeCall = new StrategyModel(openTrade.getPosition(), openTrade.getAtr(), candle.getClose(),
						openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(), true);
			}
			if (openTrade.getPosition() == PositionType.SHORT && (!maDowntrend(candle))) {
				tradeCall = new StrategyModel(openTrade.getPosition(), openTrade.getAtr(), candle.getClose(),
						openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(), true);
			}

		}
		prevCandle = candle;
		return tradeCall;
	}
	
	private Level getSupportResistanceLevel(PositionType positionType){
		List<Level> auxList = new ArrayList<>(levels);
		Level tradePriceLevel = new Level(prevCandle.getClose(), false);
		auxList.add(tradePriceLevel);
		Collections.sort(auxList);
		int tradePriceIndex = auxList.indexOf(tradePriceLevel);
		List<Level> resistanceLevels = auxList.subList(tradePriceIndex+1, auxList.size()-1);
		Collections.sort(resistanceLevels);
		List<Level> supportLevels = auxList.subList(0, tradePriceIndex-1);
		Collections.sort(supportLevels);
		Collections.reverse(supportLevels);
		switch (positionType) {
		case LONG:
			return supportLevels.get(0);
		case SHORT:
			return resistanceLevels.get(0);
		default:
			break;
		}
		return null;
	}
	
	private boolean bullishBreakout(Candle candle) {
		boolean breakout = false;
		Level currPriceLevel = new Level(candle.getClose(), false);
		List<Level> auxList = new ArrayList<>(levels);
		auxList.add(currPriceLevel);
		Collections.sort(auxList);
		int index = auxList.indexOf(currPriceLevel);
		if (index > 0 && index < (auxList.size()-1)) {
			double supportVal = auxList.get(index - 1).getValue();
			double resistanceVal = auxList.get(index + 1).getValue();
			breakout = (resistanceVal - candle.getClose()) > (candle.getClose() - supportVal) ;
		}
		return breakout;
	}
	
	private boolean bearishBreakout(Candle candle) {
		boolean breakout = false;
		Level currPriceLevel = new Level(candle.getClose(), false);
		List<Level> auxList = new ArrayList<>(levels);
		auxList.add(currPriceLevel);
		Collections.sort(auxList);
		int index = auxList.indexOf(currPriceLevel);
		if (index > 0 && index < (auxList.size()-1)) {
			double supportVal = auxList.get(index - 1).getValue();
			double resistanceVal = auxList.get(index + 1).getValue();
			breakout = (candle.getClose() - supportVal) > (resistanceVal - candle.getClose());
		}
		return breakout;
	}
	
	private void resetAcheivedFlags() {
		for(Level l: levels){
			l.setAcheived(false);
		}
		
	}
	
	private boolean maUptrendExit(Candle candle) {
		Date currentTime = getNthLastKeyEntry(ema8Map, 1);
		return (candle.getClose() < ema8Map.get(currentTime).getIndicatorValue())
				|| (ema5Map.get(currentTime).getIndicatorValue() < ema8Map.get(currentTime).getIndicatorValue());
	}

	private boolean maDowntrendExit(Candle candle) {
		Date currentTime = getNthLastKeyEntry(ema8Map, 1);
		return (candle.getClose() > ema8Map.get(currentTime).getIndicatorValue())
				|| (ema5Map.get(currentTime).getIndicatorValue() > ema8Map.get(currentTime).getIndicatorValue());
	}
	
	private boolean isTradableRange(Candle candle) {
		double stockPrice = candle.getClose();
		double range = atr.getAtrMap().lastEntry().getValue().getIndicatorValue();
		return (range >= (0.004 * stockPrice)) && (range <= (0.02 * stockPrice));
	}

	private boolean takeTargetReached(Candle candle, StrategyModel openTrade) {
		boolean targetLevelReached = false;
		List<Level> auxList = new ArrayList<>(levels);
		Level tradePriceLevel = new Level(openTrade.getTradePrice(), false);
		auxList.add(tradePriceLevel);
		Collections.sort(auxList);
		int tradePriceIndex = auxList.indexOf(tradePriceLevel);
		List<Level> resistanceLevels = auxList.subList(tradePriceIndex+1, auxList.size()-1);
		Collections.sort(resistanceLevels);
		List<Level> supportLevels = auxList.subList(0, tradePriceIndex-1);
		Collections.sort(supportLevels);
		Collections.reverse(supportLevels);
		if (levels != null) {
			switch (openTrade.getPosition()) {
			case LONG:
				for (Level l : resistanceLevels) {
					if ((openTrade.getTradePrice() < l.getValue()) && !l.isAcheived()
							&& (candle.getClose() > (0.9995 * l.getValue()))) {
						targetLevelReached = true;
					}
				}
				break;
			case SHORT:
				for (Level l : supportLevels) {
					if ((openTrade.getTradePrice() > l.getValue()) && !l.isAcheived()
							&& (candle.getClose() < (1.0005 * l.getValue()))) {
						targetLevelReached = true;
					}
				}
				break;
			default:
				break;
			}
		}
		return targetLevelReached;
	}
	
	private double getTrailingStoplossValue(Candle candle, StrategyModel openTrade){
		double trailingSl = 0;
		double prevLevel = 0;
		List<Level> auxList = new ArrayList<>(levels);
		Level tradePriceLevel = new Level(openTrade.getTradePrice(), false);
		auxList.add(tradePriceLevel);
		Collections.sort(auxList);
		int tradePriceIndex = auxList.indexOf(tradePriceLevel);
		List<Level> resistanceLevels = auxList.subList(tradePriceIndex+1, auxList.size()-1);
		Collections.sort(resistanceLevels);
		List<Level> supportLevels = auxList.subList(0, tradePriceIndex-1);
		Collections.sort(supportLevels);
		Collections.reverse(supportLevels);
		if (levels != null) {
			switch (openTrade.getPosition()) {
			case LONG:
				for (Level l : resistanceLevels) {
					prevLevel = openTrade.getTradePrice();
					int currentIndex = resistanceLevels.indexOf(l);
					if ((openTrade.getTradePrice() < l.getValue()) && !l.isAcheived()
							&& (candle.getClose() > (0.9995 * l.getValue())) && currentIndex > 0) {
						l.setAcheived(true);
						prevLevel = resistanceLevels.get(currentIndex - 1).getValue();
					}
				}
				trailingSl = prevLevel - (openTrade.getTradePrice() - openTrade.getSl());
				break;
			case SHORT:
				for (Level l : supportLevels) {
					prevLevel = openTrade.getTradePrice();
					int currentIndex = resistanceLevels.indexOf(l);
					if ((openTrade.getTradePrice() > l.getValue()) && !l.isAcheived()
							&& (candle.getClose() < (1.0005 * l.getValue())) && currentIndex > 0) {
						l.setAcheived(true);
						prevLevel = supportLevels.get(currentIndex-1).getValue();
					}
				}
				trailingSl =  (openTrade.getTradePrice() + openTrade.getSl()) - prevLevel;
				break;
			default:
				break;
			}
		}
		return trailingSl;
	
	}

	private boolean maUptrend(Candle candle) {
		Date currentTime = getNthLastKeyEntry(ema8Map, 1);
		return (ema13Map.get(currentTime).getIndicatorValue() < ema8Map.get(currentTime).getIndicatorValue())
				&& (ema8Map.get(currentTime).getIndicatorValue() < ema5Map.get(currentTime).getIndicatorValue());
	}

	private boolean maDowntrend(Candle candle) {
		Date currentTime = getNthLastKeyEntry(ema8Map, 1);
		return (ema13Map.get(currentTime).getIndicatorValue() > ema8Map.get(currentTime).getIndicatorValue())
				&& (ema8Map.get(currentTime).getIndicatorValue() > ema5Map.get(currentTime).getIndicatorValue());
	}
	
	private boolean entryBullishMacd() {
		Date currentTime = getNthLastKeyEntry(macd.getMacdMap(), 1);
		Map<Date, IndicatorValue> macdMap = macd.getMacdMap();
		Map<Date, IndicatorValue> signalMap = macd.getSignalMap();
		return (macdMap.get(currentTime).getIndicatorValue() > signalMap.get(currentTime).getIndicatorValue());
	}
	
	private boolean entryBearishMacd() {
		Date currentTime = getNthLastKeyEntry(macd.getMacdMap(), 1);
		Map<Date, IndicatorValue> macdMap = macd.getMacdMap();
		Map<Date, IndicatorValue> signalMap = macd.getSignalMap();
		return (macdMap.get(currentTime).getIndicatorValue() < signalMap.get(currentTime).getIndicatorValue());
	}

	@Override
	public void initializeSetup(List<Candle> cList) {
		candle5Set = new TreeSet<>();
		atr = ATR.calculateATR(cList);
		rsi = RSI.calculateRSI(cList);
		ema5Map = EMA.calculateEMA(5, cList);
		ema8Map = EMA.calculateEMA(8, cList);
		ema12Map = EMA.calculateEMA(12, cList);
		ema26Map = EMA.calculateEMA(26, cList);
		ema13Map = EMA.calculateEMA(13, cList);
		macd = MACD.calculateMACD(ema12Map, ema26Map, 9);
		sma90 = SMA.calculateSMA(90, cList);
		//slotTradeDone = false;

		sendInitSetupDataMail();
	}

	private void sendInitSetupDataMail() {
		IndicatorValue atrIv = atr.getAtrMap().lastEntry().getValue();
		IndicatorValue rsiIv = rsi.getRsiMap().lastEntry().getValue();
		IndicatorValue fastEma = ema5Map.lastEntry().getValue();
		IndicatorValue slowEma = ema8Map.lastEntry().getValue();
		String mailbody = "ATR : " + atrIv.toString() + "\n" + "RSI : " + rsiIv.toString() + "\n" + "fast ema : "
				+ fastEma.toString() + "\n" + "slow ema : " + slowEma.toString();
		System.out.println(mailbody);
	}

	@Override
	public void updateSetup(Candle candle) {
		double new5Ema = EMA.calculateEMA(5, candle, ema5Map.lastEntry().getValue().getIndicatorValue());
		double new8Ema = EMA.calculateEMA(8, candle, ema8Map.lastEntry().getValue().getIndicatorValue());
		double newfast12Ema = EMA.calculateEMA(12, candle, ema12Map.lastEntry().getValue().getIndicatorValue());
		double newSlow26Ema = EMA.calculateEMA(26, candle, ema26Map.lastEntry().getValue().getIndicatorValue());
		double new13Ema = EMA.calculateEMA(13, candle, ema13Map.lastEntry().getValue().getIndicatorValue());
		ema5Map.put(candle.getTime(), new IndicatorValue(candle.getTime(), new5Ema, IndicatorType.EMA));
		ema8Map.put(candle.getTime(), new IndicatorValue(candle.getTime(), new8Ema, IndicatorType.EMA));
		ema12Map.put(candle.getTime(), new IndicatorValue(candle.getTime(), newfast12Ema, IndicatorType.EMA));
		ema26Map.put(candle.getTime(), new IndicatorValue(candle.getTime(), newSlow26Ema, IndicatorType.EMA));
		ema13Map.put(candle.getTime(), new IndicatorValue(candle.getTime(), new13Ema, IndicatorType.EMA));
		RSI.updateRSI(candle, rsi);
		ATR.updateATR(candle, atr);
		MACD.updateMacdModel(this.macd, candle, newfast12Ema, newSlow26Ema, 9);
		SMA.updateSMA(90, candle, this.sma90);
		if (nextLevelsCalTime == null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(candle.getTime());
			cal.set(Calendar.HOUR_OF_DAY, 9);
			cal.set(Calendar.MINUTE, 30);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			nextLevelsCalTime = cal.getTime();
		}

		if (candle.getTime().getTime() == nextLevelsCalTime.getTime()) {
			levels = GannSquare9.getLevels(candle.getClose());
		}
		
	}

	@Override
	public void destroySetup() {
		candle5Set = null;
		atr = null;
		rsi = null;
		macd = null;
		ema5Map = null;
		ema8Map = null;
		ema13Map = null;
		ema12Map = null;
		ema26Map = null;
		sma90 = null;
		levels = null;
		nextLevelsCalTime = null;
		slotTradeDone = false;
		isOpenTrade = false;
		//candle5Set.clear();
		
	}

}

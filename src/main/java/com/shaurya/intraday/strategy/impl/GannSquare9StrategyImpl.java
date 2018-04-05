/**
 * 
 */
package com.shaurya.intraday.strategy.impl;

import static com.shaurya.intraday.util.HelperUtil.stopLossReached;
import static com.shaurya.intraday.util.HelperUtil.takeProfitReached;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import com.shaurya.intraday.enums.CandlestickPatternsType;
import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.indicator.GannSquare9;
import com.shaurya.intraday.indicator.PivotPoints;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.Level;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.strategy.GannSquare9Strategy;
import com.shaurya.intraday.util.CandlestickPatternHelper;

/**
 * @author Shaurya
 *
 */
public class GannSquare9StrategyImpl implements GannSquare9Strategy {
	// Taking 9-EMA and 21-EMA
	private TreeSet<Candle> candle15Set;
	private TreeSet<Candle> prevDayCandleSet;
	private List<Level> levels;
	private Date nextLevelsCalTime;
	private Candle prevCandle;
	private Candle prevDayCandle;

	@Override
	public StrategyModel processTrades(Candle candle, StrategyModel openTrade, boolean updateSetup) {
		if (updateSetup) {
			candle15Set.add(candle);
			Candle candle15min = form15MinCandle();
			if (candle15min != null) {
				prevDayCandleSet.add(candle15min);
				formPrevDayCandle();
				updateSetup(candle15min);
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
					candle15min = new Candle(c.getSecurity(), c.getTime(), c.getOpen(), c.getHigh(), c.getLow(),
							c.getClose(), c.getVolume());
				} else {
					candle15min.setClose(c.getClose());
					candle15min.setHigh(Math.max(candle15min.getHigh(), c.getHigh()));
					candle15min.setLow(Math.min(candle15min.getLow(), c.getLow()));
					candle15min.setVolume(candle15min.getVolume() + c.getVolume());
				}
				i++;
			}
			candle15Set.clear();
		}
		return candle15min;
	}

	private void formPrevDayCandle() {
		if (prevDayCandleSet.size() == 15) {
			int i = 0;
			Iterator<Candle> cItr = prevDayCandleSet.iterator();
			while (cItr.hasNext()) {
				Candle c = cItr.next();
				if (i == 0) {
					prevDayCandle = new Candle(c.getSecurity(), c.getTime(), c.getOpen(), c.getHigh(), c.getLow(),
							c.getClose(), c.getVolume());
				} else {
					prevDayCandle.setClose(c.getClose());
					prevDayCandle.setHigh(Math.max(prevDayCandle.getHigh(), c.getHigh()));
					prevDayCandle.setLow(Math.min(prevDayCandle.getLow(), c.getLow()));
					prevDayCandle.setVolume(prevDayCandle.getVolume() + c.getVolume());
				}
				i++;
			}
			prevDayCandleSet.clear();
		}
	}

	private StrategyModel getTradeCall(Candle candle, StrategyModel openTrade) {
		StrategyModel tradeCall = null;
		if (openTrade == null && levels != null) {
			if (reversalNearSupport(candle) || bullishBreakout(candle)) {
				tradeCall = new StrategyModel(PositionType.LONG, (0.0015 * candle.getClose()), candle.getClose(),
						candle.getSecurity(), null, 0, false);
			}
			if (reversalNearResistance(candle) || bearishBreakout(candle)) {
				tradeCall = new StrategyModel(PositionType.SHORT, (0.0015 * candle.getClose()), candle.getClose(),
						candle.getSecurity(), null, 0, false);
			}
		} else if (openTrade != null) {
			// always check for stop loss hit before exiting trade and update
			// reason in db
			if (takeProfitReached(candle, openTrade)) {
				tradeCall = new StrategyModel(openTrade.getPosition(), openTrade.getAtr(), candle.getClose(),
						openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(), true);
			}
			if (stopLossReached(candle, openTrade)) {
				resetAchievedFlag();
				tradeCall = new StrategyModel(openTrade.getPosition(), (double) (openTrade.getSl() / 2),
						candle.getClose(), openTrade.getSecurity(), openTrade.getOrderId(), openTrade.getQuantity(),
						true);

			}
		}
		prevCandle = candle;
		return tradeCall;
	}

	private boolean reversalNearResistance(Candle candle) {
		boolean reversal = false;
		double resistance = getSupportResistanceLevel(PositionType.SHORT, candle);
		for (CandlestickPatternsType pattern : CandlestickPatternsType.values()) {
			switch (pattern) {
			case MARUBOZU:
				if (CandlestickPatternHelper.bearishMarubozu(candle)) {
					reversal = true;
				}
				break;
			case DOJI_REVERSAL:
				if (CandlestickPatternHelper.dojiBearishReversal(prevCandle, candle)
						&& ((prevCandle.getClose() < (1.003 * resistance))
								&& (candle.getClose() < (0.999 * resistance)))) {
					reversal = true;
				}
				break;
			case ENGULFING:
				if (CandlestickPatternHelper.bearishEngulfing(prevCandle, candle)
						&& ((prevCandle.getOpen() < resistance) && (candle.getClose() < (0.999 * resistance)))) {
					reversal = true;
				}
				break;
			case HARAMI:
				if (CandlestickPatternHelper.bearishHarami(prevCandle, candle)
						&& (candle.getClose() < (0.999 * resistance))) {
					reversal = true;
				}
				break;
			case PEIRCING:
				if (CandlestickPatternHelper.bearishPiercing(prevCandle, candle)
						&& (candle.getClose() < (0.999 * resistance))) {
					reversal = true;
				}
				break;

			default:
				break;
			}
		}
		return reversal;
	}

	private boolean reversalNearSupport(Candle candle) {
		boolean reversal = false;
		double support = getSupportResistanceLevel(PositionType.LONG, candle);
		for (CandlestickPatternsType pattern : CandlestickPatternsType.values()) {
			switch (pattern) {
			case MARUBOZU:
				if (CandlestickPatternHelper.bullishMarubozu(candle)) {
					reversal = true;
				}
				break;
			case DOJI_REVERSAL:
				if (CandlestickPatternHelper.dojiBullishReversal(prevCandle, candle)
						&& ((prevCandle.getClose() > (0.997 * support)) && (candle.getClose() > (1.001 * support)))) {
					reversal = true;
				}
				break;
			case ENGULFING:
				if (CandlestickPatternHelper.bullishEngulfing(prevCandle, candle)
						&& ((prevCandle.getOpen() > support) && (candle.getClose() > (1.001 * support)))) {
					reversal = true;
				}
				break;
			case HARAMI:
				if (CandlestickPatternHelper.bullishHarami(prevCandle, candle)
						&& (candle.getClose() > (1.001 * support))) {
					reversal = true;
				}
				break;
			case PEIRCING:
				if (CandlestickPatternHelper.bullishPiercing(prevCandle, candle)
						&& (candle.getClose() > (1.001 * support))) {
					reversal = true;
				}
				break;

			default:
				break;
			}
		}
		return reversal;
	}

	private void resetAchievedFlag() {
		for (Level l : levels) {
			l.setAcheived(false);
		}
	}

	private double getSupportResistanceLevel(PositionType positionType, Candle candle) {
		List<Level> auxList = new ArrayList<>(levels);
		Level ltpLevel = new Level(candle.getClose(), false);
		auxList.add(ltpLevel);
		Collections.sort(auxList);
		int ltpIndex = auxList.indexOf(ltpLevel);
		if (ltpIndex > 0 && ltpIndex < (auxList.size() - 1)) {
			List<Level> resistanceLevels = auxList.subList(ltpIndex + 1, auxList.size());
			Collections.sort(resistanceLevels);
			List<Level> supportLevels = auxList.subList(0, ltpIndex);
			Collections.sort(supportLevels);
			Collections.reverse(supportLevels);
			switch (positionType) {
			case LONG:
				return supportLevels.get(0).getValue();
			case SHORT:
				return resistanceLevels.get(0).getValue();
			default:
				break;
			}
		}

		return positionType == PositionType.LONG ? (0.995 * candle.getClose()) : (1.005 * candle.getClose());
	}

	private boolean bullishBreakout(Candle candle) {
		boolean breakout = false;
		boolean greenCandle = false;
		Level prevPriceLevel = new Level(prevCandle.getClose(), false);
		List<Level> auxList = new ArrayList<>(levels);
		auxList.add(prevPriceLevel);
		Collections.sort(auxList);
		int index = auxList.indexOf(prevPriceLevel);
		if (index > 0 && index < (auxList.size() - 1)) {
			double resistanceVal = auxList.get(index + 1).getValue();
			breakout = prevCandle.getClose() <= resistanceVal && candle.getClose() > (1.001 * resistanceVal);
			greenCandle = candle.getClose() > candle.getOpen();
		}
		return breakout && greenCandle;
	}

	private boolean bearishBreakout(Candle candle) {
		boolean breakout = false;
		boolean redCandle = false;
		Level prevPriceLevel = new Level(prevCandle.getClose(), false);
		List<Level> auxList = new ArrayList<>(levels);
		auxList.add(prevPriceLevel);
		Collections.sort(auxList);
		int index = auxList.indexOf(prevPriceLevel);
		if (index > 0 && index < (auxList.size() - 1)) {
			double supportVal = auxList.get(index - 1).getValue();
			breakout = prevCandle.getClose() >= supportVal && candle.getClose() < (0.999 * supportVal);
			redCandle = candle.getClose() < candle.getOpen();
		}
		return breakout && redCandle;
	}

	@Override
	public void initializeSetup(List<Candle> cList) {
		candle15Set = new TreeSet<>();
		prevDayCandleSet = new TreeSet<>();
		// TODO:
		List<Candle> lastDayCandles = cList.subList(cList.size() - 25, cList.size());
		Collections.sort(lastDayCandles);
		for (int i = 0; i < lastDayCandles.size(); i++) {
			Candle c = lastDayCandles.get(i);
			if (i == 0) {
				prevDayCandle = new Candle(c.getSecurity(), c.getTime(), c.getOpen(), c.getHigh(), c.getLow(),
						c.getClose(), c.getVolume());
			} else {
				prevDayCandle.setClose(c.getClose());
				prevDayCandle.setHigh(Math.max(prevDayCandle.getHigh(), c.getHigh()));
				prevDayCandle.setLow(Math.min(prevDayCandle.getLow(), c.getLow()));
				prevDayCandle.setVolume(prevDayCandle.getVolume() + c.getVolume());
			}
		}

		sendInitSetupDataMail();
	}

	private void sendInitSetupDataMail() {
		String mailbody = "Prev day candle : " + prevDayCandle.toString();
		System.out.println(mailbody);
	}

	@Override
	public void updateSetup(Candle candle) {
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
			levels = PivotPoints.getLevels(prevDayCandle);
			levels.addAll(GannSquare9.getLevels(candle.getClose()));
			Collections.sort(levels);
		}

	}

	@Override
	public void destroySetup() {
		candle15Set = null;
		prevDayCandleSet = null;
		levels = null;
		nextLevelsCalTime = null;
		prevCandle = null;
		/*candle15Set.clear();
		 *prevDayCandleSet.clear();*/

	}

}

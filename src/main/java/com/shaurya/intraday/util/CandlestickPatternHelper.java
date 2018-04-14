/**
 * 
 */
package com.shaurya.intraday.util;

import com.shaurya.intraday.model.Candle;

/**
 * @author Shaurya
 *
 */
public class CandlestickPatternHelper {
	public static boolean twoWhiteSoilder(Candle prevCandle, Candle candle) {
		boolean twoWhiteSoilder = false;
		boolean prevCandleStrong = strongBullish(prevCandle);
		boolean currentCandleStrong = strongBullish(candle);
		if (prevCandleStrong && currentCandleStrong && (candle.getClose() > prevCandle.getClose())) {
			twoWhiteSoilder = true;
		}
		return twoWhiteSoilder;
	}

	public static boolean twoBlackCrows(Candle prevCandle, Candle candle) {
		boolean twoBlackCrows = false;
		boolean prevCandleStrong = strongBearish(prevCandle);
		boolean currentCandleStrong = strongBearish(candle);
		if (prevCandleStrong && currentCandleStrong && (candle.getClose() < prevCandle.getClose())) {
			twoBlackCrows = true;
		}
		return twoBlackCrows;
	}

	public static boolean bullishMarubozu(Candle candle) {
		boolean marubozu = false;
		if (((candle.getOpen() == candle.getLow()) && !dojiOrSpininTop(candle) && tradableRange(candle))
				|| (strongBullish(candle))) {
			marubozu = true;
		}
		return marubozu;
	}
	
	public static boolean strongBullish(Candle candle) {
		double range = Math.abs(candle.getHigh() - candle.getLow());
		double bodySize = Math.abs(candle.getClose() - candle.getOpen());
		return (candle.getOpen() < candle.getClose()) && (bodySize >= (0.7 * range))
				&& (range >= (0.005 * candle.getClose()));
	}
	
	public static boolean strongBearish(Candle candle) {
		double range = Math.abs(candle.getHigh() - candle.getLow());
		double bodySize = Math.abs(candle.getClose() - candle.getOpen());
		return (candle.getOpen() > candle.getClose()) && (bodySize >= (0.7 * range))
				&& (range >= (0.005 * candle.getClose()));

	}

	public static boolean bearishMarubozu(Candle candle) {
		boolean marubozu = false;
		if (((candle.getOpen() == candle.getHigh()) && !dojiOrSpininTop(candle) && tradableRange(candle))
				|| (strongBearish(candle))) {
			marubozu = true;
		}
		return marubozu;
	}

	public static boolean dojiBullishReversal(Candle prevCandle, Candle candle) {
		boolean reversal = false;
		boolean prevCandleDoji = dojiOrSpininTop(prevCandle);
		boolean currentCandleDoji = dojiOrSpininTop(candle);
		if (prevCandleDoji && !currentCandleDoji && greenCandle(candle) && (candle.getClose() > prevCandle.getHigh())
				&& tradableRange(candle)) {
			reversal = true;
		}
		return reversal;
	}

	public static boolean dojiBearishReversal(Candle prevCandle, Candle candle) {
		boolean reversal = false;
		boolean prevCandleDoji = dojiOrSpininTop(prevCandle);
		boolean currentCandleDoji = dojiOrSpininTop(candle);
		if (prevCandleDoji && !currentCandleDoji && redCandle(candle) && (candle.getClose() < prevCandle.getLow())
				&& tradableRange(candle)) {
			reversal = true;
		}
		return reversal;
	}

	public static boolean bullishEngulfing(Candle prevCanle, Candle candle) {
		boolean bullishEngulfing = false;
		boolean currentCandleDoji = dojiOrSpininTop(candle);
		boolean prevCandleBearish = redCandle(prevCanle);
		boolean currentCandleBullish = greenCandle(candle);
		if (!currentCandleDoji && prevCandleBearish && currentCandleBullish
				&& (candle.getOpen() <= prevCanle.getClose()) && (candle.getClose() > prevCanle.getOpen())
				&& tradableRange(candle)) {
			bullishEngulfing = true;
		}
		return bullishEngulfing;
	}

	public static boolean bearishEngulfing(Candle prevCanle, Candle candle) {
		boolean bearishEngulfing = false;
		boolean currentCandleDoji = dojiOrSpininTop(candle);
		boolean prevCandleBullish = greenCandle(prevCanle);
		boolean currentCandleBearish = redCandle(candle);
		if (!currentCandleDoji && prevCandleBullish && currentCandleBearish
				&& (candle.getOpen() >= prevCanle.getClose()) && (candle.getClose() < prevCanle.getOpen())
				&& tradableRange(candle)) {
			bearishEngulfing = true;
		}
		return bearishEngulfing;
	}

	public static boolean bullishPiercing(Candle prevCanle, Candle candle) {
		boolean bullishPiercing = false;
		boolean prevCandleDoji = dojiOrSpininTop(prevCanle);
		boolean currentCandleDoji = dojiOrSpininTop(candle);
		boolean prevCandleBearish = redCandle(prevCanle);
		boolean currentCandleBullish = greenCandle(candle);
		double prevRange = Math.abs(prevCanle.getClose() - prevCanle.getOpen());
		if (!prevCandleDoji && !currentCandleDoji && prevCandleBearish && currentCandleBullish
				&& (candle.getOpen() < prevCanle.getClose()) && (candle.getClose() < prevCanle.getOpen())
				&& (candle.getClose() > (prevCanle.getOpen() - (prevRange / 2))) && tradableRange(candle)) {
			bullishPiercing = true;
		}
		return bullishPiercing;
	}

	public static boolean bearishPiercing(Candle prevCanle, Candle candle) {
		boolean bearishPiercing = false;
		boolean prevCandleDoji = dojiOrSpininTop(prevCanle);
		boolean currentCandleDoji = dojiOrSpininTop(candle);
		boolean prevCandleBullish = greenCandle(prevCanle);
		boolean currentCandleBearish = redCandle(candle);
		double prevRange = Math.abs(prevCanle.getClose() - prevCanle.getOpen());
		if (!prevCandleDoji && !currentCandleDoji && prevCandleBullish && currentCandleBearish
				&& (candle.getOpen() > prevCanle.getClose()) && (candle.getClose() > prevCanle.getClose())
				&& (candle.getClose() < (prevCanle.getClose() + (prevRange / 2))) && tradableRange(candle)) {
			bearishPiercing = true;
		}
		return bearishPiercing;
	}

	public static boolean bullishHarami(Candle prevCanle, Candle candle) {
		boolean bullishHarami = false;
		boolean prevCandleDoji = dojiOrSpininTop(prevCanle);
		boolean currentCandleDoji = dojiOrSpininTop(candle);
		boolean prevCandleBearish = redCandle(prevCanle);
		boolean currentCandleBullish = greenCandle(candle);
		if (!prevCandleDoji && !currentCandleDoji && prevCandleBearish && currentCandleBullish
				&& (candle.getOpen() > prevCanle.getClose()) && (candle.getClose() < prevCanle.getOpen())
				&& tradableRange(candle)) {
			bullishHarami = true;
		}
		return bullishHarami;
	}

	public static boolean bearishHarami(Candle prevCanle, Candle candle) {
		boolean bullishHarami = false;
		boolean prevCandleDoji = dojiOrSpininTop(prevCanle);
		boolean currentCandleDoji = dojiOrSpininTop(candle);
		boolean prevCandleBullish = greenCandle(prevCanle);
		boolean currentCandleBearish = redCandle(candle);
		if (!prevCandleDoji && !currentCandleDoji && prevCandleBullish && currentCandleBearish
				&& (candle.getOpen() < prevCanle.getClose()) && (candle.getClose() > prevCanle.getOpen())
				&& tradableRange(candle)) {
			bullishHarami = true;
		}
		return bullishHarami;
	}

	public static boolean greenCandle(Candle candle) {
		return candle.getClose() > candle.getOpen();
	}

	public static boolean redCandle(Candle candle) {
		return candle.getClose() < candle.getOpen();
	}

	public static boolean dojiOrSpininTop(Candle candle) {
		boolean dojiOrSpinningTop = false;
		double range = Math.abs(candle.getHigh() - candle.getLow());
		double bodySize = Math.abs(candle.getClose() - candle.getOpen());
		if ((bodySize <= (0.25
				* range))/* || (range < 0.004 * candle.getClose()) */) {
			dojiOrSpinningTop = true;
		}
		return dojiOrSpinningTop;
	}

	public static boolean tradableRange(Candle candle) {
		double range = Math.abs(candle.getHigh() - candle.getLow());
		double bodySize = Math.abs(candle.getClose() - candle.getOpen());
		return (bodySize >= (0.6 * range)) && (range >= (0.003 * candle.getClose()));
	}
}

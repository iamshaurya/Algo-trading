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
	public static boolean bullishMarubozu(Candle candle) {
		boolean marubozu = false;
		if((candle.getOpen() == candle.getLow()) && !dojiOrSpininTop(candle)){
			marubozu = true;
		}
		return marubozu;
	}
	
	public static boolean bearishMarubozu(Candle candle){
		boolean marubozu = false;
		if((candle.getOpen() == candle.getHigh()) && !dojiOrSpininTop(candle)){
			marubozu = true;
		}
		return marubozu;
	}
	
	public static boolean dojiBullishReversal(Candle prevCandle, Candle candle) {
		boolean reversal = false;
		boolean prevCandleDoji = dojiOrSpininTop(prevCandle);
		boolean currentCandleDoji = dojiOrSpininTop(candle);
		if (prevCandleDoji && !currentCandleDoji && greenCandle(candle) && (candle.getClose() > prevCandle.getHigh())) {
			reversal = true;
		}
		return reversal;
	}
	
	public static boolean dojiBearishReversal(Candle prevCandle, Candle candle) {
		boolean reversal = false;
		boolean prevCandleDoji = dojiOrSpininTop(prevCandle);
		boolean currentCandleDoji = dojiOrSpininTop(candle);
		if (prevCandleDoji && !currentCandleDoji && redCandle(candle) && (candle.getClose() < prevCandle.getLow())) {
			reversal = true;
		}
		return reversal;
	}
	
	public static boolean bullishEngulfing(Candle prevCanle, Candle candle) {
		boolean bullishEngulfing = false;
		boolean prevCandleDoji = dojiOrSpininTop(prevCanle);
		boolean currentCandleDoji = dojiOrSpininTop(candle);
		boolean prevCandleBearish = redCandle(prevCanle);
		boolean currentCandleBullish = greenCandle(candle);
		if (!prevCandleDoji && !currentCandleDoji && prevCandleBearish && currentCandleBullish
				&& (candle.getOpen() < prevCanle.getClose()) && (candle.getClose() > prevCanle.getOpen())) {
			bullishEngulfing = true;
		}
		return bullishEngulfing;
	}
	
	public static boolean bearishEngulfing(Candle prevCanle, Candle candle) {
		boolean bearishEngulfing = false;
		boolean prevCandleDoji = dojiOrSpininTop(prevCanle);
		boolean currentCandleDoji = dojiOrSpininTop(candle);
		boolean prevCandleBullish = greenCandle(prevCanle);
		boolean currentCandleBearish = redCandle(candle);
		if (!prevCandleDoji && !currentCandleDoji && prevCandleBullish && currentCandleBearish
				&& (candle.getOpen() > prevCanle.getClose()) && (candle.getClose() < prevCanle.getOpen())) {
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
				&& (candle.getClose() > (prevCanle.getOpen() - (prevRange / 2)))) {
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
				&& (candle.getClose() < (prevCanle.getClose() + (prevRange / 2)))) {
			bearishPiercing = true;
		}
		return bearishPiercing;
	}
	
	public static boolean bullishHarami(Candle prevCanle, Candle candle){
		boolean bullishHarami = false;
		boolean prevCandleDoji = dojiOrSpininTop(prevCanle);
		boolean currentCandleDoji = dojiOrSpininTop(candle);
		boolean prevCandleBearish = redCandle(prevCanle);
		boolean currentCandleBullish = greenCandle(candle);
		if (!prevCandleDoji && !currentCandleDoji && prevCandleBearish && currentCandleBullish
				&& (candle.getOpen() > prevCanle.getClose()) && (candle.getClose() < prevCanle.getOpen())) {
			bullishHarami = true;
		}
		return bullishHarami;
	}
	
	public static boolean bearishHarami(Candle prevCanle, Candle candle){
		boolean bullishHarami = false;
		boolean prevCandleDoji = dojiOrSpininTop(prevCanle);
		boolean currentCandleDoji = dojiOrSpininTop(candle);
		boolean prevCandleBullish = greenCandle(prevCanle);
		boolean currentCandleBearish = redCandle(candle);
		if (!prevCandleDoji && !currentCandleDoji && prevCandleBullish && currentCandleBearish
				&& (candle.getOpen() < prevCanle.getClose()) && (candle.getClose() > prevCanle.getOpen())) {
			bullishHarami = true;
		}
		return bullishHarami;
	}
	
	public static boolean greenCandle(Candle candle){
		return candle.getClose() > candle.getOpen();
	}
	
	public static boolean redCandle(Candle candle){
		return candle.getClose() < candle.getOpen();
	}
	
	public static boolean dojiOrSpininTop(Candle candle){
		boolean dojiOrSpinningTop = false;
		double range = Math.abs(candle.getHigh() - candle.getLow());
		double bodySize = Math.abs(candle.getClose() - candle.getOpen());
		if ((bodySize <= (0.25 * range)) && (range < 0.004 * candle.getClose())){
			dojiOrSpinningTop = true;
		}
		return dojiOrSpinningTop;
	}
}

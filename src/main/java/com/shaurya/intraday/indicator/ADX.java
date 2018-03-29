/**
 * 
 */
package com.shaurya.intraday.indicator;

import java.util.ArrayList;
import java.util.List;

import com.shaurya.intraday.enums.IndicatorType;
import com.shaurya.intraday.model.ADXModel;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.IndicatorValue;

/**
 * @author Shaurya
 *
 */
public class ADX {
	public static ADXModel calculateADX(List<Candle> cList) {
		ADXModel adx = null;
		double adxValue = 0;
		if (cList.size() >= 30) {
			List<Double> dxList = new ArrayList<>();
			double positiveDM14 = 0;
			double negaiveDM14 = 0;
			double hlDiff = (double) cList.get(0).getHigh() - cList.get(0).getLow();
			double firstTR = hlDiff;
			for (int i = 1; i < 15; i++) {
				positiveDM14 += calculatePositiveDM(cList.get(i), cList.get(i - 1));
				negaiveDM14 += calculateNegativeDM(cList.get(i), cList.get(i - 1));

				firstTR += calculateTrueRange(cList.get(i), cList.get(i - 1));
			}
			dxList.add(calculateDX(positiveDM14, negaiveDM14, firstTR));

			adx = populateADX(cList, dxList, firstTR, positiveDM14, negaiveDM14, 15);

			double firstAdx = 0;

			for (int i = 0; i < 14; i++) {
				firstAdx += dxList.get(i);
			}
			firstAdx = (double) (firstAdx / 14);

			adxValue = calculateADX(dxList, firstAdx, 14);
		}
		adx.setAdx(new IndicatorValue(cList.get(cList.size() - 1).getTime(), adxValue, IndicatorType.ADX));
		return adx;
	}

	private static double calculateADX(List<Double> dxList, double prevADX, int index) {
		while (index < dxList.size()) {
			prevADX = (double) ((prevADX * 13) + dxList.get(index)) / 14;
			++index;
			// calculateADX(dxList, currentAdx, ++index);
		}
		return prevADX;
	}

	private static double calculateDX(double positiveDM14, double negaiveDM14, double tr14) {
		double positiveDI14 = (double) (positiveDM14 / tr14) * 100;
		double negativeDI14 = (double) (negaiveDM14 / tr14) * 100;
		double dx = (double) (Math.abs((positiveDI14 - negativeDI14)) / (positiveDI14 + negativeDI14)) * 100;
		return dx;
	}

	private static double calculateNegativeDM(Candle currentC, Candle prevC) {
		double negativeDM = (prevC.getLow() - currentC.getLow()) > (currentC.getHigh() - prevC.getHigh())
				? Math.max((prevC.getLow() - currentC.getLow()), 0) : 0;
		return negativeDM;
	}

	private static double calculatePositiveDM(Candle currentC, Candle prevC) {
		double positiveDM = (currentC.getHigh() - prevC.getHigh()) > (prevC.getLow() - currentC.getLow())
				? Math.max((currentC.getHigh() - prevC.getHigh()), 0) : 0;
		return positiveDM;
	}

	private static double calculateTrueRange(Candle currentC, Candle prevC) {
		double hlDiff;
		double hcDiff;
		double lcDiff;
		hlDiff = (double) currentC.getHigh() - currentC.getLow();
		hcDiff = Math.abs((double) (currentC.getHigh() - prevC.getClose()));
		lcDiff = Math.abs((double) (currentC.getLow() - prevC.getClose()));
		double tr = Math.max(hlDiff, Math.max(hcDiff, lcDiff));
		return tr;
	}

	public static ADXModel populateADX(List<Candle> cList, List<Double> dxList, double prevTR, double prevPositiveDM14,
			double prevNegativeDM14, int index) {
		while (index < cList.size()) {
			prevTR = prevTR - (prevTR / 14) + calculateTrueRange(cList.get(index), cList.get(index - 1));
			prevPositiveDM14 = prevPositiveDM14 - (prevPositiveDM14 / 14)
					+ calculatePositiveDM(cList.get(index), cList.get(index - 1));
			prevNegativeDM14 = prevNegativeDM14 - (prevNegativeDM14 / 14)
					+ calculateNegativeDM(cList.get(index), cList.get(index - 1));

			dxList.add(calculateDX(prevPositiveDM14, prevNegativeDM14, prevTR));

			++index;
			// populateADX(cList, dxList, tr14, positiveDM14, negativeDM14,
			// ++index);
		}
		return new ADXModel(null, prevTR, prevPositiveDM14, prevNegativeDM14, cList.get(cList.size() - 1));
	}

	public static void updateADX(Candle candle, ADXModel adx) {
		double tr14 = adx.getPrevTR() - (adx.getPrevTR() / 14) + calculateTrueRange(candle, adx.getLastCandle());
		double positiveDM14 = adx.getPrevPositiveDM() - (adx.getPrevPositiveDM() / 14)
				+ calculatePositiveDM(candle, adx.getLastCandle());
		double negativeDM14 = adx.getPrevNegativeDM() - (adx.getPrevNegativeDM() / 14)
				+ calculateNegativeDM(candle, adx.getLastCandle());

		double dx = calculateDX(positiveDM14, negativeDM14, tr14);

		double currentAdx = (double) ((adx.getAdx().getIndicatorValue() * 13) + dx) / 14;

		adx.setAdx(new IndicatorValue(candle.getTime(), currentAdx, IndicatorType.ADX));
		adx.setLastCandle(candle);
		adx.setPrevNegativeDM(negativeDM14);
		adx.setPrevPositiveDM(positiveDM14);
		adx.setPrevTR(tr14);
	}
}

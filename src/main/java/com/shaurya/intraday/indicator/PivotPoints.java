/**
 * 
 */
package com.shaurya.intraday.indicator;

import java.util.ArrayList;
import java.util.List;

import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.Level;

/**
 * @author Shaurya
 *
 */
public class PivotPoints {
	public static List<Level> getLevels(Candle candle) {
		List<Level> levels = new ArrayList<>();
		double pivotPoint = (double) (candle.getHigh() + candle.getLow() + candle.getClose()) / 3;
		double support1 = (pivotPoint * 2) - candle.getHigh();
		double support2 = pivotPoint - (candle.getHigh() - candle.getLow());
		double support3 = pivotPoint - (2*(candle.getHigh() - candle.getLow()));
		double support4 = pivotPoint - (3*(candle.getHigh() - candle.getLow()));
		double resistance1 = (pivotPoint * 2) - candle.getLow();
		double resistance2 = pivotPoint + (candle.getHigh() - candle.getLow());
		double resistance3 = pivotPoint + (2*(candle.getHigh() - candle.getLow()));
		double resistance4 = pivotPoint + (3*(candle.getHigh() - candle.getLow()));
		levels.add(new Level(pivotPoint, false));
		levels.add(new Level(support1, false));
		levels.add(new Level(support2, false));
		levels.add(new Level(support3, false));
		levels.add(new Level(support4, false));
		levels.add(new Level(resistance1, false));
		levels.add(new Level(resistance2, false));
		levels.add(new Level(resistance3, false));
		levels.add(new Level(resistance4, false));
		return levels;
	}
}

/**
 * 
 */
package com.shaurya.intraday.indicator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.shaurya.intraday.model.Level;

/**
 * @author Shaurya
 *
 */
public class GannSquare9 {
	public static List<Level> getLevels(double ltp) {
		List<Level> levels = new ArrayList<>();
		List<Double> levelsD = new ArrayList<>();
		double[][] gannSq = new double[9][9];
		int multiplyingFactor = 1;
		if (ltp / 10 < 10) {
			multiplyingFactor = 100;
		}
		int[] baseNum = new int[4];
		double meanLtp = Math.sqrt(ltp * multiplyingFactor);
		baseNum[0] = ((int) meanLtp) - 1;
		baseNum[1] = ((int) meanLtp);
		baseNum[2] = ((int) meanLtp) + 1;
		baseNum[3] = ((int) meanLtp) + 2;

		int i = 4, j = 4;
		gannSq[i][j] = (double) Math.pow(baseNum[0], 2) / multiplyingFactor;
		for (int k = 0; k < baseNum.length; k++) {
			double base = baseNum[k];
			base += 0.125;
			gannSq[i - (k + 1)][j] = (double) Math.pow(base, 2) / multiplyingFactor;
			base += 0.125;
			gannSq[i - (k + 1)][j - (k + 1)] = (double) Math.pow(base, 2) / multiplyingFactor;
			base += 0.125;
			gannSq[i][j - (k + 1)] = (double) Math.pow(base, 2) / multiplyingFactor;
			base += 0.125;
			gannSq[i + (k + 1)][j - (k + 1)] = (double) Math.pow(base, 2) / multiplyingFactor;
			base += 0.125;
			gannSq[i + (k + 1)][j] = (double) Math.pow(base, 2) / multiplyingFactor;
			base += 0.125;
			gannSq[i + (k + 1)][j + (k + 1)] = (double) Math.pow(base, 2) / multiplyingFactor;
			base += 0.125;
			gannSq[i][j + (k + 1)] = (double) Math.pow(base, 2) / multiplyingFactor;
			base += 0.125;
			gannSq[i - (k + 1)][j + (k + 1)] = (double) Math.pow(base, 2) / multiplyingFactor;
		}

		for (int k = 0; k < baseNum.length; k++) {
			levelsD.add(gannSq[i - (k + 1)][j]);
			levelsD.add(gannSq[i - (k + 1)][j - (k + 1)]);
			levelsD.add(gannSq[i][j - (k + 1)]);
			levelsD.add(gannSq[i + (k + 1)][j - (k + 1)]);
			levelsD.add(gannSq[i + (k + 1)][j]);
			levelsD.add(gannSq[i + (k + 1)][j + (k + 1)]);
			levelsD.add(gannSq[i][j + (k + 1)]);
			levelsD.add(gannSq[i - (k + 1)][j + (k + 1)]);
		}

		levelsD.add(ltp);

		Collections.sort(levelsD);

		for (Double l : levelsD) {
			levels.add(new Level(l, false));
		}

		Collections.sort(levels);
		return levels;
	}
}

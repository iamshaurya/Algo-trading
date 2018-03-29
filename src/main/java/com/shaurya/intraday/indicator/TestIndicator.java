/**
 * 
 */
package com.shaurya.intraday.indicator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.shaurya.intraday.model.ADXModel;
import com.shaurya.intraday.model.Candle;

/**
 * @author Shaurya
 *
 */
public class TestIndicator {

	/**
	 * @param args
	 */
	public static void mainTs(String[] args) {
		//Open, high, low, close
		List<Candle> cList = new ArrayList<>();
		cList.add(new Candle("INFY", new Date(2018, 01, 1), 44.52, 44.52, 43.98, 44.52, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 2), 44.52, 44.93, 44.36, 44.65, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 3), 44.52, 45.39, 44.70, 45.22, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 4), 44.52, 45.70, 45.13, 45.45, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 5), 44.52, 45.63, 44.89, 45.49, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 6), 44.52, 45.52, 44.20, 44.24, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 7), 44.52, 44.71, 44.00, 44.62, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 8), 44.52, 45.15, 43.76, 45.15, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 9), 44.52, 45.65, 44.46, 44.54, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 10), 44.52, 45.87, 45.13, 45.66, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 11), 44.52, 45.99, 45.27, 45.95, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 12), 44.52, 46.35, 45.80, 46.33, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 13), 44.52, 46.61, 46.10, 46.31, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 14), 44.52, 46.47, 45.77, 45.94, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 15), 44.52, 46.30, 45.14, 45.60, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 16), 44.52, 45.98, 44.97, 45.70, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 17), 44.52, 46.68, 46.10, 46.56, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 18), 44.52, 46.59, 46.14, 46.36, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 19), 44.52, 46.88, 46.39, 46.83, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 20), 44.52, 46.81, 46.41, 46.72, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 21), 44.52, 46.74, 45.94, 46.65, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 22), 44.52, 47.08, 46.68, 46.97, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 23), 44.52, 46.84, 46.17, 46.56, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 24), 44.52, 45.81, 45.10, 45.29, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 25), 44.52, 45.13, 44.35, 44.94, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 26), 44.52, 44.96, 44.61, 44.62, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 27), 44.52, 45.01, 44.20, 44.70, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 28), 44.52, 45.67, 44.93, 45.27, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 29), 44.52, 45.71, 45.01, 45.44, 0));
		cList.add(new Candle("INFY", new Date(2018, 01, 30), 44.52, 45.35, 44.46, 44.76, 0));
		ADXModel adx = ADX.calculateADX(cList);
		System.out.println("adx : "+adx.getAdx().toString());
		System.out.println("last candle : "+adx.getLastCandle().toString());
		System.out.println("+DM14 : "+adx.getPrevPositiveDM());
		System.out.println("-DM14 : "+adx.getPrevNegativeDM());
		System.out.println("TR : "+adx.getPrevTR());

	}

}

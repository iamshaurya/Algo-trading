/**
 * 
 */
package com.shaurya.intraday.indicator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import com.shaurya.intraday.model.StockMovement;
import com.shaurya.intraday.model.Level;

/**
 * @author Shaurya
 *
 */
public class TestIndicator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		List<Level> level = GannSquare9.getLevels(348.70);
		for (Level l : level) {
			System.out.println(l.getValue());
		}
		List<StockMovement> topGainer = new ArrayList<>();
		StockMovement bs1 = new StockMovement("a", 1, 10, 10);
		StockMovement bs2 = new StockMovement("b", 2, 15, 15);
		StockMovement bs3 = new StockMovement("c", 3, 12, 12);
		StockMovement bs4 = new StockMovement("d", 4, 100, 100);
		topGainer.add(bs1);
		topGainer.add(bs2);
		topGainer.add(bs3);
		topGainer.add(bs4);
		Collections.sort(topGainer);
		System.out.println("break");
		bs1.updateLtp(12.5);
		bs2.updateLtp(16.5);
		bs3.updateLtp(11.6);
		bs4.updateLtp(99);
		Collections.sort(topGainer);
		StockMovement bs5 = new StockMovement("e", 5, 500, 550);
		topGainer.add(bs5);
		Collections.sort(topGainer);
		System.out.println("break");
	}

}

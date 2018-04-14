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
		
	}

}

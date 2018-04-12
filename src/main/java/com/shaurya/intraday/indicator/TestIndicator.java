/**
 * 
 */
package com.shaurya.intraday.indicator;

import java.util.List;

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
		List<Level> level = GannSquare9.getLevels(300.15);
		for(Level l:level){
			System.out.println(l.getValue());
		}
	}

}

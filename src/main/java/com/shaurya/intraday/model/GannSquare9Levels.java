/**
 * 
 */
package com.shaurya.intraday.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Shaurya
 *
 */
@Getter
@Setter
public class GannSquare9Levels {
	private List<Level> supportLevels;
	private List<Level> resistanceLevels;
	private double ltp;
}

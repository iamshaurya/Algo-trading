/**
 * 
 */
package com.shaurya.intraday.model;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Shaurya
 *
 */
@Getter
@Setter
public class Level implements Comparable<Level> {
	private double value;
	private boolean acheived;

	public Level(double value, boolean acheived) {
		this.value = value;
		this.acheived = acheived;
	}

	@Override
	public int compareTo(Level o) {
		if (this.value < o.value) {
			return -1;
		} else if (this.value > o.value) {
			return 1;
		} else {
			return 0;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		Level l = (Level) o;
		if (this.value == l.value) {
			return true;
		}
		return false;
	}
}

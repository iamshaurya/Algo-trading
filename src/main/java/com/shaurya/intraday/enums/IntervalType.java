/**
 * 
 */
package com.shaurya.intraday.enums;

/**
 * @author Shaurya
 *
 */
public enum IntervalType {
	MINUTE_1(1, "minute"), MINUTE_3(2, "3minute"), MINUTE_5(3, "5minute"), MINUTE_10(4, "10minute"), MINUTE_15(5,
			"15minute"), MINUTE_30(6, "30minute"), MINUTE_60(7, "60minute"), DAY(8, "day");

	private int id;
	private String desc;

	private IntervalType(int id, String desc) {
		this.id = id;
		this.desc = desc;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

}

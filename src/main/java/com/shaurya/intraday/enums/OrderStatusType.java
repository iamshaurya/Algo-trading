/**
 * 
 */
package com.shaurya.intraday.enums;

/**
 * @author Shaurya
 *
 */
public enum OrderStatusType {
	OPEN(1), CLOSE(2);
	private int id;

	private OrderStatusType(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public static OrderStatusType getEnumById(int id) {
		for (OrderStatusType e : OrderStatusType.values()) {
			if (e.getId() == id) {
				return e;
			}
		}
		return null;
	}
}

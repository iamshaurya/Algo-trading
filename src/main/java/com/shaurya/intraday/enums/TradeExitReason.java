/**
 * 
 */
package com.shaurya.intraday.enums;

/**
 * @author Shaurya
 *
 */
public enum TradeExitReason {
	TAKE_PROFIT_REACHED((byte) 1), STRATEGY_EXIT_CRITERIA_MET((byte) 2), HARD_STOP_LOSS_HIT((byte) 3), CLOSING_TIME(
			(byte) 4), STOP_LOSS_REACHED((byte)5);
	private byte id;

	private TradeExitReason(byte id) {
		this.id = id;
	}

	public byte getId() {
		return id;
	}

	public void setId(byte id) {
		this.id = id;
	}

	public static TradeExitReason getEnumById(byte id) {
		for (TradeExitReason e : TradeExitReason.values()) {
			if (e.getId() == id) {
				return e;
			}
		}
		return null;
	}

}

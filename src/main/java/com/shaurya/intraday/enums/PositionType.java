/**
 * 
 */
package com.shaurya.intraday.enums;

/**
 * @author Shaurya
 *
 */
public enum PositionType {
	LONG(1), SHORT(2), BOTH(3);
	private int id;

	private PositionType(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public static PositionType getEnumById(int id){
		for(PositionType e: PositionType.values()){
			if(e.getId() == id){
				return e;
			}
		}
		return null;
	}
	
}

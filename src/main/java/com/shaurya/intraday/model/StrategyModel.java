/**
 * 
 */
package com.shaurya.intraday.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shaurya.intraday.enums.PositionType;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Shaurya
 *
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class StrategyModel {
	// Risk to reward => 1:2
	private PositionType position;
	private double atr;
	private double sl;
	private double tp;
	private double tradePrice;
	private String security;
	private Long securityToken;
	private String orderId;
	private int quantity;
	private boolean exitOrder;
	private PositionType preferedPosition;
	private double marginMultiplier;
	private double tradeMargin;
	private double marginPortion;
	private boolean trailSl;

	public StrategyModel() {

	}

	public StrategyModel(PositionType position, double atr, double tradePrice, String security, String orderId,
			int quantity, boolean exitOrder) {
		this.position = position;
		this.atr = atr;
		this.tradePrice = tradePrice;
		this.security = security;
		this.orderId = orderId;
		this.quantity = quantity;
		this.exitOrder = exitOrder;
		this.setSl(this.atr);
		this.setTp(this.sl);
		this.trailSl = false;
	}

	public void setSl(double atr) {
		int ai = (int) (atr * 100);
		double ad = Math.ceil((double) ai / 10);
		double fa = (double) ad / 10;
		this.sl = fa * 2;
	}
	
	public void trailSl(double tsl){
		int ai = (int) (tsl * 100);
		double ad = Math.ceil((double) ai / 10);
		double fa = (double) ad / 10;
		this.sl = this.sl - fa;
	}

	public void setTp(double sl) {
		//20 to keep it open ended
		int ai = (int) (this.sl * 20 * 100);
		double ad = Math.ceil((double) ai / 10);
		double fa = (double) ad / 10;
		this.tp = fa;
	}

	@Override
	public String toString() {
		return "StrategyModel [position=" + position + ", atr=" + atr + ", sl=" + sl + ", tp=" + tp + ", tradePrice="
				+ tradePrice + ", security=" + security + ", orderId=" + orderId + ", quantity=" + quantity
				+ ", exitOrder=" + exitOrder + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((security == null) ? 0 : security.hashCode());
		result = prime * result + ((securityToken == null) ? 0 : securityToken.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StrategyModel other = (StrategyModel) obj;
		if (security == null) {
			if (other.security != null)
				return false;
		} else if (!security.equals(other.security))
			return false;
		if (securityToken == null) {
			if (other.securityToken != null)
				return false;
		} else if (!securityToken.equals(other.securityToken))
			return false;
		return true;
	}

}

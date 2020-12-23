/**
 * 
 */
package com.shaurya.intraday.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.shaurya.intraday.enums.ExchangeType;
import com.shaurya.intraday.enums.PositionType;

import java.util.Date;
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
	private double sl;
	private double tp;
	private double tradePrice;
	private String security;
	private Long securityToken;
	private String orderId;
	private int quantity;
	private int lotSize;
	private boolean exitOrder;
	private boolean slhit;
	private PositionType preferedPosition;
	private double marginMultiplier;
	private double tradeMargin;
	private double marginPortion;
	private boolean trailSl;
	private ExchangeType exchangeType;
	@JsonProperty("beta")
	private double atr;
	private Date tradeDate;

	public StrategyModel() {

	}

	public StrategyModel(long token, PositionType position, double slPoint, double tradePrice, String security,
			String orderId, int quantity, boolean exitOrder) {
		this.securityToken = token;
		this.position = position;
		this.tradePrice = tradePrice;
		this.security = security;
		this.orderId = orderId;
		this.quantity = quantity;
		this.exitOrder = exitOrder;
		this.sl = slPoint;
		this.setTp(this.sl);
		this.trailSl = false;
		this.slhit = false;
	}

	public void trailSl(double tsl) {
		int ai = (int) (tsl * 100);
		double ad = Math.ceil((double) ai / 10);
		double fa = (double) ad / 10;
		this.sl = this.sl - fa;
		System.out.println("Trailing sl for : " + this.security + " new sl : " + this.sl);
	}

	public void setTp(double sl) {
		// 50 to keep it open ended
		/*int ai = (int) (this.sl * 50 * 100);
		double ad = Math.ceil((double) ai / 10);
		double fa = (double) ad / 10;*/
		this.tp = sl * 1000;
	}

	@Override
	public String toString() {
		return "StrategyModel [position=" + position + ", sl=" + sl + ", tp=" + tp + ", tradePrice="
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

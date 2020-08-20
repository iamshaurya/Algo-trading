/**
 * 
 */
package com.shaurya.intraday.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Shaurya
 *
 */
@Table(name = "trade_strategy")
@Entity
@Getter
@Setter
public class TradeStrategy {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Integer id;
	@Column(name = "security_name")
	private String securityName;
	@Column(name = "security_token")
	private Long securityToken;
	@Column(name = "strategy_type")
	private Integer strategyType;
	@Column(name = "day")
	private Integer day;
	@Column(name = "prefered_position")
	private Byte preferedPosition;
	@Column(name = "margin_multiplier")
	private Double marginMultiplier;
	@Column(name = "margin_portion")
	private Double marginPortion;
	@Column(name = "quantity")
	private Integer quantity;
	@Column(name = "lot_size")
	private Integer lotSize;
}

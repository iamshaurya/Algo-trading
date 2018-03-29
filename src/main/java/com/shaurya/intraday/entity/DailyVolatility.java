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
@Getter
@Setter
@Entity
@Table(name = "prev_nse_volatility")
public class DailyVolatility {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Integer id;
	@Column(name = "symbol")
	private String symbol;
	@Column(name = "daily_vol")
	private double dailyVolatility;
	@Column(name = "yearly_vol")
	private double annualVolatility;
}

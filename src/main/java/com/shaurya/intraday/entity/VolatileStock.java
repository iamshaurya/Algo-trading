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
@Table(name = "volatile_stock")
@Entity
@Getter
@Setter
public class VolatileStock {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Integer id;
	@Column(name = "symbol")
	private String symbol;
	@Column(name = "state")
	private Byte state;

}

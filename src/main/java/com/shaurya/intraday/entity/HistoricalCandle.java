/**
 * 
 */
package com.shaurya.intraday.entity;

import java.util.Date;

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
@Entity
@Table(name = "historical_candles")
@Getter
@Setter
public class HistoricalCandle implements Comparable<HistoricalCandle> {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Integer id;
	@Column(name = "security_name")
	private String securityName;
	@Column(name = "open")
	private Double open;
	@Column(name = "close")
	private Double close;
	@Column(name = "high")
	private Double high;
	@Column(name = "low")
	private Double low;
	@Column(name = "timestamp")
	private Date timestamp;
	@Column(name = "day")
	private Integer day;

	@Override
	public int compareTo(HistoricalCandle o) {
		Date a = this.timestamp;
		Date b = o.timestamp;
		if (a.before(b)) {
			return -1;
		} else if (a.after(b)) {
			return 1;
		} else {
			return 0;
		}
	}

}

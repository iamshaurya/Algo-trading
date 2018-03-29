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
@Entity
@Table(name = "kite_account")
@Getter
@Setter
public class KiteAccount {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Integer id;
	@Column(name = "api_key")
	private String apiKey;
	@Column(name = "secret_key")
	private String secretKey;
	@Column(name = "user_id")
	private String userId;

}

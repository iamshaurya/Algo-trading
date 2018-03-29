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
@Table(name = "mail_sender_account")
@Getter
@Setter
public class MailSenderAccount {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Integer id;
	@Column(name = "api_user")
	private String apiUser;
	@Column(name = "api_key")
	private String apiKey;
	@Column(name = "from_id")
	private String fromId;

}

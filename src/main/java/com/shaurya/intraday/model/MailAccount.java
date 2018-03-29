/**
 * 
 */
package com.shaurya.intraday.model;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Shaurya
 *
 */
@Getter
@Setter
public class MailAccount {
	private Integer id;
	private String apiUser;
	private String apiKey;
	private String fromId;
}

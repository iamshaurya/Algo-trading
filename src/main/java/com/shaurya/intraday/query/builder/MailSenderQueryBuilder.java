/**
 * 
 */
package com.shaurya.intraday.query.builder;

import java.util.HashMap;

import com.shaurya.intraday.model.CustomQueryHolder;

/**
 * @author Shaurya
 *
 */
public class MailSenderQueryBuilder {
	public static CustomQueryHolder queryToFetchMailAccountDetails() {
		CustomQueryHolder cq = new CustomQueryHolder();
		StringBuilder sb = new StringBuilder();
		sb.append("select ma from MailSenderAccount ma");
		cq.setQueryString(sb.toString());
		cq.setInParamMap(new HashMap<>());
		return cq;
	}
}

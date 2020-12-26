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
public class AccountQueryBuilder {
	public static CustomQueryHolder fetchAccountQuery() {
		CustomQueryHolder cq = new CustomQueryHolder();
		StringBuilder sb = new StringBuilder();
		sb.append("select acc from KiteAccount acc");
		cq.setQueryString(sb.toString());
		cq.setInParamMap(new HashMap<>());
		return cq;
	}

	public static CustomQueryHolder fetchKiteAccountAuditData() {
		CustomQueryHolder cq = new CustomQueryHolder();
		StringBuilder sb = new StringBuilder();
		sb.append("select acc from KiteAccountAudit acc");
		cq.setQueryString(sb.toString());
		cq.setInParamMap(new HashMap<>());
		return cq;
	}
}

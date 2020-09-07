/**
 * 
 */
package com.shaurya.intraday.trade.service;

import com.shaurya.intraday.entity.KiteAccount;

/**
 * @author Shaurya
 *
 */
public interface AccountService {
	public KiteAccount getAccount();

  void updateFundBalance(Integer totalFund);

  Integer getFund();
}

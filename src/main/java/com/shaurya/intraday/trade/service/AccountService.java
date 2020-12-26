/**
 * 
 */
package com.shaurya.intraday.trade.service;

import com.shaurya.intraday.entity.KiteAccount;
import com.shaurya.intraday.entity.KiteAccountAudit;
import java.util.TreeSet;

/**
 * @author Shaurya
 *
 */
public interface AccountService {
	public KiteAccount getAccount();

  void updateFundBalance(Long totalFund);

  Long getFund();

  TreeSet<KiteAccountAudit> getAllAuditData();
}

/**
 * 
 */
package com.shaurya.intraday.trade.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.shaurya.intraday.entity.KiteAccount;
import com.shaurya.intraday.query.builder.AccountQueryBuilder;
import com.shaurya.intraday.repo.JpaRepo;

/**
 * @author Shaurya
 *
 */
@Service
public class AccountServiceImpl implements AccountService {
	@Autowired
	private JpaRepo<KiteAccount> accountRepo;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.shaurya.intraday.trade.service.AccountService#getAccount()
	 */
	@Override
	public KiteAccount getAccount() {
		List<KiteAccount> accounts = accountRepo.fetchByQuery(AccountQueryBuilder.fetchAccountQuery());
		if (accounts != null && !accounts.isEmpty()) {
			return accounts.get(0);
		}
		return null;
	}

}

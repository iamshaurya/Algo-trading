/**
 *
 */
package com.shaurya.intraday.trade.service;

import com.shaurya.intraday.entity.KiteAccountAudit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.shaurya.intraday.entity.KiteAccount;
import com.shaurya.intraday.query.builder.AccountQueryBuilder;
import com.shaurya.intraday.repo.JpaRepo;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Shaurya
 *
 */
@Service
public class AccountServiceImpl implements AccountService {

  @Autowired
  private JpaRepo<KiteAccount> accountRepo;
  @Autowired
  private JpaRepo<KiteAccountAudit> accountAuditJpaRepo;

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

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void updateFundBalance(Integer totalFund) {
    List<KiteAccount> accounts = accountRepo.fetchByQuery(AccountQueryBuilder.fetchAccountQuery());
    KiteAccount accountToUpdate = null;
    if (accounts != null && !accounts.isEmpty()) {
      accountToUpdate = accounts.get(0);
    }
    KiteAccountAudit audit = KiteAccountAudit.builder().accountId(accountToUpdate.getId())
        .fund(accountToUpdate.getFund()).build();
    this.accountAuditJpaRepo.update(audit);

    accountToUpdate.setFund(totalFund);
    this.accountRepo.update(accountToUpdate);
  }


  @Override
  public Integer getFund() {
    KiteAccount account = getAccount();
    if (account == null) {
      return 0;
    }
    return account.getFund();
  }

}

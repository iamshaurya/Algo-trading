/**
 *
 */
package com.shaurya.intraday.trade.service;

import com.shaurya.intraday.entity.KiteAccountAudit;
import java.util.List;

import java.util.TreeSet;
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
  public void updateFundBalance(Long totalFund) {
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
  public Long getFund() {
    KiteAccount account = getAccount();
    if (account == null) {
      return 0l;
    }
    return account.getFund();
  }

  @Override
  public TreeSet<KiteAccountAudit> getAllAuditData() {
    List<KiteAccountAudit> kiteAccountAudits = this.accountAuditJpaRepo
        .fetchByQuery(AccountQueryBuilder.fetchKiteAccountAuditData());
    TreeSet<KiteAccountAudit> sortedAuditData = new TreeSet<>(kiteAccountAudits);
    KiteAccountAudit latestAccountAudit = KiteAccountAudit.builder().fund(getFund())
        .id(sortedAuditData.last().getId() + 1).build();
    sortedAuditData.add(latestAccountAudit);
    return sortedAuditData;
  }

}

/**
 * 
 */
package com.shaurya.intraday.conf;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.shaurya.intraday.entity.MailSenderAccount;
import com.shaurya.intraday.model.MailAccount;
import com.shaurya.intraday.query.builder.MailSenderQueryBuilder;
import com.shaurya.intraday.repo.JpaRepo;

/**
 * @author Shaurya
 *
 */
@Configuration
public class MailSenderConfig {
	@Autowired
	private JpaRepo<MailSenderAccount> mailRepo;

	@Bean
	public MailAccount mailAccount(){
		MailAccount mailAccount = null;
		List<MailSenderAccount> maList = mailRepo.fetchByQuery(MailSenderQueryBuilder.queryToFetchMailAccountDetails());
		MailSenderAccount mailSenderAccount = null;
		if(maList != null && !maList.isEmpty()){
			mailSenderAccount = maList.get(0);
			mailAccount = new MailAccount();
			mailAccount.setId(mailSenderAccount.getId());
			mailAccount.setApiUser(mailSenderAccount.getApiUser());
			mailAccount.setApiKey(mailSenderAccount.getApiKey());
			mailAccount.setFromId(mailSenderAccount.getFromId());
		}
		return mailAccount;
	}
}

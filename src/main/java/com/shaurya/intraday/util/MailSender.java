/**
 * 
 */
package com.shaurya.intraday.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.shaurya.intraday.model.MailAccount;

/**
 * @author Shaurya
 *
 */
public class MailSender {
	public static void sendMail(String to, String toName, String subject, String body, MailAccount mailAccount) {
		String url = "https://api.sendgrid.com/api/mail.send.json";
		List<NameValuePair> nvp = new ArrayList<>();
		nvp.add(new BasicNameValuePair("api_user", mailAccount.getApiUser()));
		nvp.add(new BasicNameValuePair("api_key", mailAccount.getApiKey()));
		nvp.add(new BasicNameValuePair("to", to));
		nvp.add(new BasicNameValuePair("toname", toName));
		nvp.add(new BasicNameValuePair("subject", subject));
		nvp.add(new BasicNameValuePair("text", body));
		nvp.add(new BasicNameValuePair("from", mailAccount.getFromId()));

		HttpClientService.executePostRequest(url, nvp, null);
	}
}

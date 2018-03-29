/**
 * 
 */
package com.shaurya.intraday.trade.service;

import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.shaurya.intraday.util.HttpClientService;

/**
 * @author Shaurya
 *
 */
//@Component
public class CronService {
	//@Scheduled(cron = "0 30 8 * * MON-FRI")
	public void login() {
		HttpClientService.executeGetRequest("http://127.0.0.1:8080/trade/v1/startup/login", new ArrayList<NameValuePair>());
	}
	
	//@Scheduled(cron = "0 55 8 * * MON-FRI")
	public void startup() {
		HttpClientService.executeGetRequest("http://127.0.0.1:8080/trade/v1/startup", new ArrayList<NameValuePair>());
	}

	//@Scheduled(cron = "0 0 16 * * MON-FRI")
	public void shutdown() {
		HttpClientService.executeGetRequest("http://127.0.0.1:8080/trade/v1/shutdown", new ArrayList<NameValuePair>());
	}

	//@Scheduled(cron = "	0 12 13 * * *")
	public void stockScreener() {
		HttpClientService.executeGetRequest("http://127.0.0.1:8080/trade/v1/stockScreener", new ArrayList<NameValuePair>());
	}
}

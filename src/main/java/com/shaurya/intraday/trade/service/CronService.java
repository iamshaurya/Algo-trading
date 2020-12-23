/**
 *
 */
package com.shaurya.intraday.trade.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.Date;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.shaurya.intraday.util.HttpClientService;

/**
 * @author Shaurya
 *
 */
@Slf4j
@Component
public class CronService {
  @Autowired
  private TradeService tradeService;

	@Scheduled(cron = "0 30 8 * * MON-FRI")
	public void login() {
    if (isTodayHoliday()) {
      return;
    }
		HttpClientService.executeGetRequest("http://127.0.0.1:9080/trade/v1/startup/login", new ArrayList<NameValuePair>());
	}

	@Scheduled(cron = "0 55 8 * * MON-FRI")
	public void startup() {
    if (isTodayHoliday()) {
      return;
    }
		HttpClientService.executeGetRequest("http://127.0.0.1:9080/trade/v1/startup", new ArrayList<NameValuePair>());
	}

	@Scheduled(cron = "0 0 16 * * MON-FRI")
	public void shutdown() {
    if (isTodayHoliday()) {
      return;
    }
		HttpClientService.executeGetRequest("http://127.0.0.1:9080/trade/v1/shutdown", new ArrayList<NameValuePair>());
	}

	//@Scheduled(cron = "0 0 20 * * MON-FRI")
	public void stockScreener() {
    if (isTodayHoliday()) {
      return;
    }
		HttpClientService.executeGetRequest("http://127.0.0.1:9080/trade/v1/update/strategies", new ArrayList<NameValuePair>());
	}

  private boolean isTodayHoliday() {
    try {
      Set<Date> holidays = tradeService.getHolidayDates();
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      Date date = sdf.parse(sdf.format(new Date()));
      return holidays.contains(date);
    } catch (ParseException e) {
      log.error("error in parsing holiday {}", e);
    }
    return false;
  }

}

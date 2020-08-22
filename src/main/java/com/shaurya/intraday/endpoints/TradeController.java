/**
 *
 */
package com.shaurya.intraday.endpoints;

import com.shaurya.intraday.constant.Constants;
import com.shaurya.intraday.enums.IntervalType;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.MailAccount;
import com.shaurya.intraday.trade.service.SetupServiceImpl;
import com.shaurya.intraday.trade.service.TradeService;
import com.shaurya.intraday.util.MailSender;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Shaurya
 *
 */
@Slf4j
@RestController
@RequestMapping("/v1")
public class TradeController {

  @Autowired
  private SetupServiceImpl setupService;
  @Autowired
  private TradeService tradeService;
  @Autowired
  private MailAccount mailAccount;

  @RequestMapping(value = "/startup/login", method = RequestMethod.GET)
  public ResponseEntity<Boolean> startUpLogin() {
    setupService.startupLogin();
    return new ResponseEntity<>(true, HttpStatus.OK);
  }

  @RequestMapping(value = "/startup", method = RequestMethod.GET)
  public ResponseEntity<Boolean> startUp() {
    try {
      setupService.startup();
    } catch (KiteException | IOException | JSONException e) {
      String reason = "startup failed by cron because :: " + e.getCause();
      log.error("startup failed {}", reason);
      MailSender.sendMail(Constants.TO_MAIL, Constants.TO_NAME, Constants.STARTUP_FALIED, reason,
          mailAccount);
    }
    return new ResponseEntity<>(true, HttpStatus.OK);
  }

  @RequestMapping(value = "/shutdown", method = RequestMethod.GET)
  public ResponseEntity<Boolean> shutdown() {
    try {
      setupService.shutdown();
    } catch (IOException | KiteException e) {
      String reason = "shutdown failed by cron because :: " + e.getCause();
			log.error("shutdown failed {}", reason);
      MailSender.sendMail(Constants.TO_MAIL, Constants.TO_NAME, Constants.SHUTDOWN_FALIED, reason,
          mailAccount);
    }
    return new ResponseEntity<>(true, HttpStatus.OK);
  }

  @RequestMapping(value = "/test", method = RequestMethod.POST)
  public ResponseEntity<List<Candle>> test(
      final @RequestParam(value = "security", required = true) Long security,
      final @RequestParam(value = "intervalType", required = true) IntervalType intervalType,
      final @RequestParam(value = "from", required = true) @DateTimeFormat(pattern = "dd-MM-yyyy") Date from,
      final @RequestParam(value = "to", required = true) @DateTimeFormat(pattern = "dd-MM-yyyy") Date to,
      final @RequestParam(value = "candleCount", required = true) Integer candleCount) {
    return new ResponseEntity<List<Candle>>(
        tradeService.getPrevDayCandles(security, IntervalType.MINUTE_15, from, to, candleCount),
        HttpStatus.OK);
  }

  @RequestMapping(value = "/test/indicator", method = RequestMethod.GET)
  public ResponseEntity<String> testIndicator() {
    try {
      tradeService.testIndicator();
    } catch (IOException | KiteException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return new ResponseEntity<>("Check mail", HttpStatus.OK);
  }

  @RequestMapping(value = "/test/simulation", method = RequestMethod.GET)
  public ResponseEntity<String> simulation(
      final @RequestParam(value = "security", required = true) Long security) {
    tradeService.simulation(security);
    return new ResponseEntity<>("Check mail", HttpStatus.OK);
  }

  @RequestMapping(value = "/checkBalance", method = RequestMethod.GET)
  public ResponseEntity<Double> checkBalance() throws IOException, KiteException {
    return new ResponseEntity<Double>(tradeService.checkBalance(), HttpStatus.OK);
  }

}

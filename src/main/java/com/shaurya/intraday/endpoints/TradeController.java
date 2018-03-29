/**
 * 
 */
package com.shaurya.intraday.endpoints;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shaurya.intraday.constant.Constants;
import com.shaurya.intraday.enums.StrategyType;
import com.shaurya.intraday.model.MailAccount;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.trade.service.SetupServiceImpl;
import com.shaurya.intraday.trade.service.StockScreener;
import com.shaurya.intraday.trade.service.TradeService;
import com.shaurya.intraday.util.MailSender;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

/**
 * @author Shaurya
 *
 */
@RestController
@RequestMapping("/v1")
public class TradeController {
	@Autowired
	private SetupServiceImpl setupService;
	@Autowired
	private TradeService tradeService;
	@Autowired
	private StockScreener screener;
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
			System.out.println(reason);
			MailSender.sendMail(Constants.TO_MAIL, Constants.TO_NAME, Constants.STARTUP_FALIED, reason, mailAccount);
		}
		return new ResponseEntity<>(true, HttpStatus.OK);
	}

	@RequestMapping(value = "/shutdown", method = RequestMethod.GET)
	public ResponseEntity<Boolean> shutdown() {
		try {
			setupService.shutdown();
		} catch (IOException | KiteException e) {
			String reason = "shutdown failed by cron because :: " + e.getCause();
			System.out.println(reason);
			MailSender.sendMail(Constants.TO_MAIL, Constants.TO_NAME, Constants.SHUTDOWN_FALIED, reason, mailAccount);
		}
		return new ResponseEntity<>(true, HttpStatus.OK);
	}

	/*@RequestMapping(value = "/stockScreener", method = RequestMethod.GET)
	public ResponseEntity<Boolean> stockScreener() {
		setupService.stockScreener();
		return new ResponseEntity<>(true, HttpStatus.OK);
	}*/

	@RequestMapping(value = "/test", method = RequestMethod.POST)
	public ResponseEntity<String> test() {
		tradeService.sendPNLStatement();
		return new ResponseEntity<>("Check mail", HttpStatus.OK);
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
	public ResponseEntity<String> simulation(final @RequestParam(value = "security", required = true) Long security) {
		tradeService.simulation(security);
		return new ResponseEntity<>("Check mail", HttpStatus.OK);
	}

	@RequestMapping(value = "/test/screen", method = RequestMethod.GET)
	public ResponseEntity<Map<StrategyType, List<StrategyModel>>> screen(
			final @RequestParam(value = "security", required = true) Long security) throws IOException, KiteException {
		return new ResponseEntity<>(screener.getFilteredStocks(), HttpStatus.OK);
	}

}

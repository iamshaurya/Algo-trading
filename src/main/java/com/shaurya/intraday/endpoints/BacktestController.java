/**
 * 
 */
package com.shaurya.intraday.endpoints;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
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
import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.enums.StrategyType;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.trade.backtest.service.BackTestService;
import com.shaurya.intraday.trade.service.LoginService;
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
public class BacktestController {
	@Autowired
	private BackTestService backtest;
	@Autowired
	private LoginService loginService;

	@RequestMapping(value = "/backtest", method = RequestMethod.POST)
	public ResponseEntity<Boolean> backtest(
			final @RequestParam(value = "position", required = true) PositionType position,
			final @RequestParam(value = "strategyType", required = true) StrategyType strategyType,
			final @RequestParam(value = "security", required = true) String security,
			final @RequestParam(value = "duration", required = true) Integer duration) {
		try {
			backtest.start(position, strategyType, 500, duration);
		} catch (IOException | KiteException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ResponseEntity<>(true, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/backtest/login", method = RequestMethod.GET)
	public ResponseEntity<String> login(
			final @RequestParam(value = "request_token", required = true) String request_token) {
		try {
			loginService.initializeSdkClient(request_token);
		} catch (JSONException | IOException | KiteException e) {
			System.out.println("Error in initializing sdk client after receiving request token :: " + request_token);
			return new ResponseEntity<>("login unsuccessfull", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>("login successfull", HttpStatus.OK);
	}

}

/**
 * 
 */
package com.shaurya.intraday.endpoints;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shaurya.intraday.trade.service.LoginService;
import com.shaurya.intraday.trade.service.SetupServiceImpl;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

/**
 * @author Shaurya
 *
 */
@RestController
@RequestMapping("/v1")
public class LoginController {
	@Autowired
	private LoginService loginService;
	@Autowired
	private SetupServiceImpl setupService;

	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public ResponseEntity<String> getTrade(
			final @RequestParam(value = "request_token", required = true) String request_token) {
		try {
			loginService.initializeSdkClient(request_token);
			Calendar start = Calendar.getInstance();
			start.set(Calendar.HOUR_OF_DAY, 8);
			start.set(Calendar.MINUTE, 58);
			Calendar end = Calendar.getInstance();
			end.set(Calendar.HOUR_OF_DAY, 15);
			end.set(Calendar.MINUTE, 31);
			Date now = new Date();
			if (now.after(start.getTime()) && now.before(end.getTime())) {
				setupService.startup();
			} else {
				setupService.stockScreener();
			}
		} catch (JSONException | IOException | KiteException e) {
			System.out.println("Error in initializing sdk client after receiving request token :: " + request_token);
			return new ResponseEntity<>("login unsuccessfull", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<>("login successfull", HttpStatus.OK);
	}
}

/**
 *
 */
package com.shaurya.intraday.endpoints;

import com.shaurya.intraday.trade.service.LoginService;
import com.shaurya.intraday.trade.service.SetupServiceImpl;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
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
      setupService.startup();
    } catch (JSONException | IOException | KiteException e) {
      log.error(
          "Error in initializing sdk client after receiving request token :: " + request_token);
      return new ResponseEntity<>("login unsuccessfull", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return new ResponseEntity<>("login successfull", HttpStatus.OK);
  }
}

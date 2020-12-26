/**
 *
 */
package com.shaurya.intraday.trade.service;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.shaurya.intraday.constant.Constants;
import com.shaurya.intraday.model.MailAccount;
import com.shaurya.intraday.util.MailSender;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.SessionExpiryHook;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.User;

/**
 * @author Shaurya
 *
 */
@Slf4j
@Service
public class LoginServiceImpl implements LoginService {

  // singleton
  private KiteConnect kiteSdk;

  @Autowired
  private AccountService accountService;
  @Autowired
  private MailAccount mailAccount;
  @Autowired
  private TradeOrderService tradeOrderService;

  @Override
  public String getLoginUrl() {
    if (kiteSdk == null) {
      kiteSdk = new KiteConnect(accountService.getAccount().getApiKey());
    }

    // Set userId.
    kiteSdk.setUserId(accountService.getAccount().getUserId());

    /*
     * First you should get request_token, public_token using kitconnect
     * login and then use request_token, public_token, api_secret to make
     * any kiteconnect api call. Get login url. Use this url in webview to
     * login user, after authenticating user you will get requestToken. Use
     * the same to get accessToken.
     */
    return kiteSdk.getLoginURL();
  }

  @Override
  public void initializeSdkClient(String requestToken)
      throws JSONException, IOException, KiteException {
    // Get accessToken as follows,
    User userModel = kiteSdk
        .generateSession(requestToken, accountService.getAccount().getSecretKey());

    // Set request token and public token which are obtained from login
    // process.
    kiteSdk.setAccessToken(userModel.accessToken);
    kiteSdk.setPublicToken(userModel.publicToken);

    // Set session expiry callback.
    kiteSdk.setSessionExpiryHook(new SessionExpiryHook() {
      @Override
      public void sessionExpired() {
        handleSessionExpired();
      }
    });

    refreshFundBalance();
  }

  private void refreshFundBalance() {
    try {
      Long totalFund = this.tradeOrderService.getTotalMargin().longValue();
      this.accountService.updateFundBalance(totalFund);
    } catch (IOException e) {
      log.error("something went wrong in fetching total margin {}", e);
      MailSender
          .sendMail(Constants.TO_MAIL, Constants.TO_NAME,
              Constants.STARTUP_FALIED,
              "LoginServcice.refreshFundBalance :: Some exception occured due to : " + e.getCause(),
              mailAccount);
    } catch (KiteException e) {
      log.error("something went wrong in fetching total margin {}", e);
      MailSender
          .sendMail(Constants.TO_MAIL, Constants.TO_NAME,
              Constants.STARTUP_FALIED,
              "LoginServcice.refreshFundBalance :: Some exception occured due to : " + e.getCause(),
              mailAccount);
    }
  }

  private void handleSessionExpired() {
    log.error("session expired :: re-authenticate :: sending mail");
    MailSender.sendMail(Constants.TO_MAIL, Constants.TO_NAME, Constants.SESSION_EXPIRED_SUBJECT,
        getSessionExpiredMailBody(), mailAccount);
  }

  @Override
  public KiteConnect getSdkClient() {
    if (kiteSdk != null) {
      return kiteSdk;
    } else {
      handleSessionExpired();
    }
    return null;
  }

  private String getSessionExpiredMailBody() {
    return "Hi \n kite login session expired, please re-authenticate by clicking on the link :: \n"
        + getLoginUrl();
  }

  @Override
  public void destroySdkClient() {
    if (kiteSdk != null) {
      kiteSdk = null;
    }
  }
}

/**
 *
 */
package com.shaurya.intraday.trade.service;

import java.io.IOException;

import org.json.JSONException;

import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

/**
 * @author Shaurya
 *
 */
public interface LoginService {

  public String getLoginUrl();

  public void initializeSdkClient(String requestToken)
      throws JSONException, IOException, KiteException;

  public KiteConnect getSdkClient();

  public void destroySdkClient();

}

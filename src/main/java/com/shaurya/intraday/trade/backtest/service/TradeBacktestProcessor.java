/**
 *
 */
package com.shaurya.intraday.trade.backtest.service;

import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.StrategyModel;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import java.io.IOException;
import java.util.Date;
import org.json.JSONException;

/**
 * @author Shaurya
 *
 */
public interface TradeBacktestProcessor {

  public StrategyModel getTradeCall(Candle candle);

  public void initializeStrategyMap(Date date) throws IOException, KiteException, JSONException;

  public void destroyStrategyMap();
}

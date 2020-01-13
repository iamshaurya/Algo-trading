/**
 *
 */
package com.shaurya.intraday.trade.service;

import static com.shaurya.intraday.util.HelperUtil.getTradeQuantity;
import static com.shaurya.intraday.util.HelperUtil.isIntradayClosingTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.shaurya.intraday.constant.Constants;
import com.shaurya.intraday.enums.OrderStatusType;
import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.enums.StrategyType;
import com.shaurya.intraday.enums.TradeExitReason;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.MailAccount;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.strategy.OpenHighLowStrategy;
import com.shaurya.intraday.strategy.OpeningRangeBreakoutStrategy;
import com.shaurya.intraday.strategy.Strategy;
import com.shaurya.intraday.strategy.impl.OpenHighLowStrategyImpl;
import com.shaurya.intraday.strategy.impl.OpeningRangeBreakoutStrategyImpl;
import com.shaurya.intraday.util.HelperUtil;
import com.shaurya.intraday.util.MailSender;
import com.shaurya.intraday.util.StringUtil;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

/**
 * @author Shaurya
 *
 */
@Slf4j
@Service
public class TradeProcessorImpl implements TradeProcessor {

  private Map<String, Strategy> strategyMap;
  private Map<String, StrategyModel> metadataMap;
  private Calendar first15MinTime;
  @Autowired
  private TradeService tradeService;
  @Autowired
  private TradeOrderService tradeOrderService;
  @Autowired
  private MailAccount mailAccount;

  @Override
  public synchronized StrategyModel getTradeCall(Candle candle) {
    log.info("candle data " + candle.toString());
    try {
      StrategyModel openTrade = tradeService.fetchOpenTradeBySecurity(candle.getSecurity());
      StrategyModel tradeCall = strategyMap.get(candle.getSecurity())
          .processTrades(candle, openTrade, true);

      if (isIntradayClosingTime(candle.getTime())) {
        // square off all position
        if (openTrade != null) {
          if (tradeOrderService.getOrderStatus(openTrade) == OrderStatusType.OPEN) {
            tradeOrderService.placeExitCoverOrder(openTrade);
          }
          openTrade.setTradePrice(candle.getClose());
          tradeService.closeTrade(openTrade, TradeExitReason.CLOSING_TIME);
        }
      } else {
        if (tradeCall != null && tradeCall.isExitOrder()) {
          TradeExitReason reason = null;
          if (tradeOrderService.getOrderStatus(tradeCall) == OrderStatusType.OPEN) {
            tradeOrderService.placeExitCoverOrder(tradeCall);
            reason = HelperUtil.takeProfitReached(candle, openTrade)
                ? TradeExitReason.TAKE_PROFIT_REACHED
                : HelperUtil.stopLossReached(candle, openTrade) ? TradeExitReason.STOP_LOSS_REACHED
                    : TradeExitReason.STRATEGY_EXIT_CRITERIA_MET;
          } else {
            reason = TradeExitReason.HARD_STOP_LOSS_HIT;
          }
          tradeService.closeTrade(tradeCall, reason);

          tradeCall = (tradeCall = strategyMap.get(candle.getSecurity()).processTrades(candle, null,
              false)) != null ? tradeCall : null;
        }
        if (tradeCall != null) {
          Integer quantity =
              (metadataMap.get(tradeCall.getSecurity()).getQuantity() == 0) ? getTradeQuantity(
                  metadataMap.get(tradeCall.getSecurity()).getTradeMargin(),
                  tradeCall.getTradePrice(),
                  metadataMap.get(tradeCall.getSecurity()).getMarginMultiplier())
                  : metadataMap.get(tradeCall.getSecurity()).getQuantity();
          switch (tradeCall.getPosition()) {
            case LONG:
              if (isPreferedPosition(tradeCall)) {
                // make call for long cover order
                tradeCall.setQuantity(quantity);
                tradeCall = tradeOrderService.placeEntryCoverOrder(tradeCall);
                if (tradeCall.getOrderId() == null) { // handling
                  // failure
                  // case
                  String orderId = retryToFetchOrderId(tradeCall);
                  if (orderId == null) {
                    log.error("######### Place entry order failed ##############");
                    break;
                  } else {
                    log.error(
                        "Open trade call passed but response not received, order id fetched later: "
                            + orderId);
                    tradeCall.setOrderId(orderId);
                  }
                }
                tradeCall = tradeService.openTrade(tradeCall);
                log.info("Taking long position :: " + tradeCall.toString());
              }
              break;
            case SHORT:
              if (isPreferedPosition(tradeCall)) {
                // make call for short cover order
                tradeCall.setQuantity(quantity);
                tradeCall = tradeOrderService.placeEntryCoverOrder(tradeCall);
                if (tradeCall.getOrderId() == null) { // handling
                  // failure
                  // case
                  String orderId = retryToFetchOrderId(tradeCall);
                  if (orderId == null) {
                    log.error("######### Place entry order failed ##############");
                    break;
                  } else {
                    log.error(
                        "Open trade call passed but response not received, order id fetched later: "
                            + orderId);
                    tradeCall.setOrderId(orderId);
                  }
                }
                tradeCall = tradeService.openTrade(tradeCall);
                log.info("Taking short position :: " + tradeCall.toString());
              }
              break;
            default:
              break;
          }
        }
      }
    } catch (KiteException e) {
      log.error("Some exception occured due to : " + e.getCause());
      MailSender
          .sendMail(Constants.TO_MAIL, Constants.TO_NAME,
              Constants.KITE_EXCEPTION_TRADE_PROCESSOR,
              "TradeProcessorImpl.getTradeCall :: candle : "
                  + candle.toString() + " :: Some exception occured due to : " + e.getCause(),
              mailAccount);
    } catch (Exception e) {
      log.error("Some exception occured due to : {} ", e);
    }

    return null;
  }

  private boolean isPreferedPosition(StrategyModel tradeCall) {
    boolean isPreferedPosition = false;
    switch (metadataMap.get(tradeCall.getSecurity()).getPreferedPosition()) {
      case LONG:
        isPreferedPosition = tradeCall.getPosition() == PositionType.LONG;
        break;
      case SHORT:
        isPreferedPosition = tradeCall.getPosition() == PositionType.SHORT;
        break;
      case BOTH:
        isPreferedPosition = true;
        break;
      default:
        break;
    }
    return isPreferedPosition;
  }

  private String retryToFetchOrderId(StrategyModel model)
      throws JSONException, IOException, KiteException {
    OrderStatusType status = tradeOrderService.getOrderStatus(model);
    if (status != null && status == OrderStatusType.OPEN) {
      return tradeOrderService.getOrderId(model);
    }
    return null;
  }

  @Override
  public void initializeStrategyMap() throws IOException, KiteException, JSONException {
    Calendar cal = Calendar.getInstance();
    strategyMap = new HashMap<>();
    metadataMap = new HashMap<>();
    Map<StrategyModel, StrategyType> strategyTypeMap = tradeService.getTradeStrategy();
    for (Entry<StrategyModel, StrategyType> e : strategyTypeMap.entrySet()) {
      try {
        List<Candle> cList = null;
        e.getKey().setTradeMargin(e.getKey().getMarginPortion());
        switch (e.getValue()) {
          case EMA_MACD_RSI:
            //historical api not subscribed
            break;
          case EMA_RSI:
            //historical api not subscribed
            break;
          case MACD_RSI:
            //historical api not subscribed
            break;
          case MACD_HISTOGRAM:
            //historical api not subscribed
            break;
          case OPEN_HIGH_LOW:
            cList = new ArrayList<>();
            OpenHighLowStrategy ohl = new OpenHighLowStrategyImpl();
            ohl.initializeSetup(cList);
            strategyMap.put(e.getKey().getSecurity(), ohl);
            break;
          case HEIKIN_ASHI_OHL:
            //historical api not subscribed
            break;
          case OPENING_RANGE_BREAKOUT:
            cList = new ArrayList<>();
            OpeningRangeBreakoutStrategy orb = new OpeningRangeBreakoutStrategyImpl();
            orb.initializeSetup(cList);
            strategyMap.put(e.getKey().getSecurity(), orb);
            break;
          case GANN_SQUARE_9:
            //historical api not subscribed
            break;
          default:
            break;
        }
        metadataMap.put(e.getKey().getSecurity(), e.getKey());

      } catch (Exception ex) {
        log.error(
            "Error initializing historical data :: " + StringUtil.getStackTraceInStringFmt(ex));
        MailSender.sendMail(Constants.TO_MAIL, Constants.TO_NAME,
            Constants.KITE_EXCEPTION_TRADE_PROCESSOR,
            "Error initialiing historical data :: " + ex.getMessage() + "\n for : "
                + e.getKey().getSecurity(),
            mailAccount);
      }
    }
    // tradeService.deletePrevDayCandlesAndStrategy();
    cal.set(Calendar.HOUR_OF_DAY, 9);
    cal.set(Calendar.MINUTE, 30);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    first15MinTime = cal;
  }

  @Override
  public void destroyStrategyMap() {
    if (strategyMap != null) {
      for (Entry<String, Strategy> e : strategyMap.entrySet()) {
        e.getValue().destroySetup();
      }
      strategyMap = null;
    }
    metadataMap = null;
    first15MinTime = null;
  }

}

/**
 *
 */
package com.shaurya.intraday.trade.backtest.service;

import static com.shaurya.intraday.util.HelperUtil.isIntradayClosingTime;

import com.shaurya.intraday.constant.Constants;
import com.shaurya.intraday.entity.Performance;
import com.shaurya.intraday.entity.Trade;
import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.enums.StrategyType;
import com.shaurya.intraday.enums.TradeExitReason;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.MailAccount;
import com.shaurya.intraday.model.PreOpenModel;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.query.builder.TradeQueryBuilder;
import com.shaurya.intraday.repo.JpaRepo;
import com.shaurya.intraday.strategy.OpenHighLowStrategy;
import com.shaurya.intraday.strategy.OpeningRangeBreakoutStrategy;
import com.shaurya.intraday.strategy.OpeningRangeBreakoutV2Strategy;
import com.shaurya.intraday.strategy.Strategy;
import com.shaurya.intraday.strategy.SuperTrendStrategy;
import com.shaurya.intraday.strategy.impl.OpenHighLowStrategyImpl;
import com.shaurya.intraday.strategy.impl.OpeningRangeBreakoutStrategyImpl;
import com.shaurya.intraday.strategy.impl.OpeningRangeBreakoutV2StrategyImpl;
import com.shaurya.intraday.strategy.impl.PreOpenStrategyImpl;
import com.shaurya.intraday.strategy.impl.SuperTrendStrategyImpl;
import com.shaurya.intraday.trade.service.AccountService;
import com.shaurya.intraday.trade.service.TradeService;
import com.shaurya.intraday.util.HelperUtil;
import com.shaurya.intraday.util.MailSender;
import com.shaurya.intraday.util.StringUtil;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * @author Shaurya
 *
 */
@Slf4j
@Service
public class TradeBacktestProcessorImpl implements TradeBacktestProcessor {

  private Map<String, Strategy> strategyMap;
  private Map<String, StrategyModel> metadataMap;
  private Calendar first15MinTime;
  @Autowired
  private TradeService tradeService;
  @Autowired
  private MailAccount mailAccount;
  @Autowired
  private AccountService accountService;
  @Autowired
  private JpaRepo<Performance> performanceRepo;
  @Autowired
  private JpaRepo<Trade> tradeJpaRepo;

  @Override
  public StrategyModel getTradeCall(Candle candle) {
    log.info("candle data " + candle.toString());
    try {
      StrategyModel openTrade = tradeService.fetchOpenTradeBySecurity(candle.getSecurity());
      StrategyModel tradeCall = strategyMap.get(candle.getSecurity())
          .processTrades(candle, openTrade, true);

      if (isIntradayClosingTime(candle.getTime())) {
        log.error("Squaring of position - candle time {}  closing time {}", candle.getTime(),
            new Date());
        // square off all position
        if (openTrade != null) {
          openTrade.setTradePrice(candle.getClose());
          if (HelperUtil.stopLossReached(candle, openTrade)) {
            tradeService.closeTrade(openTrade, TradeExitReason.HARD_STOP_LOSS_HIT);
          } else {
            tradeService.closeTrade(openTrade, TradeExitReason.CLOSING_TIME);
          }
        }
      } else {
        if (tradeCall != null) {
          if (tradeCall.isExitOrder()) {
            TradeExitReason reason = null;
            reason = TradeExitReason.HARD_STOP_LOSS_HIT;
            openTrade.setTradePrice(candle.getClose());
            tradeService.closeTrade(tradeCall, reason);
            /*List<Trade> tradeList = tradeJpaRepo.findByQuery(
                TradeQueryBuilder.queryToFetchLatestBySecurityName(openTrade.getSecurity()));
            updatePerformance(Arrays.asList(tradeList.get(0)));*/
            tradeCall =
                (tradeCall = strategyMap.get(candle.getSecurity()).processTrades(candle, null,
                    false)) != null ? tradeCall : null;
          }

          if (tradeCall != null && tradeCall.isTrailSl()) {
            tradeCall.setExchangeType(metadataMap.get(candle.getSecurity()).getExchangeType());
            tradeService.updateTrailSlTrade(tradeCall);
            //no further trade hence making this null
            tradeCall = null;
          }
        }

        if (tradeCall != null) {
          tradeCall.setTradeDate(candle.getTime());
          Integer quantity =
              getQuantityAsPerRisk(accountService.getFund(), tradeCall.getSl(),
                  metadataMap.get(candle.getSecurity()).getLotSize(),
                  metadataMap.get(candle.getSecurity()).getMarginPortion());
          tradeCall.setExchangeType(metadataMap.get(candle.getSecurity()).getExchangeType());
          switch (tradeCall.getPosition()) {
            case LONG:
              if (isPreferedPosition(tradeCall) && quantity > 0) {
                // make call for long cover order
                tradeCall.setQuantity(quantity);
                tradeCall.setOrderId("123");
                if (tradeCall.getOrderId() == null) { // handling
                  // failure
                  // case
                  String orderId = "123";
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
              if (isPreferedPosition(tradeCall) && quantity > 0) {
                // make call for short cover order
                tradeCall.setQuantity(quantity);
                tradeCall.setOrderId("123");
                if (tradeCall.getOrderId() == null) { // handling
                  // failure
                  // case
                  String orderId = "123";
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
    } catch (Exception e) {
      log.error("Some exception occured due to : " + e.getCause());
      MailSender
          .sendMail(Constants.TO_MAIL, Constants.TO_NAME,
              Constants.KITE_EXCEPTION_TRADE_PROCESSOR,
              "TradeProcessorImpl.getTradeCall :: candle : "
                  + candle.toString() + " :: Some exception occured due to : " + e.getCause(),
              mailAccount);
    }

    return null;
  }

  public Integer getQuantityAsPerRisk(final Long equity, final Double slPoints,
      final Integer lotSize,
      final Double riskPerTradePer) {
    Double riskPerTrade = riskPerTradePer * equity;
    Integer quantity = (int) Math.floor(riskPerTrade / slPoints);
    if (lotSize != null) {
      Integer lots = (int) Math.floor(quantity / lotSize);
      quantity = lots * lotSize;
    }
    if (riskPerTrade > (0.01 * equity)) {
      System.out.println("Something wrong");
    }
    return quantity;
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

  private void updatePerformance(List<Trade> trades) throws IOException, KiteException {
    List<Performance> performanceList = performanceRepo
        .fetchByQuery(TradeQueryBuilder.queryToPerformance());
    if (!CollectionUtils.isEmpty(performanceList) && !CollectionUtils.isEmpty(trades)) {
      Performance performance = performanceList.get(0);
      int totalWinToday = 0;
      int totalLossToday = 0;
      double totalWinR = 0;
      double totalLossR = 0;
      double todayWinR = 0;
      double todayLossR = 0;
      double buyTradeAmount = 0;
      double sellTradeAmount = 0;
      double pl = 0;
      for (Trade t : trades) {
        buyTradeAmount += t.getTradeEntryPrice() * t.getQuantity();
        sellTradeAmount += t.getTradeExitPrice() * t.getQuantity();
        pl += t.getPl();
        if (t.getPl() > 0) {
          todayWinR += t.getRiskToReward();
          totalWinR += t.getRiskToReward();
          totalWinToday++;
        } else {
          todayLossR += t.getRiskToReward();
          totalLossR += t.getRiskToReward();
          totalLossToday++;
        }
      }

      double brokerageAndSlippage = brokerageChargeAndSlippage(buyTradeAmount, sellTradeAmount);
      Double currentEquity = accountService.getFund() + pl;
      currentEquity = currentEquity - brokerageAndSlippage;
      Double returnPer =
          ((currentEquity - performance.getStartingCapital()) / performance.getStartingCapital())
              * 100;
      performance.setCurrentCapital(currentEquity);
      performance.setReturnPercentage(returnPer);
      performance.setTotalWinningTrade(performance.getTotalWinningTrade() + totalWinToday);
      performance.setTotalWinningR(performance.getTotalWinningR() + totalWinR);
      performance
          .setAvgWinningR(performance.getTotalWinningTrade() == 0 ? 0
              : performance.getTotalWinningR() / performance.getTotalWinningTrade());
      performance.setTotalLosingTrade(performance.getTotalLosingTrade() + totalLossToday);
      performance.setTotalLosingR(performance.getTotalLosingR() + (totalLossR * -1));
      performance.setAvgLosingR(performance.getTotalLosingTrade() == 0 ? 0
          : performance.getTotalLosingR() / performance.getTotalLosingTrade());
      Double winRate =
          ((double) performance.getTotalWinningTrade() / (performance.getTotalWinningTrade()
              + performance.getTotalLosingTrade())) * 100;
      performance.setWinRate(winRate);
      Double edge = (performance.getWinRate() * performance.getAvgWinningR()) - (
          (100 - performance.getWinRate()) * performance.getAvgLosingR());
      edge = BigDecimal
          .valueOf(edge)
          .setScale(2, RoundingMode.HALF_UP).doubleValue();
      performance.setEdge(edge);
      //TODO: need to check how to calculate max drawdown
      //performance.setSharpeRatio();
      //performance.setMaxDrawDown();

      performanceRepo.update(performance);
      this.accountService.updateFundBalance(currentEquity.longValue());
    }
  }

  private double brokerageChargeAndSlippage(double buyTradePrice, double sellTradePrice) {
    double turnover = (buyTradePrice + sellTradePrice);
    double brokerage =
        Math.min((buyTradePrice * 0.0003), 20) + Math.min((sellTradePrice * 0.0003), 20);
    double stt = 0.0001 * (sellTradePrice);
    double transactionCharge = (0.000019 * buyTradePrice) + (0.000019 * sellTradePrice);
    double gst = 0.18 * (transactionCharge + brokerage);
    double sebiCharge = (0.0000015 * buyTradePrice) + (0.0000015 * sellTradePrice);
    double stampCharge = (0.00002 * buyTradePrice) + (0.00002 * sellTradePrice);

    double brokerageCharge = brokerage + stt + transactionCharge + gst + sebiCharge + stampCharge;
    return 1.5 * brokerageCharge;
  }


  @Override
  public void initializeStrategyMap(Date date) throws IOException, KiteException, JSONException {
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
          case OPENING_RANGE_BREAKOUT_V2:
            cList = new ArrayList<>();
            OpeningRangeBreakoutV2Strategy orbv2 = new OpeningRangeBreakoutV2StrategyImpl();
            orbv2.initializeSetup(cList);
            orbv2.setDeviationPercentage(e.getKey().getAtr());
            strategyMap.put(e.getKey().getSecurity(), orbv2);
            break;
          case GANN_SQUARE_9:
            //historical api not subscribed
            break;
          case SUPER_TREND:
            TreeSet<Candle> candleTreeSet = new TreeSet<>();
            fetchCandlesForAtr(e.getKey().getSecurity(), date, candleTreeSet);
            SuperTrendStrategy superTrendStrategy = new SuperTrendStrategyImpl();
            superTrendStrategy.initializeSetup(new ArrayList<>(candleTreeSet));
            strategyMap.put(e.getKey().getSecurity(), superTrendStrategy);
          case PRE_OPEN_GAPPER:
            cList = new ArrayList<>();
            PreOpenStrategyImpl preOpenStrategy = new PreOpenStrategyImpl();
            preOpenStrategy.initializeSetup(cList);
            strategyMap.put(e.getKey().getSecurity(), preOpenStrategy);
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
  public void updateStrategyMap(String stock, Double maxRange) {
    OpeningRangeBreakoutStrategyImpl openingRangeBreakoutStrategy = (OpeningRangeBreakoutStrategyImpl) strategyMap
        .get(stock);
    openingRangeBreakoutStrategy.updateMaxRange(maxRange);
  }

  private void fetchCandlesForAtr(String stock, Date date, TreeSet<Candle> candles)
      throws IOException {
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    cal.roll(Calendar.DAY_OF_YEAR, -1);
    while (!TickerGenerator.checkIfDataExistForDate(cal.getTime())) {
      cal.roll(Calendar.DAY_OF_YEAR, -1);
    }
    TreeSet<Candle> candleTreeSet = TickerGenerator.generateDayCandles(cal.getTime(), stock);
    candles.addAll(HelperUtil.form5MinCandle(candleTreeSet));
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

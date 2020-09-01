/**
 *
 */
package com.shaurya.intraday.trade.service;

import static com.shaurya.intraday.util.HelperUtil.getDayEndTime;
import static com.shaurya.intraday.util.HelperUtil.getDayStartTime;
import static com.shaurya.intraday.util.HelperUtil.getNthLastKeyEntry;
import static com.shaurya.intraday.util.HelperUtil.getPrevTradingDate;

import com.shaurya.intraday.entity.Performance;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.shaurya.intraday.builder.TradeBuilder;
import com.shaurya.intraday.constant.Constants;
import com.shaurya.intraday.entity.Trade;
import com.shaurya.intraday.entity.TradeStrategy;
import com.shaurya.intraday.enums.IntervalType;
import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.enums.StrategyType;
import com.shaurya.intraday.enums.TradeExitReason;
import com.shaurya.intraday.indicator.ADX;
import com.shaurya.intraday.indicator.ATR;
import com.shaurya.intraday.indicator.EMA;
import com.shaurya.intraday.indicator.MACD;
import com.shaurya.intraday.indicator.RSI;
import com.shaurya.intraday.model.ADXModel;
import com.shaurya.intraday.model.ATRModel;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.IndicatorValue;
import com.shaurya.intraday.model.MACDModel;
import com.shaurya.intraday.model.MailAccount;
import com.shaurya.intraday.model.RSIModel;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.query.builder.TradeQueryBuilder;
import com.shaurya.intraday.repo.JpaRepo;
import com.shaurya.intraday.util.MailSender;
import com.shaurya.intraday.util.StringUtil;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.HistoricalData;
import org.springframework.util.CollectionUtils;

/**
 * @author Shaurya
 *
 */
@Slf4j
@Service
public class TradeServiceImpl implements TradeService {

  @Autowired
  private JpaRepo<Performance> performanceRepo;
  @Autowired
  private JpaRepo<Trade> tradeRepo;
  @Autowired
  private JpaRepo<TradeStrategy> strategyRepo;
  @Autowired
  private LoginService loginService;
  @Autowired
  private MailAccount mailAccount;
  @Autowired
  private TradeOrderService tradeOrderService;
  private Map<Long, TreeSet<Candle>> monitorStockMap;

  /*
   * (non-Javadoc)
   *
   * @see
   * com.shaurya.intraday.trade.service.TradeService#openTrade(com.shaurya.
   * intraday.model.StrategyModel)
   */
  @Override
  public StrategyModel openTrade(StrategyModel model) {
    Trade openTrade = TradeBuilder.convert(model);
    openTrade = tradeRepo.update(openTrade);
    return TradeBuilder.reverseConvert(openTrade, true);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * com.shaurya.intraday.trade.service.TradeService#closeTrade(com.shaurya.
   * intraday.model.StrategyModel)
   */
  @Override
  public StrategyModel closeTrade(StrategyModel model, TradeExitReason reason) {
    Trade openTrade = fetchOpenTradeEntityBySecurity(model.getSecurity());
    if (openTrade == null) {
      throw new RuntimeException("no open trade found for " + model.toString());
    }
    Double exitPrice = model.getTradePrice();
    if (TradeExitReason.HARD_STOP_LOSS_HIT.equals(reason)) {
      exitPrice =
          PositionType.getEnumById(openTrade.getPositionType().intValue()).equals(PositionType.LONG)
              ?
              (openTrade.getTradeEntryPrice() - openTrade.getSl())
              : openTrade.getTradeEntryPrice() + openTrade.getSl();
    }
    Double pl = (exitPrice - openTrade.getTradeEntryPrice()) * openTrade.getQuantity();
    Double rr = (pl / openTrade.getRisk());
    openTrade.setTradeExitPrice(exitPrice);
    openTrade.setStatus((byte) 0);
    openTrade.setTradeExitReason(reason.getId());
    openTrade.setRiskToReward(rr);
    openTrade.setPl(pl);
    try {
      openTrade.setCurrentEquity(tradeOrderService.getTotalMargin());
    } catch (Exception | KiteException e) {
      log.error("error in fetching total margin when closing trade {} ", e);
    }
    openTrade = tradeRepo.update(openTrade);
    return TradeBuilder.reverseConvert(openTrade, false);
  }

  private Trade fetchOpenTradeEntityBySecurity(String securityName) {
    List<Trade> tradeList = tradeRepo
        .fetchByQuery(TradeQueryBuilder.queryToFetchOpenTradeBySecurityName(securityName));
    if (tradeList != null && !tradeList.isEmpty()) {
      return tradeList.get(0);
    }
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * com.shaurya.intraday.trade.service.TradeService#fetchOpenTradeBySecurity(
   * java.lang.String)
   */
  @Override
  public StrategyModel fetchOpenTradeBySecurity(String security) {
    Trade openTrade = fetchOpenTradeEntityBySecurity(security);
    if (openTrade == null) {
      log.error("no open trade found for name ::" + security);
      return null;
    }
    return TradeBuilder.reverseConvert(openTrade, true);
  }

  /*
   * (non-Javadoc)
   *
   * @see com.shaurya.intraday.trade.service.TradeService#getTradeStrategy()
   */
  @Override
  public Map<StrategyModel, StrategyType> getTradeStrategy() {
    Map<StrategyModel, StrategyType> sMap = new HashMap<>();
    List<TradeStrategy> strategyList = tradeRepo
        .fetchByQuery(TradeQueryBuilder.queryToFetchSecurityTradeStrategy());
    if (strategyList != null && !strategyList.isEmpty()) {
      for (TradeStrategy sl : strategyList) {
        StrategyModel model = new StrategyModel();
        model.setSecurity(sl.getSecurityName());
        model.setSecurityToken(sl.getSecurityToken());
        model.setPreferedPosition(PositionType.getEnumById(sl.getPreferedPosition()));
        model.setMarginMultiplier(sl.getMarginMultiplier());
        model.setMarginPortion(sl.getMarginPortion());
        model.setQuantity(sl.getQuantity());
        model.setLotSize(sl.getLotSize());
        model.setExchangeType(sl.getExchangeType());
        sMap.put(model, StrategyType.getEnumById(sl.getStrategyType()));
      }
    }
    return sMap;
  }

  @Override
  public Map<StrategyModel, StrategyType> getMonitorStrategy() {
    Map<StrategyModel, StrategyType> sMap = new HashMap<>();
    List<TradeStrategy> strategyList = tradeRepo
        .fetchByQuery(TradeQueryBuilder.queryToFetchSecurityMonitorStrategy());
    if (strategyList != null && !strategyList.isEmpty()) {
      for (TradeStrategy sl : strategyList) {
        StrategyModel model = new StrategyModel();
        model.setSecurity(sl.getSecurityName());
        model.setSecurityToken(sl.getSecurityToken());
        model.setPreferedPosition(PositionType.getEnumById(sl.getPreferedPosition()));
        model.setMarginMultiplier(sl.getMarginMultiplier());
        model.setMarginPortion(sl.getMarginPortion());
        model.setQuantity(sl.getQuantity());
        model.setLotSize(sl.getLotSize());
        model.setExchangeType(sl.getExchangeType());
        sMap.put(model, StrategyType.getEnumById(sl.getStrategyType()));
      }
    }
    return sMap;
  }

  @Override
  public Map<StrategyModel, StrategyType> getAllTradeStrategy() {
    Map<StrategyModel, StrategyType> sMap = new HashMap<>();
    List<TradeStrategy> strategyList = tradeRepo
        .fetchByQuery(TradeQueryBuilder.queryToFetchSecurityAllTradeStrategy());
    if (strategyList != null && !strategyList.isEmpty()) {
      for (TradeStrategy sl : strategyList) {
        StrategyModel model = new StrategyModel();
        model.setSecurity(sl.getSecurityName());
        model.setSecurityToken(sl.getSecurityToken());
        model.setPreferedPosition(PositionType.getEnumById(sl.getPreferedPosition()));
        model.setMarginMultiplier(sl.getMarginMultiplier());
        sMap.put(model, StrategyType.getEnumById(sl.getStrategyType()));
      }
    }
    return sMap;
  }

  @Override
  public Double checkBalance() throws IOException, KiteException {
    return tradeOrderService.getTotalMargin();
  }

  @Override
  public void updateStrategyStocks(List<StrategyModel> smList) {
    for (StrategyModel sm : smList) {
      TradeStrategy ts = TradeBuilder.convertStrategyModelToEntity(sm);
      strategyRepo.update(ts);
    }
  }

  @Override
  public List<Candle> getPrevDayCandles(Long instrumentToken, Date currentDate)
      throws IOException, KiteException {
    Map<Long, String> nameTokenMap = getNameTokenMap();
    List<Candle> cList = new ArrayList<>();
    Date from = getPrevTradingDate(currentDate);
    List<HistoricalData> hd;
    do {
      hd = loginService.getSdkClient()
          .getHistoricalData(from, currentDate, instrumentToken.toString(),
              IntervalType.MINUTE_1.getDesc(), false, false).dataArrayList;
      from = getPrevTradingDate(from);
    } while (hd == null || hd.size() <= 200);

    for (HistoricalData d : hd) {
      cList.add(
          TradeBuilder.convertHistoricalDataToCandle(d, nameTokenMap.get(instrumentToken),
              instrumentToken));
    }
    return cList;
  }

  @Override
  public List<Candle> getPrevDayCandles(Long instrumentToken, IntervalType interval, Date from,
      Date to,
      int candleCount) {
    Map<Long, String> nameTokenMap = getNameTokenMap();
    List<Candle> cList = new ArrayList<>();
    try {
      List<HistoricalData> hd;
      do {
        Thread.sleep(500);
        hd = loginService.getSdkClient().getHistoricalData(from, to, instrumentToken.toString(),
            interval.getDesc(), false, false).dataArrayList;
        from = getPrevTradingDate(from);
      } while (hd == null || hd.size() <= candleCount);
      for (HistoricalData d : hd) {
        cList.add(TradeBuilder.convertHistoricalDataToCandle(d, nameTokenMap.get(instrumentToken),
            instrumentToken));
      }
    } catch (KiteException | Exception e) {
      log.error("Error fetching historical data :: " + StringUtil.getStackTraceInStringFmt(e));
      MailSender
          .sendMail(Constants.TO_MAIL, Constants.TO_NAME,
              Constants.PREV_DAY_CANDLE, "Error fetching historical data :: " + e.getMessage()
                  + "\n for : " + instrumentToken + "\n from : " + from + "\n to : " + to,
              mailAccount);
    }
    return cList;
  }

  @Override
  public Map<Long, String> getNameTokenMap() {
    Map<Long, String> nameTokenMap = new HashMap<>();
    List<Object[]> nameTokenList = tradeRepo
        .runNativeQuery(TradeQueryBuilder.nativeQueryToFetchNameTokenMap());
    if (nameTokenList != null && !nameTokenList.isEmpty()) {
      for (Object[] nt : nameTokenList) {
        nameTokenMap.put(Long.parseLong(nt[1].toString()), nt[0].toString());
      }
    }
    return nameTokenMap;
  }

  @Override
  public void sendPNLStatement() throws IOException, KiteException {
    Map<String, List<Trade>> securityTradeMap = new HashMap<>();
    List<Trade> tradeList = tradeRepo.fetchByQuery(TradeQueryBuilder.queryToFetchDayTrades(
        getDateStringFormat(getDayStartTime().getTime()),
        getDateStringFormat(getDayEndTime().getTime())));
    updatePerformance(tradeList);
    for (Trade t : tradeList) {
      if (securityTradeMap.get(t.getSecurityName()) == null) {
        securityTradeMap.put(t.getSecurityName(), new ArrayList<>());
      }
      securityTradeMap.get(t.getSecurityName()).add(t);
    }
    int successfullTrade = 0;
    int unsuccessfullTrade = 0;
    double pnl = 0;
    for (Entry<String, List<Trade>> te : securityTradeMap.entrySet()) {
      double brokerage = 0;
      double scripPnl = 0;
      double buyTradeAmount = 0;
      double sellTradeAmount = 0;
      for (Trade t : te.getValue()) {
        double tradePnL = 0;
        switch (PositionType.getEnumById(t.getPositionType().intValue())) {
          case LONG:
            buyTradeAmount += t.getTradeEntryPrice() * t.getQuantity();
            sellTradeAmount += t.getTradeExitPrice() * t.getQuantity();
            tradePnL = ((t.getTradeExitPrice() - t.getTradeEntryPrice()) * t.getQuantity());
            scripPnl += tradePnL;
            if (tradePnL > 0) {
              successfullTrade++;
            } else {
              unsuccessfullTrade++;
            }
            break;
          case SHORT:
            sellTradeAmount += t.getTradeEntryPrice() * t.getQuantity();
            buyTradeAmount += t.getTradeExitPrice() * t.getQuantity();
            tradePnL = ((t.getTradeEntryPrice() - t.getTradeExitPrice()) * t.getQuantity());
            scripPnl += tradePnL;
            if (tradePnL > 0) {
              successfullTrade++;
            } else {
              unsuccessfullTrade++;
            }
            break;
          default:
            break;
        }
      }
      brokerage = brokerageCharge(buyTradeAmount, sellTradeAmount);
      scripPnl = scripPnl - brokerage;
      pnl += scripPnl;
    }
    MailSender.sendMail(Constants.TO_MAIL, Constants.TO_NAME, Constants.DAY_TRADE_SUMMARY,
        "Date : " + new Date() + "\n" + "Total Profit/Loss : " + pnl + "\n" + "Total trades : "
            + (successfullTrade + unsuccessfullTrade) + "\n" + "Succefull trades : "
            + successfullTrade
            + "\n" + "unsuccessfull trades : " + unsuccessfullTrade,
        mailAccount);
  }

  private void updatePerformance(List<Trade> trades) throws IOException, KiteException {
    List<Performance> performanceList = performanceRepo
        .fetchByQuery(TradeQueryBuilder.queryToPerformance());
    if (!CollectionUtils.isEmpty(performanceList) && !CollectionUtils.isEmpty(trades)) {
      Performance performance = performanceList.get(0);
      Double currentEquity = tradeOrderService.getTotalMargin();
      Double returnPer =
          ((currentEquity - performance.getStartingCapital()) / performance.getStartingCapital())
              * 100;
      performance.setCurrentCapital(currentEquity);
      performance.setReturnPercentage(returnPer);
      int totalWinToday = 0;
      int totalLossToday = 0;
      double totalWinR = 0;
      double totalLossR = 0;
      for (Trade t : trades) {
        if (t.getPl() > 0) {
          totalWinR += t.getRiskToReward();
          totalWinToday++;
        } else {
          totalLossR += t.getRiskToReward();
          totalLossToday++;
        }
      }
      performance.setTotalWinningTrade(performance.getTotalWinningTrade() + totalWinToday);
      performance.setTotalWinningR(performance.getTotalWinningR() + totalWinR);
      performance
          .setAvgWinningR(performance.getTotalWinningR() / performance.getTotalWinningTrade());
      performance.setTotalLosingTrade(performance.getTotalLosingTrade() + totalLossToday);
      performance.setTotalLosingR(performance.getTotalLosingR() + (totalLossR * -1));
      performance.setAvgLosingR(performance.getTotalLosingR() / performance.getTotalLosingTrade());
      Double winRate =
          ((double) performance.getTotalWinningTrade() / (performance.getTotalWinningTrade()
              + performance.getTotalLosingTrade())) * 100;
      performance.setWinRate(winRate);
      Double edge = (performance.getWinRate() * performance.getAvgWinningR()) - (
          (100 - performance.getWinRate()) * performance.getAvgLosingR());
      performance.setEdge(edge);
      //TODO: need to check how to calculate max drawdown
      //performance.setSharpeRatio();
      //performance.setMaxDrawDown();

      performanceRepo.update(performance);
    }
  }

  private double brokerageCharge(double buyTradePrice, double sellTradePrice) {
    double turnover = (buyTradePrice + sellTradePrice);
    double brokerage =
        Math.min((buyTradePrice * 0.0001), 20) + Math.min((sellTradePrice * 0.0001), 20);
    double stt = 0.00025 * (sellTradePrice);
    double transactionCharge = (0.0000325 * buyTradePrice) + (0.0000325 * sellTradePrice);
    double gst = 0.18 * (transactionCharge + brokerage);
    double sebiCharge = (0.0000015 * buyTradePrice) + (0.0000015 * sellTradePrice);
    double stampCharge = (0.00003 * buyTradePrice) + (0.00003 * sellTradePrice);

    return brokerage + stt + transactionCharge + gst + sebiCharge + stampCharge;
  }

  private String getDateStringFormat(Date refDate) {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String frmDate = format.format(refDate);
    return frmDate;
  }

  @Override
  public void testIndicator() throws IOException, KiteException {
    ATRModel atr;
    RSIModel rsi;
    MACDModel macd;
    ADXModel adx;
    TreeMap<Date, IndicatorValue> fastEmaMap;
    TreeMap<Date, IndicatorValue> slowEmaMap;
    TreeMap<Date, IndicatorValue> ema200Map;

    List<Candle> cList = getPrevDayCandles(5097729l, new Date());

    atr = ATR.calculateATR(cList, 14);
    rsi = RSI.calculateRSI(cList);
    adx = ADX.calculateADX(cList);
    fastEmaMap = EMA.calculateEMA(20, cList);
    slowEmaMap = EMA.calculateEMA(50, cList);
    macd = MACD.calculateMACD(fastEmaMap, slowEmaMap, 20);
    ema200Map = EMA.calculateEMA(200, cList);

    Date lastDataDate = getNthLastKeyEntry(macd.getMacdMap(), 1);
    log.error("sendInitSetupDataMail last date :: " + lastDataDate);
    IndicatorValue atrIv = atr.getAtrMap().lastEntry().getValue();
    IndicatorValue adxIv = adx.getAdx();
    IndicatorValue rsiIv = rsi.getRsiMap().lastEntry().getValue();
    IndicatorValue fastEma = fastEmaMap.lastEntry().getValue();
    IndicatorValue slowEma = slowEmaMap.lastEntry().getValue();
    IndicatorValue macdIv = macd.getMacdMap().lastEntry().getValue();
    IndicatorValue macdSignalIv = macd.getSignalMap().lastEntry().getValue();
    IndicatorValue ema200 = ema200Map.get(lastDataDate);
    log.error("sendInitSetupDataMail atr :: " + atrIv.toString());
    log.error("sendInitSetupDataMail adx :: " + adxIv.toString());
    log.error("sendInitSetupDataMail rsi :: " + rsiIv.toString());
    log.error("sendInitSetupDataMail fast ema :: " + fastEma.toString());
    log.error("sendInitSetupDataMail slow ema :: " + slowEma.toString());
    log.error("sendInitSetupDataMail macd :: " + macdIv.toString());
    log.error("sendInitSetupDataMail macd signal :: " + macdSignalIv.toString());
    log.error("sendInitSetupDataMail 200 ema :: " + ema200.toString());
    String mailbody =
        "ATR : " + atrIv.toString() + "\n" + "ADX : " + adxIv.toString() + "\n" + "RSI : "
            + rsiIv.toString() + "\n" + "fast ema : " + fastEma.toString() + "\n" + "slow ema : "
            + slowEma.toString() + "\n" + "macd : " + macdIv.toString() + "\n" + "macd signal : "
            + macdSignalIv.toString() + "\n" + "200 ema : " + ema200.toString();
    MailSender
        .sendMail(Constants.TO_MAIL, Constants.TO_NAME, Constants.MACD_RSI_STRATEGY_SETUP_DATA,
            mailbody,
            mailAccount);

  }

  @Override
  public void simulation(Long security) {
    /*List<Candle> cList = null;
    Calendar prevDayCalFrom = Calendar.getInstance();
    prevDayCalFrom.setTime(new Date());
    prevDayCalFrom.set(Calendar.HOUR_OF_DAY, 9);
    prevDayCalFrom.set(Calendar.MINUTE, 15);
    prevDayCalFrom.set(Calendar.SECOND, 0);
    Date from = prevDayCalFrom.getTime();
    Date to = new Date();
    cList = getPrevDayCandles(security, IntervalType.MINUTE_1, from, to, 200);
    for (Candle c : cList) {
      processor.getTradeCall(c);
    }*/
  }

  @Override
  public Map<String, Long> getTokenNameMap() {
    Map<String, Long> tokenNameMap = new HashMap<>();
    List<Object[]> nameTokenList = tradeRepo
        .runNativeQuery(TradeQueryBuilder.nativeQueryToFetchNameTokenMap());
    if (nameTokenList != null && !nameTokenList.isEmpty()) {
      for (Object[] nt : nameTokenList) {
        tokenNameMap.put(nt[0].toString(), Long.parseLong(nt[1].toString()));
      }
    }
    return tokenNameMap;

  }

  @Override
  public void recordMonitorStock(Candle candle) {
    if (monitorStockMap == null) {
      monitorStockMap = new HashMap<>();
    }
    if (monitorStockMap.get(candle.getToken()) == null) {
      monitorStockMap.put(candle.getToken(), new TreeSet<>());
    }
    TreeSet<Candle> candleSet = monitorStockMap.get(candle.getToken());
    candleSet.add(candle);
    monitorStockMap.put(candle.getToken(), candleSet);
  }

  @Override
  public Map<Long, TreeSet<Candle>> getMonitorStockMap() {
    return monitorStockMap;
  }

  @Override
  public void cleanUpMonitorStockMap() {
    monitorStockMap = null;
  }

  @Override
  public void updateAllStockToMonitorStock() {
    strategyRepo.runNativeQueryForUpdate(TradeQueryBuilder.queryToUpdateAllStockToMonitorStock());
  }

  @Override
  public void updateTradeStocks(List<Long> eligibleStocks, Double marginPortion) {
    strategyRepo.runNativeQueryForUpdate(
        TradeQueryBuilder.queryToUpdateTradeStock(eligibleStocks, marginPortion));
  }

}

package com.shaurya.intraday.trade.backtest.service;

import static com.shaurya.intraday.util.HelperUtil.getDayEndTime;
import static com.shaurya.intraday.util.HelperUtil.getDayStartTime;

import com.shaurya.intraday.entity.Performance;
import com.shaurya.intraday.entity.Trade;
import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.enums.StrategyType;
import com.shaurya.intraday.indicator.ADX;
import com.shaurya.intraday.indicator.ATR;
import com.shaurya.intraday.model.ADXModel;
import com.shaurya.intraday.model.ATRModel;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.PreOpenModel;
import com.shaurya.intraday.model.StockBeta;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.query.builder.TradeQueryBuilder;
import com.shaurya.intraday.repo.JpaRepo;
import com.shaurya.intraday.trade.service.AccountService;
import com.shaurya.intraday.trade.service.TradeService;
import com.shaurya.intraday.util.HelperUtil;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class BackTestServiceV2 {

  @Autowired
  private TradeService tradeService;
  @Autowired
  private TradeBacktestProcessor tradeBacktestProcessor;
  @Autowired
  private JpaRepo<Performance> performanceRepo;
  @Autowired
  private JpaRepo<Trade> tradeRepo;
  @Autowired
  private AccountService accountService;

  public void startSwingBacktest(Date startDate, Date endDate) throws IOException, KiteException {
    //accumulate data
    //run

    //validate date
    //updateStocksForTheDay
    Map<String, Double> stockList = updateStocksForTheDay(startDate);
    if (!CollectionUtils.isEmpty(stockList)) {
      //initialise setup
      tradeBacktestProcessor.initializeStrategyMap(startDate);
      //start trade
      List<String> stockListToTrade = stockList.entrySet()
          .stream()
          .map(m -> m.getKey())
          .collect(Collectors.toList());
      ExecutorService taskExecutor = Executors.newFixedThreadPool(10);
      ArrayBlockingQueue<String> queue =
          new ArrayBlockingQueue(stockListToTrade.size(), true, stockListToTrade);
      List<Future<?>> futures = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        BacktestWorker worker =
            new BacktestWorker(
                queue, tradeBacktestProcessor, startDate, null
            );
        futures.add(taskExecutor.submit(worker));
      }
      //wait
      while (true) {
        boolean allThreadsDone = true;
        for (Future future : futures) {
          if (!future.isDone()) {
            allThreadsDone = false;
          }
        }
        if (allThreadsDone) {
          break;
        }
      }
      //destroy setup
      tradeBacktestProcessor.destroyStrategyMap();
    }


  }

  public void startBacktest(Date startDate, Date endDate) throws IOException, KiteException {
    //validate date
    Date date = startDate;
    Date lastTradingDate = startDate;
    Calendar lastCal = Calendar.getInstance();
    lastCal.setTime(date);
    Calendar yearStartIn = Calendar.getInstance();
    yearStartIn.setTime(lastCal.getTime());
    yearStartIn.set(Calendar.MONTH, 0);
    yearStartIn.set(Calendar.DATE, 1);
    if (lastCal.getTime().after(yearStartIn.getTime())) {
      lastCal.roll(Calendar.DAY_OF_YEAR, -1);
    } else {
      lastCal.roll(Calendar.YEAR, -1);
      lastCal.set(Calendar.MONTH, 11);
      lastCal.set(Calendar.DATE, 31);
    }
    while (!TickerGenerator.checkIfDataExistForDate(lastCal.getTime())) {
      yearStartIn.setTime(lastCal.getTime());
      yearStartIn.set(Calendar.MONTH, 0);
      yearStartIn.set(Calendar.DATE, 1);
      if (lastCal.getTime().after(yearStartIn.getTime())) {
        lastCal.roll(Calendar.DAY_OF_YEAR, -1);
      } else {
        lastCal.roll(Calendar.YEAR, -1);
        lastCal.set(Calendar.MONTH, 11);
        lastCal.set(Calendar.DATE, 31);
      }
    }
    lastTradingDate = lastCal.getTime();
    while (date.before(endDate)) {
      if (!TickerGenerator.checkIfDataExistForDate(date)) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.roll(Calendar.DAY_OF_YEAR, 1);
        date = cal.getTime();
        continue;
      }
      //updateStocksForTheDay
      Map<String, Double> stockList = updateStocksForTheDay(date);
      if (!CollectionUtils.isEmpty(stockList)) {
        //initialise setup
        tradeBacktestProcessor.initializeStrategyMap(date);
        //tradeBacktestProcessor.updateStrategyMap(stockList);
        //start trade
        List<String> stockListToTrade = stockList.entrySet()
            .stream()
            .map(m -> m.getKey())
            .collect(Collectors.toList());
        ExecutorService taskExecutor = Executors.newFixedThreadPool(10);
        ArrayBlockingQueue<String> queue =
            new ArrayBlockingQueue(stockListToTrade.size(), true, stockListToTrade);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
          BacktestWorker worker =
              new BacktestWorker(
                  queue, tradeBacktestProcessor, date, lastTradingDate
              );
          futures.add(taskExecutor.submit(worker));
        }
        //wait
        while (true) {
          boolean allThreadsDone = true;
          for (Future future : futures) {
            if (!future.isDone()) {
              allThreadsDone = false;
            }
          }
          if (allThreadsDone) {
            break;
          }
        }
        //destroy setup
        tradeBacktestProcessor.destroyStrategyMap();
        //pnl update
        List<Trade> tradeList = tradeRepo.fetchByQuery(TradeQueryBuilder.queryToFetchDayTrades(
            getDateStringFormat(getDayStartTime(date).getTime()),
            getDateStringFormat(getDayEndTime(date).getTime())));
        updatePerformance(tradeList);
      }
      //increment date
      Calendar cal = Calendar.getInstance();
      cal.setTime(date);
      cal.roll(Calendar.DAY_OF_YEAR, 1);
      date = cal.getTime();
    }
  }

  private String getDateStringFormat(Date refDate) {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String frmDate = format.format(refDate);
    return frmDate;
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

  private Map<String, Double> updateStocksForTheDay(Date date) throws IOException {
    Calendar yearStart = Calendar.getInstance();
    yearStart.setTime(date);
    yearStart.set(Calendar.MONTH, 0);
    yearStart.set(Calendar.DATE, 1);
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    if (date.after(yearStart.getTime())) {
      cal.roll(Calendar.DAY_OF_YEAR, -1);
    } else {
      cal.roll(Calendar.YEAR, -1);
      cal.set(Calendar.MONTH, 11);
      cal.set(Calendar.DATE, 31);
    }

    while (!TickerGenerator.checkIfDataExistForDate(cal.getTime())) {
      Calendar yearStartIn = Calendar.getInstance();
      yearStartIn.setTime(cal.getTime());
      yearStartIn.set(Calendar.MONTH, 0);
      yearStartIn.set(Calendar.DATE, 1);
      if (cal.getTime().after(yearStartIn.getTime())) {
        cal.roll(Calendar.DAY_OF_YEAR, -1);
      } else {
        cal.roll(Calendar.YEAR, -1);
        cal.set(Calendar.MONTH, 11);
        cal.set(Calendar.DATE, 31);
      }
    }
    List<String> stockNameList = TickerGenerator.getStockListForDate(cal.getTime());
    Map<String, Double> stockList = getPreopenBuzzers(date, cal.getTime(), stockNameList);
    //Map<String, Double> stockList = getFilteredStockListByPreopen(preOpenBuzzers, cal.getTime());
    /*Map<String, Double> stockList= new HashMap<>();
    stockList.put("BANKNIFTY", 2.5);*/
    tradeService.updateAllStockToMonitorStock();
    updateStocksToTrade(stockList);
    return stockList;
  }

  private Double getPrevDayMaxRange(Date lastTradingDate, String stock) throws IOException {
    Candle lastTradingDayCandle = TickerGenerator.generateDayCandle(lastTradingDate, stock);
    if (lastTradingDayCandle != null && lastTradingDayCandle.getClose() >= 50.0) {
      Double prevHigh = lastTradingDayCandle.getHigh();
      Double prevLow = lastTradingDayCandle.getLow();
      Double prevOpen = lastTradingDayCandle.getOpen();
      Double maxRange = Math.max((prevHigh - prevOpen), (prevOpen - prevLow));
      return (maxRange / lastTradingDayCandle.getClose()) * 100;
    }
    return null;
  }

  private Map<String, Double> getPreopenBuzzers(Date today, Date yesterday,
      List<String> stockNameList)
      throws IOException {
    Map<String, Long> tokenNameMap = tradeService.getTokenNameMap();
    Map<StrategyModel, StrategyType> strategyTypeMap = tradeService.getAllTradeStrategy();
    Map<String, Double> highBetaStockList = new HashMap<>();
    for (Entry<StrategyModel, StrategyType> e : strategyTypeMap.entrySet()) {
      if (e.getKey().getAtr() > 0.0) {
        highBetaStockList.put(e.getKey().getSecurity(), e.getKey().getAtr());
      }
    }

    List<String> preOpenBuzzerNames = new ArrayList<>();
    List<StockBeta> preOpenBuzzers = new ArrayList<>();
    for (String stock : stockNameList) {
      //PreOpenModel preOpenModel = getPreOpenPer(stock, today, yesterday);
      if (highBetaStockList.get(stock) != null) {
        Boolean volumeSurge = breaksPrevDayRange(yesterday, today, stock);
        if (volumeSurge && highBetaStockList.get(stock) != null) {
          preOpenBuzzers.add(new StockBeta(highBetaStockList.get(stock), stock, null));
        }
      }
    }
    Collections.sort(preOpenBuzzers);
    Map<String, PreOpenModel> filteredStocks = new HashMap<>();
    int i = 0;
    for (StockBeta e : preOpenBuzzers) {
      if (i >= 10) {
        break;
      }
      if (highBetaStockList.get(e.getName()) != null && e.getBeta() > 0.0) {
        filteredStocks.put(e.getName(), e.getPreOpenModel());
        i++;
      }

    }

    Map<String, Double> filteredStocksMap = new HashMap<>();
    for (Entry<String, PreOpenModel> stock : filteredStocks.entrySet()) {
      if (highBetaStockList.get(stock.getKey()) != null) {
        filteredStocksMap.put(stock.getKey(), highBetaStockList.get(stock.getKey()));
      }

    }

    return filteredStocksMap;
  }

  private boolean breaksPrevDayRange(Date yesterday, Date today, String stock) throws IOException {
    Candle yesterdayCandle = TickerGenerator.generateDayCandle(yesterday, stock);
    if (yesterdayCandle != null) {
      Candle first5minCandle = TickerGenerator.generateFirstNminCandle(today, stock, 5);
      if (first5minCandle.getClose() > yesterdayCandle.getHigh()
          || first5minCandle.getClose() < yesterdayCandle.getLow()) {
        return true;
      }
    }
    return false;
  }

  private Boolean isVolumeSurge(Date yesterday, String stock) throws IOException {
    TreeSet<Candle> candleSet = new TreeSet<>();
    candleSet.add(TickerGenerator.generateDayCandle(yesterday, stock));
    Calendar cal = Calendar.getInstance();
    cal.setTime(yesterday);
    cal.roll(Calendar.DAY_OF_YEAR, -1);
    for (int i = 1; i < 10; i++) {
      while (!TickerGenerator.checkIfDataExistForDate(cal.getTime())) {
        Calendar yearStartIn = Calendar.getInstance();
        yearStartIn.setTime(cal.getTime());
        yearStartIn.set(Calendar.MONTH, 0);
        yearStartIn.set(Calendar.DATE, 1);
        if (cal.getTime().after(yearStartIn.getTime())) {
          cal.roll(Calendar.DAY_OF_YEAR, -1);
        } else {
          cal.roll(Calendar.YEAR, -1);
          cal.set(Calendar.MONTH, 11);
          cal.set(Calendar.DATE, 31);
        }
      }
      candleSet.add(TickerGenerator.generateDayCandle(cal.getTime(), stock));
      cal.roll(Calendar.DAY_OF_YEAR, -1);
    }

    Double latestVol = candleSet.last().getVolume();
    Double volSma10Sum = 0.0;
    for (Candle c : candleSet) {
      volSma10Sum += c.getVolume();
    }
    Double volSma10 = volSma10Sum / 10;
    return latestVol >= volSma10;
  }

  private PreOpenModel getPreOpenPer(String stock, Date today, Date yesterday) throws IOException {

    try {
      if (TickerGenerator.checkIfDataExistForDateAndStock(stock, today) && TickerGenerator
          .checkIfDataExistForDateAndStock(stock, yesterday)) {
        Candle yesterdayPostCloseCandle = TickerGenerator
            .parseAndGeneratePostCloseCandle(stock, yesterday);
        Candle todayPreOpenCandle = TickerGenerator.parseAndGeneratePreOpenCandle(stock, today);
        Candle yesterdayCandle = TickerGenerator.generateDayCandle(yesterday, stock);
        if (yesterdayCandle != null && yesterdayPostCloseCandle != null
            && todayPreOpenCandle != null
            && todayPreOpenCandle.getOpen() >= 50.0) {
          Double yesterdayClose = yesterdayPostCloseCandle
              .getClose();
          Double preOpenValue = todayPreOpenCandle.getOpen();
          Double preOpenPer = Math.abs(((preOpenValue - yesterdayClose) / yesterdayClose) * 100);
          PreOpenModel preOpenModel = new PreOpenModel();
          preOpenModel.setChange(preOpenPer);
          /*if (preOpenValue > yesterdayCandle.getHigh()) {
            preOpenModel.setIsFullGapUp(Boolean.TRUE);
            return preOpenModel;
          }
          if (preOpenValue < yesterdayCandle.getLow()) {
            preOpenModel.setIsFullGapUp(Boolean.FALSE);
            return preOpenModel;
          }*/
          return preOpenModel;
        } else {
          System.out.println(
              "either open price less than 50 or yesterdayPostCloseCandle or todayPreOpenCandle is null for "
                  + stock
                  + "  todays date "
                  + today);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Something went wrong for : " + stock + " date : " + today);
      throw e;
    }
    return null;
  }

  private Map<String, Double> getFilteredStockList(List<String> stockNameList, Date date)
      throws IOException {
    Map<String, Long> tokenNameMap = tradeService.getTokenNameMap();
    Map<StrategyModel, StrategyType> strategyTypeMap = tradeService.getAllTradeStrategy();
    Map<String, Double> highBetaStockList = new HashMap<>();
    for (Entry<StrategyModel, StrategyType> e : strategyTypeMap.entrySet()) {
      if (e.getKey().getAtr() > 0.0) {
        highBetaStockList.put(e.getKey().getSecurity(), e.getKey().getAtr());
      }
    }
    Map<String, Double> filteredStocks = new HashMap<>();
    TreeSet<StockBeta> stockBetas = new TreeSet<>();
    for (String stock : stockNameList) {
      if (tokenNameMap.get(stock) != null) {
        Double atr = calculateAtrPer(stock, date);
        if (atr > 0.0 && highBetaStockList.get(stock) != null) {
          stockBetas.add(new StockBeta(highBetaStockList.get(stock), stock, null));
        }
      }
    }

    int i = 0;
    for (StockBeta e : stockBetas) {
      if (i >= 10) {
        break;
      }
      filteredStocks.put(e.getName(), e.getBeta());
      i++;
    }
    return filteredStocks;
  }

  private Map<String, Double> getFilteredStockListByPreopen(List<String> stockNameList, Date date)
      throws IOException {
    Map<String, Long> tokenNameMap = tradeService.getTokenNameMap();
    Map<StrategyModel, StrategyType> strategyTypeMap = tradeService.getAllTradeStrategy();
    Map<String, Double> highBetaStockList = new HashMap<>();
    for (Entry<StrategyModel, StrategyType> e : strategyTypeMap.entrySet()) {
      if (e.getKey().getAtr() > 0.0) {
        highBetaStockList.put(e.getKey().getSecurity(), e.getKey().getAtr());
      }
    }
    Map<String, Double> filteredStocks = new HashMap<>();
    TreeSet<StockBeta> stockBetas = new TreeSet<>();
    for (String stock : stockNameList) {
      if (tokenNameMap.get(stock) != null) {
        if (highBetaStockList.get(stock) != null) {
          stockBetas.add(new StockBeta(highBetaStockList.get(stock), stock, null));
        }
      }
    }

    int i = 0;
    for (StockBeta e : stockBetas) {
      if (i >= 10) {
        break;
      }
      filteredStocks.put(e.getName(), e.getBeta());
      i++;
    }
    return filteredStocks;
  }


  private Double calculateAtrPer(String stock, Date date) throws IOException {
    TreeSet<Candle> candles = new TreeSet<>();
    try {
      fetchCandlesForAtr(stock, date, candles, 0);
    } catch (Exception e) {
      e.printStackTrace();
      return 0.0;
    }
    List<Candle> candleList = new ArrayList<>(candles);
    if (candleList.get(candleList.size() - 1).getClose() < 50) {
      return 0.0;
    }
    ATRModel atrModel = ATR.calculateATR(candleList, 14);
    if (atrModel.getAtrSignal().lastEntry().getValue().getIndicatorValue() < atrModel
        .getAtrSignalSlow().lastEntry().getValue().getIndicatorValue()) {
      return 0.0;
    }
    return atrModel.getAtrMap().get(date).getIndicatorValue();
  }

  private void fetchCandlesForAtr(String stock, Date date, TreeSet<Candle> candles, int count)
      throws IOException {
    if (count >= 23) {
      return;
    }
    if (!TickerGenerator.checkIfDataExistForDate(date)) {
      Calendar cal = Calendar.getInstance();
      cal.setTime(date);
      cal.roll(Calendar.DAY_OF_YEAR, -1);
      fetchCandlesForAtr(stock, cal.getTime(), candles, count);
    } else {
      Candle candle = TickerGenerator.generateDayCandle(date, stock);
      candles.add(candle);
      Calendar cal = Calendar.getInstance();
      cal.setTime(date);
      cal.roll(Calendar.DAY_OF_YEAR, -1);
      fetchCandlesForAtr(stock, cal.getTime(), candles, ++count);
    }
  }

  private void fetchCandlesForAdx(String stock, Date date, TreeSet<Candle> candles, int count)
      throws IOException {
    if (count >= 30) {
      return;
    }
    if (!TickerGenerator.checkIfDataExistForDate(date)) {
      Calendar cal = Calendar.getInstance();
      cal.setTime(date);
      cal.roll(Calendar.DAY_OF_YEAR, -1);
      fetchCandlesForAdx(stock, cal.getTime(), candles, count);
    } else {
      Candle candle = TickerGenerator.generateDayCandle(date, stock);
      candles.add(candle);
      Calendar cal = Calendar.getInstance();
      cal.setTime(date);
      cal.roll(Calendar.DAY_OF_YEAR, -1);
      fetchCandlesForAdx(stock, cal.getTime(), candles, ++count);
    }
  }

  private void updateStocksToTrade(Map<String, Double> stockList) {
    for (Entry<String, Double> e : stockList.entrySet()) {
      tradeService.updateTradeStocks(e.getKey(), e.getValue(), 0.005);
    }
  }
}

package com.shaurya.intraday.trade.backtest.service;

import static com.shaurya.intraday.util.HelperUtil.getDayEndTime;
import static com.shaurya.intraday.util.HelperUtil.getDayStartTime;

import com.shaurya.intraday.entity.Performance;
import com.shaurya.intraday.entity.Trade;
import com.shaurya.intraday.enums.StrategyType;
import com.shaurya.intraday.indicator.ADX;
import com.shaurya.intraday.indicator.ATR;
import com.shaurya.intraday.model.ADXModel;
import com.shaurya.intraday.model.ATRModel;
import com.shaurya.intraday.model.Candle;
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

  public void startBacktest(Date startDate, Date endDate) throws IOException, KiteException {
    //validate date
    Date date = startDate;
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
      //initialise setup
      tradeBacktestProcessor.initializeStrategyMap(date);
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
                queue, tradeBacktestProcessor, date
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
      for (Trade t : trades) {
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
      Double currentEquity =
          accountService.getFund() * (1 + ((todayWinR + todayLossR) * 0.005));
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
    tradeService.updateAllStockToMonitorStock();
    updateStocksToTrade(stockList);
    return stockList;
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
    TreeSet<StockBeta> preOpenBuzzers = new TreeSet<>();
    for (String stock : stockNameList) {
      preOpenBuzzers.add(new StockBeta(getPreOpenPer(stock, today, yesterday), stock, 0.0));
    }

    int i = 0;
    for (StockBeta e : preOpenBuzzers) {
      if (i >= 10) {
        break;
      }
      if (highBetaStockList.get(e.getName()) != null) {
        preOpenBuzzerNames.add(e.getName());
        i++;
      }

    }

    Map<String, Double> filteredStocks = new HashMap<>();
    for (String stock : preOpenBuzzerNames) {
      if (tokenNameMap.get(stock) != null) {
        if (highBetaStockList.get(stock) != null) {
          filteredStocks.put(stock, highBetaStockList.get(stock));
        }
      }
    }
    return filteredStocks;
  }

  private Double getPreOpenPer(String stock, Date today, Date yesterday) throws IOException {
    try {
      if (TickerGenerator.checkIfDataExistForDateAndStock(stock, today) && TickerGenerator
          .checkIfDataExistForDateAndStock(stock, yesterday)) {
        Candle yesterdayPostCloseCandle = TickerGenerator
            .parseAndGeneratePostCloseCandle(stock, yesterday);
        Candle todayPreOpenCandle = TickerGenerator.parseAndGeneratePreOpenCandle(stock, today);
        if (yesterdayPostCloseCandle != null && todayPreOpenCandle != null
            && todayPreOpenCandle.getOpen() >= 50.0) {
          Double yesterdayClose = yesterdayPostCloseCandle
              .getClose();
          Double preOpenValue = todayPreOpenCandle.getOpen();
          return Math.abs(((preOpenValue - yesterdayClose) / yesterdayClose) * 100);
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
    return 0.0;
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
          stockBetas.add(new StockBeta(highBetaStockList.get(stock), stock, 0.0));
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
          stockBetas.add(new StockBeta(highBetaStockList.get(stock), stock, 0.0));
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

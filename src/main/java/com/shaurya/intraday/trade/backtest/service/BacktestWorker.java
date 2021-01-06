package com.shaurya.intraday.trade.backtest.service;

import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.util.StringUtil;
import java.io.IOException;
import java.util.Date;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;

public class BacktestWorker implements Runnable {

  private final ArrayBlockingQueue<String> queue;
  private final TradeBacktestProcessor tradeBacktestProcessor;
  private final Date tradeDate;
  private final Date lastTradingDate;

  public BacktestWorker(ArrayBlockingQueue<String> queue,
      TradeBacktestProcessor tradeBacktestProcessor, Date tradeDate, Date lastTradingDate) {
    this.queue = queue;
    this.tradeBacktestProcessor = tradeBacktestProcessor;
    this.tradeDate = tradeDate;
    this.lastTradingDate = lastTradingDate;
  }


  @Override
  public void run() {
    while (queue.peek() != null) {

      try {
        String entity = queue.poll();
        TreeSet<Candle> ticker = TickerGenerator.generateTicker(this.tradeDate, entity);
        for (Candle c : ticker) {
          tradeBacktestProcessor.getTradeCall(c);
        }
      } catch (IOException e) {
        System.out
            .println("error in performing backtest " + StringUtil.getStackTraceInStringFmt(e));
      }

    }
  }
}

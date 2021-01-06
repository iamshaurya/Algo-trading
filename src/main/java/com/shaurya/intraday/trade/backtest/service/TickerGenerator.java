package com.shaurya.intraday.trade.backtest.service;

import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.util.HelperUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

public class TickerGenerator {

  private static final String dataDumpPath = "/Users/shaurya/Downloads/nifty-data-dump/";

  public static List<String> getStockListForDate(Date date) {
    List<String> stockNameList = new ArrayList<>();
    SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy");
    SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM");
    SimpleDateFormat sdfMonthDate = new SimpleDateFormat("ddMMM");
    String year = sdfYear.format(date).toUpperCase();
    String month = sdfMonth.format(date).toUpperCase();
    String monthDate = sdfMonthDate.format(date).toUpperCase();

    String dataLocation =
        dataDumpPath + year + "/" + month + "/" + monthDate;
    File[] directoryListing = new File(dataLocation).listFiles();
    if (directoryListing != null) {
      for (File child : directoryListing) {
        if (!child.getName().matches(".*\\d.*") && !child.getName().contains("CNX") && !child
            .getName().contains("NIFTY")) {
          stockNameList.add(child.getName().split("\\.")[0]);
        }
      }
    }
    return stockNameList;
  }

  public static boolean checkIfDataExistForDate(Date date) {
    SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy");
    SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM");
    SimpleDateFormat sdfMonthDate = new SimpleDateFormat("ddMMM");
    String year = sdfYear.format(date).toUpperCase();
    String month = sdfMonth.format(date).toUpperCase();
    String monthDate = sdfMonthDate.format(date).toUpperCase();

    String dataLocation =
        dataDumpPath + year + "/" + month + "/" + monthDate;
    return new File(dataLocation).exists();
  }

  public static boolean checkIfDataExistForDateAndStock(String stock, Date date) {
    SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy");
    SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM");
    SimpleDateFormat sdfMonthDate = new SimpleDateFormat("ddMMM");
    String year = sdfYear.format(date).toUpperCase();
    String month = sdfMonth.format(date).toUpperCase();
    String monthDate = sdfMonthDate.format(date).toUpperCase();

    String dataLocation =
        dataDumpPath + year + "/" + month + "/" + monthDate + "/" + stock + ".txt";
    return new File(dataLocation).exists();
  }

  public static Candle generateDayCandle(Date date, String symbol) throws IOException {
    SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy");
    SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM");
    SimpleDateFormat sdfMonthDate = new SimpleDateFormat("ddMMM");
    String year = sdfYear.format(date).toUpperCase();
    String month = sdfMonth.format(date).toUpperCase();
    String monthDate = sdfMonthDate.format(date).toUpperCase();

    String dataLocation =
        dataDumpPath + year + "/" + month + "/" + monthDate + "/" + symbol + ".txt";
    TreeSet<Candle> candles = parseAndGenerateCandleList(dataLocation, date);
    return HelperUtil.formDayCandle(candles);
  }

  public static Candle generateFirstNminCandle(Date date, String symbol, int n) throws IOException {
    SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy");
    SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM");
    SimpleDateFormat sdfMonthDate = new SimpleDateFormat("ddMMM");
    String year = sdfYear.format(date).toUpperCase();
    String month = sdfMonth.format(date).toUpperCase();
    String monthDate = sdfMonthDate.format(date).toUpperCase();

    String dataLocation =
        dataDumpPath + year + "/" + month + "/" + monthDate + "/" + symbol + ".txt";
    TreeSet<Candle> candles = parseAndGenerateCandleList(dataLocation, date);
    TreeSet<Candle> candlesSubset = new TreeSet<>();
    int i = n;
    for (Candle c : candles) {
      if (i <= 0) {
        break;
      }
      candlesSubset.add(c);
      i--;
    }
    return form5MinCandle(candlesSubset);
  }

  private static Candle form5MinCandle(TreeSet<Candle> candleSet) {
    Candle candle5min = null;
    if (candleSet.size() == 5) {
      int i = 0;
      Iterator<Candle> cItr = candleSet.iterator();
      while (cItr.hasNext()) {
        Candle c = cItr.next();
        if (i == 0) {
          candle5min = new Candle(c.getSecurity(), c.getToken(), c.getTime(), c.getOpen(),
              c.getHigh(),
              c.getLow(), c.getClose(), 0);
        } else {
          candle5min.setClose(c.getClose());
          candle5min.setHigh(Math.max(candle5min.getHigh(), c.getHigh()));
          candle5min.setLow(Math.min(candle5min.getLow(), c.getLow()));
        }
        i++;
      }
      candleSet.clear();
    }
    return candle5min;
  }

  public static TreeSet<Candle> generateDayCandles(Date date, String symbol) throws IOException {
    SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy");
    SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM");
    SimpleDateFormat sdfMonthDate = new SimpleDateFormat("ddMMM");
    String year = sdfYear.format(date).toUpperCase();
    String month = sdfMonth.format(date).toUpperCase();
    String monthDate = sdfMonthDate.format(date).toUpperCase();

    String dataLocation =
        dataDumpPath + year + "/" + month + "/" + monthDate + "/" + symbol + ".txt";
    TreeSet<Candle> candles = parseAndGenerateCandleList(dataLocation, date);
    return candles;
  }


  public static TreeSet<Candle> generateTicker(Date date, String symbol) throws IOException {
    SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy");
    SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM");
    SimpleDateFormat sdfMonthDate = new SimpleDateFormat("ddMMM");
    String year = sdfYear.format(date).toUpperCase();
    String month = sdfMonth.format(date).toUpperCase();
    String monthDate = sdfMonthDate.format(date).toUpperCase();

    String dataLocation =
        dataDumpPath + year + "/" + month + "/" + monthDate + "/" + symbol + ".txt";
    return parseAndGenerateCandleList(dataLocation, date);
  }

  //symbol, date, time, open, high, low, close, volume
  private static TreeSet<Candle> parseAndGenerateCandleList(String filePath, Date date)
      throws IOException {
    Calendar startCal = Calendar.getInstance();
    startCal.setTime(date);
    startCal.set(Calendar.HOUR_OF_DAY, 9);
    startCal.set(Calendar.MINUTE, 14);
    Calendar endCal = Calendar.getInstance();
    endCal.setTime(date);
    endCal.set(Calendar.HOUR_OF_DAY, 15);
    endCal.set(Calendar.MINUTE, 31);
    TreeSet<Candle> candleList = new TreeSet<>();
    File file = new File(filePath);
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(file));
      String st;
      while ((st = br.readLine()) != null) {
        String[] dataArr = st.split(",");
        String[] time = dataArr[2].split(":");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time[0]));
        cal.set(Calendar.MINUTE, Integer.parseInt(time[1]));
        if (cal.getTime().after(startCal.getTime()) && cal.getTime().before(endCal.getTime())) {
          Candle candle = new Candle(dataArr[0], 0, cal.getTime(), Double.parseDouble(dataArr[3]),
              Double.parseDouble(dataArr[4]), Double.parseDouble(dataArr[5]),
              Double.parseDouble(dataArr[6]), Double.parseDouble(dataArr[7]));
          candleList.add(candle);
        }
      }
    } finally {
      if (br != null) {
        br.close();
      }
    }
    return candleList;
  }

  //symbol, date, time, open, high, low, close, volume
  public static Candle parseAndGeneratePreOpenCandle(String symbol, Date date)
      throws IOException {
    Candle noPreOpenCandle = null;
    SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy");
    SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM");
    SimpleDateFormat sdfMonthDate = new SimpleDateFormat("ddMMM");
    String year = sdfYear.format(date).toUpperCase();
    String month = sdfMonth.format(date).toUpperCase();
    String monthDate = sdfMonthDate.format(date).toUpperCase();

    String dataLocation =
        dataDumpPath + year + "/" + month + "/" + monthDate + "/" + symbol + ".txt";
    Calendar startCal = Calendar.getInstance();
    startCal.setTime(date);
    startCal.set(Calendar.HOUR_OF_DAY, 9);
    startCal.set(Calendar.MINUTE, 14);
    File file = new File(dataLocation);
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(file));
      String st;
      while ((st = br.readLine()) != null) {
        String[] dataArr = st.split(",");
        String[] time = dataArr[2].split(":");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time[0]));
        cal.set(Calendar.MINUTE, Integer.parseInt(time[1]));
        Candle candle = new Candle(dataArr[0], 0, cal.getTime(), Double.parseDouble(dataArr[3]),
            Double.parseDouble(dataArr[4]), Double.parseDouble(dataArr[5]),
            Double.parseDouble(dataArr[6]), Double.parseDouble(dataArr[7]));
        if (cal.getTime().before(startCal.getTime())) {
          return candle;
        } else {
          noPreOpenCandle = candle;
          break;
        }
      }
    } finally {
      if (br != null) {
        br.close();
      }
    }
    return noPreOpenCandle;
  }

  //symbol, date, time, open, high, low, close, volume
  public static Candle parseAndGeneratePostCloseCandle(String symbol, Date date)
      throws IOException {
    Candle postCloseCandle = null;
    SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy");
    SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM");
    SimpleDateFormat sdfMonthDate = new SimpleDateFormat("ddMMM");
    String year = sdfYear.format(date).toUpperCase();
    String month = sdfMonth.format(date).toUpperCase();
    String monthDate = sdfMonthDate.format(date).toUpperCase();

    String dataLocation =
        dataDumpPath + year + "/" + month + "/" + monthDate + "/" + symbol + ".txt";
    Calendar endCal = Calendar.getInstance();
    endCal.setTime(date);
    endCal.set(Calendar.HOUR_OF_DAY, 15);
    endCal.set(Calendar.MINUTE, 31);
    File file = new File(dataLocation);
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(file));
      String st;
      while ((st = br.readLine()) != null) {
        String[] dataArr = st.split(",");
        String[] time = dataArr[2].split(":");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time[0]));
        cal.set(Calendar.MINUTE, Integer.parseInt(time[1]));
        Candle candle = new Candle(dataArr[0], 0, cal.getTime(), Double.parseDouble(dataArr[3]),
            Double.parseDouble(dataArr[4]), Double.parseDouble(dataArr[5]),
            Double.parseDouble(dataArr[6]), Double.parseDouble(dataArr[7]));
        postCloseCandle = candle;
        if (cal.getTime().after(endCal.getTime())) {
          return candle;
        }
      }
    } finally {
      if (br != null) {
        br.close();
      }
    }
    return postCloseCandle;
  }
}

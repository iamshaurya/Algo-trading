/**
 *
 */
package com.shaurya.intraday.util;

import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.IndicatorValue;
import com.shaurya.intraday.model.StrategyModel;
import com.zerodhatech.models.Tick;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.joda.time.DateTime;

/**
 * @author Shaurya
 *
 */
public class HelperUtil {

  public static TreeMap<Date, IndicatorValue> convertListToMap(List<IndicatorValue> list) {
    TreeMap<Date, IndicatorValue> map = new TreeMap<>();
    for (IndicatorValue iv : list) {
      map.put(iv.getDate(), iv);
    }
    return map;
  }

  public static Date getDate(String date) {
    return new DateTime(date).toDate();
  }

  public static Date getPrevDateInMinute(Date currentDate) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(currentDate);
    if (cal.get(Calendar.HOUR_OF_DAY) == 9 && cal.get(Calendar.MINUTE) == 15) {
      cal.set(Calendar.HOUR_OF_DAY, 15);
      cal.set(Calendar.MINUTE, 29);
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MILLISECOND, 0);
      cal.roll(Calendar.DAY_OF_YEAR, false);
    } else {
      cal.setTimeInMillis(currentDate.getTime() - 60000);
    }
    return cal.getTime();
  }

  public static Date getPrevTradingDate(Date currentDate) {
    Calendar prevDayCalFrom = Calendar.getInstance();
    prevDayCalFrom.setTime(currentDate);
    prevDayCalFrom.set(Calendar.HOUR_OF_DAY, 9);
    prevDayCalFrom.set(Calendar.MINUTE, 15);
    prevDayCalFrom.set(Calendar.SECOND, 0);
    prevDayCalFrom.set(Calendar.MILLISECOND, 0);
    if (prevDayCalFrom.get(Calendar.DAY_OF_WEEK) == 2) {
      int i = 0;
      while (i < 2) {
        prevDayCalFrom.roll(Calendar.DAY_OF_YEAR, false);
        i++;
      }
    }
    prevDayCalFrom.roll(Calendar.DAY_OF_YEAR, false);
    return prevDayCalFrom.getTime();
  }

  public static Date rollDayOfYearByN(Date currentDate, int n) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(currentDate);
    cal.set(Calendar.HOUR_OF_DAY, 9);
    cal.set(Calendar.MINUTE, 15);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    cal.add(Calendar.DAY_OF_YEAR, n);
    return cal.getTime();
  }

  public static List<Candle> convertIndiactorValueToCandle(List<IndicatorValue> ivlist) {
    List<Candle> cList = new ArrayList<>();
    for (IndicatorValue iv : ivlist) {
      cList.add(new Candle(null, 0, iv.getDate(), 0, 0, 0, iv.getIndicatorValue(), 0));
    }
    return cList;
  }

  public static boolean takeProfitReached(Candle candle, StrategyModel openTrade) {
    boolean isTakeProfitReached = false;
    switch (openTrade.getPosition()) {
      case LONG:
        isTakeProfitReached = (candle.getClose() - openTrade.getTp() >= openTrade.getTradePrice());
        break;
      case SHORT:
        isTakeProfitReached = (openTrade.getTradePrice() - openTrade.getTp() >= candle.getClose());
        break;
      default:
        break;
    }
    return isTakeProfitReached;
  }

  public static boolean stopLossReached(Candle candle, StrategyModel openTrade) {
    boolean isStopLossReached = false;
    switch (openTrade.getPosition()) {
      case LONG:
        isStopLossReached = (openTrade.getTradePrice() - openTrade.getSl() >= candle.getLow());
        break;
      case SHORT:
        isStopLossReached = (openTrade.getTradePrice() + openTrade.getSl() <= candle.getHigh());
        break;
      default:
        break;
    }
    return isStopLossReached;
  }

  public static boolean isTimeAfterNoon(Date date) {
    Calendar noonTime = getNoonTime();
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    return date.getTime() >= noonTime.getTimeInMillis();
  }

  public static Calendar getNoonTime() {
    Calendar noonTime = Calendar.getInstance();
    noonTime.set(Calendar.HOUR_OF_DAY, 12);
    noonTime.set(Calendar.MINUTE, 0);
    noonTime.set(Calendar.SECOND, 0);
    noonTime.set(Calendar.MILLISECOND, 0);
    return noonTime;
  }

  public static boolean isTimeDiff1Min(AtomicInteger count) {
    return count.intValue() == 60;
  }

  public static boolean isTimeDiff1Min(Date startTime, Date endTime) {
    return endTime.getTime() - startTime.getTime() > 59000;
  }

  public static boolean isBetweenTradingWindow(Date tickTimestamp) {
    Calendar openTime = getDayStartTime();
    Calendar closeTime = getDayEndTime();
    Calendar tick = Calendar.getInstance();
    tick.setTime(tickTimestamp);

    return tick.after(openTime) && tick.before(closeTime);
  }

  public static Calendar getDayEndTime() {
    Calendar closeTime = Calendar.getInstance();
    closeTime.set(Calendar.HOUR_OF_DAY, 15);
    closeTime.set(Calendar.MINUTE, 30);
    closeTime.set(Calendar.SECOND, 5);
    closeTime.set(Calendar.MILLISECOND, 0);
    return closeTime;
  }

  public static Calendar getDayStartTime() {
    Calendar openTime = Calendar.getInstance();
    openTime.set(Calendar.HOUR_OF_DAY, 9);
    openTime.set(Calendar.MINUTE, 15);
    openTime.set(Calendar.SECOND, 0);
    openTime.set(Calendar.MILLISECOND, 0);
    return openTime;
  }

  public static boolean isIntradayClosingTime(Date time) {
    Calendar closeTime = Calendar.getInstance();
    closeTime.set(Calendar.HOUR_OF_DAY, 15);
    closeTime.set(Calendar.MINUTE, 19);
    closeTime.set(Calendar.SECOND, 0);
    closeTime.set(Calendar.MILLISECOND, 0);
    Calendar currTime = Calendar.getInstance();
    currTime.setTime(time);
    currTime.set(Calendar.SECOND, 0);
    currTime.set(Calendar.MILLISECOND, 0);

    return currTime.getTimeInMillis() >= closeTime.getTimeInMillis();
  }

  public static Candle convertTickToCandle(Tick tick, Map<Long, String> nameTokenMap) {
    return new Candle(nameTokenMap.get(tick.getInstrumentToken()), tick.getInstrumentToken(),
        getDateFromTickTimestamp(tick.getTickTimestamp()), tick.getOpenPrice(), tick.getHighPrice(),
        tick.getLowPrice(), tick.getClosePrice(), tick.getVolumeTradedToday());
  }

  public static Date getDateFromTickTimestamp(Date tTsmp) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(tTsmp);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTime();
  }

  public static Date getDateTillSecondsFromTickTimestamp(Date tTsmp) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(tTsmp);
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTime();
  }

  public static Date getLastDayEndDate() {
    Calendar cal = Calendar.getInstance();
    cal.roll(Calendar.DATE, false);
    cal.set(Calendar.HOUR_OF_DAY, 15);
    cal.set(Calendar.MINUTE, 29);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    return cal.getTime();
  }

  public static int getTradeQuantity(Double margin, Double ltp, double marginMultiplier) {
    return (int) Math.floor((margin * marginMultiplier) / ltp);
  }

  public static Date getNthLastKeyEntry(TreeMap<Date, IndicatorValue> map, int n) {
    int i = 1;
    for (Date e : map.descendingKeySet()) {
      if (n == i) {
        return e;
      }
      i++;
    }
    return null;
  }

  public static boolean isNifty50Uptrend(Candle c) {
    return c.getClose() >= (c.getOpen() * 1.0025);
  }

  public static boolean isNifty50Downtrend(Candle c) {
    return c.getClose() <= (c.getOpen() * 0.9975);
  }

  public static Candle formDayCandle(TreeSet<Candle> candleSet) {
    Candle candleday = null;
    int i = 0;
    Iterator<Candle> cItr = candleSet.iterator();
    while (cItr.hasNext()) {
      Candle c = cItr.next();
      if (i == 0) {
        candleday = new Candle(c.getSecurity(), c.getToken(), c.getTime(), c.getOpen(),
            c.getHigh(),
            c.getLow(), c.getClose(), 0);
      } else {
        candleday.setClose(c.getClose());
        candleday.setHigh(Math.max(candleday.getHigh(), c.getHigh()));
        candleday.setLow(Math.min(candleday.getLow(), c.getLow()));
      }
      i++;
    }
    candleSet.clear();
    return candleday;
  }

  public static double cprRange(Candle dayCandle) {
    double cprWidth = Math.abs(
        ((2 * dayCandle.getClose()) - dayCandle.getHigh() - dayCandle.getLow()) / (
            dayCandle.getClose() + dayCandle.getHigh() + dayCandle.getLow())) * 100;
    return cprWidth;
  }

}

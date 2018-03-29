/**
 * 
 */
package com.shaurya.intraday.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.joda.time.DateTime;

import com.shaurya.intraday.entity.DailyVolatility;
import com.shaurya.intraday.entity.NseStock;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.IndicatorValue;
import com.shaurya.intraday.model.StrategyModel;
import com.zerodhatech.models.Tick;

import au.com.bytecode.opencsv.CSVReader;

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
			cList.add(new Candle(null, iv.getDate(), 0, 0, 0, iv.getIndicatorValue(), 0));
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
			isStopLossReached = (openTrade.getTradePrice() - openTrade.getSl() >= candle.getClose());
			break;
		case SHORT:
			isStopLossReached = (openTrade.getTradePrice() + openTrade.getSl() <= candle.getClose());
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
		return endTime.getTime() - startTime.getTime() >= 59000;
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
		closeTime.set(Calendar.MINUTE, 18);
		Calendar currTime = Calendar.getInstance();
		currTime.setTime(time);

		return currTime.getTimeInMillis() >= closeTime.getTimeInMillis();
	}

	public static Candle convertTickToCandle(Tick tick, Map<Long, String> nameTokenMap) {
		return new Candle(nameTokenMap.get(tick.getInstrumentToken()),
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

	public static List<NseStock> parseCsvFileIntoStockList(File file) {
		List<NseStock> stocktSet = new ArrayList<NseStock>();
		try {
			CSVReader csvReader = new CSVReader(new FileReader(file));
			String[] row = csvReader.readNext();
			while ((row = csvReader.readNext()) != null) {
				NseStock ns = new NseStock();
				ns.setSymbol(row[2]);
				stocktSet.add(ns);
			}
			csvReader.close();
		} catch (FileNotFoundException e) {
			System.out.println("NSE List : file not found");
		} catch (IOException e) {
			System.out.println("NSE List : file not found");
		}
		return stocktSet;
	}

	public static List<DailyVolatility> parseCsvFileIntoVolatileStockList(File file) {
		List<DailyVolatility> stocktSet = new ArrayList<DailyVolatility>();
		try {
			CSVReader csvReader = new CSVReader(new FileReader(file));
			String[] row = csvReader.readNext();
			while ((row = csvReader.readNext()) != null) {
				DailyVolatility dv = new DailyVolatility();
				dv.setSymbol(row[1]);
				dv.setDailyVolatility(Double.parseDouble(row[6]) * 100);
				dv.setAnnualVolatility(Double.parseDouble(row[7]) * 100);

				stocktSet.add(dv);
			}
			csvReader.close();
		} catch (FileNotFoundException e) {
			System.out.println("NSE Volatility: file not found");
		} catch (IOException e) {
			System.out.println("NSE Volatility : file not found");
		}
		return stocktSet;
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

}

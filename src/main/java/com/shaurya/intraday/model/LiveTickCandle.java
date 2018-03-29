/**
 * 
 */
package com.shaurya.intraday.model;

import java.util.Date;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import com.shaurya.intraday.util.HelperUtil;

/**
 * @author Shaurya
 *
 */
public class LiveTickCandle {
	private Candle candle;
	private Date prevCandleCreationTime;
	private AtomicInteger count = null;
	private TreeSet<Date> candleDate;

	public Candle getCandle() {
		return candle;
	}

	public void setCandle(Candle candle) {
		this.candle = candle;
	}

	public Date getPrevCandleCreationTime() {
		return prevCandleCreationTime;
	}

	public void setPrevCandleCreationTime(Date prevCandleCreationTime) {
		this.prevCandleCreationTime = prevCandleCreationTime;
	}

	public AtomicInteger getCount() {
		return count;
	}

	public void setCount(AtomicInteger count) {
		this.count = count;
	}
	
	public TreeSet<Date> getCandleDate() {
		return candleDate;
	}

	public void setCandleDate(TreeSet<Date> candleDate) {
		this.candleDate = candleDate;
	}

	public LiveTickCandle() {

	}

	public LiveTickCandle(double ltp, String security, Date time) {
		if (candle == null) {
			candle = new Candle(security, time, ltp, ltp, ltp, ltp, 0);
		} else {
			this.candle.setTime(time);
			this.candle.setOpen(ltp);
			this.candle.setClose(ltp);
			this.candle.setHigh(ltp);
			this.candle.setLow(ltp);
		}
		this.prevCandleCreationTime = HelperUtil.getDateFromTickTimestamp(time);
		this.count = new AtomicInteger(1);
		candleDate = new TreeSet<>();
		candleDate.add(this.prevCandleCreationTime);
	}


	public void update(double ltp, Date time) {
		this.candle.setTime(this.prevCandleCreationTime);
		this.candle.setClose(ltp);
		this.candle.setHigh(Math.max(this.candle.getHigh(), ltp));
		this.candle.setLow(Math.min(this.candle.getLow(), ltp));
		this.count.incrementAndGet();
		candleDate.add(time);
	}
	
	public Date getLastTime(){
		return getCandleDate().last();
	}
}

/**
 * 
 */
package com.shaurya.intraday.builder;

import static com.shaurya.intraday.util.HelperUtil.getDateFromTickTimestamp;

import java.util.Date;
import java.util.Map;

import org.joda.time.DateTime;

import com.shaurya.intraday.entity.HistoricalCandle;
import com.shaurya.intraday.entity.Trade;
import com.shaurya.intraday.entity.TradeStrategy;
import com.shaurya.intraday.entity.VolatileStock;
import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.enums.StrategyType;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.StrategyModel;
import com.zerodhatech.models.HistoricalData;
import com.zerodhatech.models.Tick;

/**
 * @author Shaurya
 *
 */
public class TradeBuilder {

	public static Trade convert(StrategyModel model) {
		Trade trade = new Trade();
		trade.setAtr(model.getAtr());
		trade.setOrderId(model.getOrderId());
		trade.setPositionType((byte) model.getPosition().getId());
		trade.setQuantity(model.getQuantity());
		trade.setSecurityCode(null);
		trade.setSecurityName(model.getSecurity());
		trade.setSl(model.getSl());
		trade.setTp(model.getTp());
		trade.setTradeDate(new Date());
		trade.setTradeEntryPrice(model.getTradePrice());
		trade.setTradeExitPrice(null);
		trade.setStatus((byte) 1);
		return trade;
	}

	public static StrategyModel reverseConvert(Trade openTrade, boolean isOpenTrade) {
		StrategyModel model = new StrategyModel(PositionType.getEnumById(openTrade.getPositionType().intValue()),
				openTrade.getAtr(), isOpenTrade ? openTrade.getTradeEntryPrice() : openTrade.getTradeExitPrice(),
				openTrade.getSecurityName(), openTrade.getOrderId(), openTrade.getQuantity(), false);
		return model;
	}

	public static HistoricalCandle convert(Tick tick, Map<Long, String> nameTokenMap) {
		HistoricalCandle hc = new HistoricalCandle();
		hc.setClose(tick.getClosePrice());
		hc.setDay(1);
		hc.setHigh(tick.getHighPrice());
		hc.setLow(tick.getLowPrice());
		hc.setOpen(tick.getOpenPrice());
		hc.setSecurityName(nameTokenMap.get(tick.getInstrumentToken()));
		hc.setTimestamp(getDateFromTickTimestamp(tick.getTickTimestamp()));
		return hc;
	}

	public static HistoricalCandle convertToHistoricalcandle(Candle ca) {
		HistoricalCandle hc = new HistoricalCandle();
		hc.setClose(ca.getClose());
		hc.setDay(1);
		hc.setHigh(ca.getHigh());
		hc.setLow(ca.getLow());
		hc.setOpen(ca.getOpen());
		hc.setSecurityName(ca.getSecurity());
		hc.setTimestamp(getDateFromTickTimestamp(ca.getTime()));
		return hc;
	}

	public static Candle reverseConvert(HistoricalCandle hc) {
		return new Candle(hc.getSecurityName(), hc.getTimestamp(), hc.getOpen(), hc.getHigh(), hc.getLow(),
				hc.getClose(), 0);

	}

	public static Candle convertHistoricalDataToCandle(HistoricalData hd, String security) {
		Date time = new DateTime(hd.timeStamp).toDate();
		return new Candle(security, time, hd.open, hd.high, hd.low, hd.close, hd.volume);
	}

	public static TradeStrategy convertStrategyModelToEntity(StrategyModel sm) {
		TradeStrategy ts = new TradeStrategy();
		ts.setDay(2);
		ts.setMarginMultiplier(sm.getMarginMultiplier());
		ts.setPreferedPosition((byte) sm.getPreferedPosition().getId());
		ts.setSecurityName(sm.getSecurity());
		ts.setSecurityToken(sm.getSecurityToken());
		ts.setStrategyType(StrategyType.OPEN_HIGH_LOW.getId());
		ts.setMarginPortion((double) 5000);
		return ts;
	}

	public static VolatileStock convertToVolatileStock(String symbol) {
		VolatileStock vs = new VolatileStock();
		vs.setSymbol(symbol);
		vs.setState((byte) 0);
		return vs;
	}

}

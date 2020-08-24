/**
 * 
 */
package com.shaurya.intraday.builder;

import com.shaurya.intraday.entity.Trade;
import com.shaurya.intraday.entity.TradeStrategy;
import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.enums.StrategyType;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.StrategyModel;
import com.zerodhatech.models.HistoricalData;
import java.util.Date;
import org.joda.time.DateTime;

/**
 * @author Shaurya
 *
 */
public class TradeBuilder {

	public static Trade convert(StrategyModel model) {
		Trade trade = new Trade();
		trade.setOrderId(model.getOrderId());
		trade.setPositionType((byte) model.getPosition().getId());
		trade.setQuantity(model.getQuantity());
		trade.setSecurityCode(model.getSecurityToken());
		trade.setSecurityName(model.getSecurity());
		trade.setSl(model.getSl());
		trade.setTp(model.getTp());
		trade.setTradeDate(new Date());
		trade.setTradeEntryPrice(model.getTradePrice());
		trade.setTradeExitPrice(null);
		trade.setStatus((byte) 1);
		trade.setRisk(model.getSl() * model.getQuantity());
		return trade;
	}

	public static StrategyModel reverseConvert(Trade openTrade, boolean isOpenTrade) {
		StrategyModel model = new StrategyModel(openTrade.getSecurityCode(),
				PositionType.getEnumById(openTrade.getPositionType().intValue()), openTrade.getSl(),
				isOpenTrade ? openTrade.getTradeEntryPrice() : openTrade.getTradeExitPrice(),
				openTrade.getSecurityName(), openTrade.getOrderId(), openTrade.getQuantity(), false);
		return model;
	}

	public static Candle convertHistoricalDataToCandle(HistoricalData hd, String security, long token) {
		Date time = new DateTime(hd.timeStamp).toDate();
		return new Candle(security, token, time, hd.open, hd.high, hd.low, hd.close, hd.volume);
	}

	public static TradeStrategy convertStrategyModelToEntity(StrategyModel sm) {
		TradeStrategy ts = new TradeStrategy();
		ts.setDay(2);
		ts.setMarginMultiplier(sm.getMarginMultiplier());
		ts.setPreferedPosition((byte) sm.getPreferedPosition().getId());
		ts.setSecurityName(sm.getSecurity());
		ts.setSecurityToken(sm.getSecurityToken());
		ts.setStrategyType(StrategyType.OPENING_RANGE_BREAKOUT.getId());
		ts.setMarginPortion((double) 2000);
		ts.setQuantity(sm.getQuantity());
		return ts;
	}

}

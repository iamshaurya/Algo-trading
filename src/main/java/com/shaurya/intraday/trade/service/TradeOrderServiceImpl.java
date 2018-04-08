/**
 * 
 */
package com.shaurya.intraday.trade.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.shaurya.intraday.constant.Constants;
import com.shaurya.intraday.enums.OrderStatusType;
import com.shaurya.intraday.enums.PositionType;
import com.shaurya.intraday.enums.StrategyType;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.MailAccount;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.strategy.Strategy;
import com.shaurya.intraday.util.MailSender;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Margin;
import com.zerodhatech.models.Order;
import com.zerodhatech.models.OrderParams;
import com.zerodhatech.models.Position;

/**
 * @author Shaurya
 *
 */
@Service
public class TradeOrderServiceImpl implements TradeOrderService {
	private static final String VALIDITY_DAY = "DAY";
	private static final String PRODUCT_MIS = "MIS";
	private static final String ORDER_TYPE_MARKET = "MARKET";
	private static final String EXCHANGE_NSE = "NSE";
	private static final String VARIETY_COVER_ORDER = "co";
	@Autowired
	private LoginService loginService;
	@Autowired
	private MailAccount mailAccount;

	@Override
	public StrategyModel placeEntryCoverOrder(StrategyModel model) {
		OrderParams orderParams = new OrderParams();
		orderParams.exchange = EXCHANGE_NSE;
		orderParams.tradingsymbol = model.getSecurity();
		orderParams.orderType = ORDER_TYPE_MARKET;
		orderParams.product = PRODUCT_MIS;
		orderParams.quantity = model.getQuantity();
		orderParams.triggerPrice = getTriggerPrice(model);
		orderParams.validity = VALIDITY_DAY;
		orderParams.transactionType = getTransactionType(model.getPosition());
		Order order = null;
		try {
			order = loginService.getSdkClient().placeOrder(orderParams, VARIETY_COVER_ORDER);
			model.setOrderId(order.orderId);
		} catch (JSONException | IOException | KiteException e) {
			System.out.println("Error in placing entry cover order :: " + e.getCause());
			MailSender.sendMail(Constants.TO_MAIL, Constants.TO_NAME, Constants.FAILED_ENTRY_COVER_ORDER,
					e.getMessage(), mailAccount);
		}
		return model;
	}

	private Double getTriggerPrice(StrategyModel model) {
		// on system keep sl X3 for minimizing stop loss hunting by high/low
		// tails and check 0.5% stop loss by candle from our system
		Double tPrice = null;
		switch (model.getPosition()) {
		case LONG:
			tPrice = model.getTradePrice() - (model.getSl() * 5);
			break;
		case SHORT:
			tPrice = model.getTradePrice() + (model.getSl() * 5);
			break;
		default:
			break;
		}
		return tPrice;
	}

	private String getTransactionType(PositionType pos) {
		return pos == PositionType.LONG ? "BUY" : "SELL";
	}

	@Override
	public StrategyModel placeExitCoverOrder(StrategyModel model) throws JSONException, IOException, KiteException {
		List<Order> orders = loginService.getSdkClient().getOrders();
		for (Order or : orders) {
			if (or.parentOrderId != null && or.parentOrderId.equals(model.getOrderId())) {
				loginService.getSdkClient().cancelOrder(or.orderId, or.parentOrderId, VARIETY_COVER_ORDER);
			}
		}
		return model;
	}

	@Override
	public String getOrderId(StrategyModel model) throws JSONException, IOException, KiteException {
		List<Order> orders = loginService.getSdkClient().getOrders();
		for (Order or : orders) {
			if (or.tradingSymbol.equalsIgnoreCase(model.getSecurity())) {
				return or.orderId;
			}
		}
		return null;
	}

	@Override
	public OrderStatusType getOrderStatus(StrategyModel model) throws JSONException, IOException, KiteException {
		OrderStatusType status = null;
		Map<String, List<Position>> positionMap = loginService.getSdkClient().getPositions();
		Position position = null;
		for (Position p : positionMap.get("net")) {
			if (p.tradingSymbol.equals(model.getSecurity())) {
				position = p;
				break;
			}
		}
		if (position != null) {
			if (position.netQuantity != 0) {
				status = OrderStatusType.OPEN;
			} else {
				status = OrderStatusType.CLOSE;
			}
		}
		return status;
	}

	@Override
	public List<Candle> getPrevDayData(Object obj) {
		throw new RuntimeException("This feature is not supported");
	}

	@Override
	public Double getMarginForSecurity(Map<String, Strategy> strategyMap)
			throws JSONException, IOException, KiteException {
		Margin margins = loginService.getSdkClient().getMargins("equity");
		double net = Double.parseDouble(margins.net);
		return (0.8 * net) / strategyMap.size();
	}

	@Override
	public Double getTotalMargin() throws JSONException, IOException, KiteException {
		Margin margins = loginService.getSdkClient().getMargins("equity");
		double net = Double.parseDouble(margins.net);
		return ((0.8 * net));
	}

}

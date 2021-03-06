package org.knowm.xchange.poloniex.service.polling;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;

/**
 * @author Zach Holmes
 */

import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.poloniex.PoloniexAuthenticated;
import org.knowm.xchange.poloniex.PoloniexException;
import org.knowm.xchange.poloniex.PoloniexUtils;
import org.knowm.xchange.poloniex.dto.trade.PoloniexMoveResponse;
import org.knowm.xchange.poloniex.dto.trade.PoloniexOpenOrder;
import org.knowm.xchange.poloniex.dto.trade.PoloniexOrderFlags;
import org.knowm.xchange.poloniex.dto.trade.PoloniexTradeResponse;
import org.knowm.xchange.poloniex.dto.trade.PoloniexUserTrade;

import si.mazi.rescu.ParamsDigest;
import si.mazi.rescu.SynchronizedValueFactory;

public class PoloniexTradeServiceRaw extends PoloniexBasePollingService {

  public PoloniexTradeServiceRaw(Exchange exchange) {

    super(exchange);
  }

  public HashMap<String, PoloniexOpenOrder[]> returnOpenOrders() throws IOException {

    return poloniexAuthenticated.returnOpenOrders(apiKey, signatureCreator, exchange.getNonceFactory(), "all");
  }

  public PoloniexUserTrade[] returnTradeHistory(CurrencyPair currencyPair, Long startTime, Long endTime) throws IOException {

    return poloniexAuthenticated.returnTradeHistory(apiKey, signatureCreator, exchange.getNonceFactory(), PoloniexUtils.toPairString(currencyPair),
        startTime, endTime);
  }

  public HashMap<String, PoloniexUserTrade[]> returnTradeHistory(Long startTime, Long endTime) throws IOException {

    String ignore = null; // only used so PoloniexAuthenticated.returnTradeHistory can be overloaded
    return poloniexAuthenticated.returnTradeHistory(apiKey, signatureCreator, exchange.getNonceFactory(), "all", startTime, endTime, ignore);
  }

  public PoloniexTradeResponse buy(LimitOrder limitOrder) throws IOException {
    return orderEntry(limitOrder, "buy");
  }

  public PoloniexTradeResponse sell(LimitOrder limitOrder) throws IOException {
    return orderEntry(limitOrder, "sell");
  }

  private PoloniexTradeResponse orderEntry(LimitOrder limitOrder, String name) throws IOException {
    Integer fillOrKill;
    if (limitOrder.getOrderFlags().contains(PoloniexOrderFlags.FILL_OR_KILL)) {
      fillOrKill = 1;
    } else {
      fillOrKill = null;
    }

    Integer immediateOrCancel;
    if (limitOrder.getOrderFlags().contains(PoloniexOrderFlags.IMMEDIATE_OR_CANCEL)) {
      immediateOrCancel = 1;
    } else {
      immediateOrCancel = null;
    }

    Integer postOnly;
    if (limitOrder.getOrderFlags().contains(PoloniexOrderFlags.POST_ONLY)) {
      postOnly = 1;
    } else {
      postOnly = null;
    }

    try {
      Method method = PoloniexAuthenticated.class.getDeclaredMethod(name, String.class, ParamsDigest.class, SynchronizedValueFactory.class,
          String.class, String.class, String.class, Integer.class, Integer.class, Integer.class);
      PoloniexTradeResponse response = (PoloniexTradeResponse) method.invoke(poloniexAuthenticated, apiKey, signatureCreator,
          exchange.getNonceFactory(), limitOrder.getTradableAmount().toPlainString(), limitOrder.getLimitPrice().toPlainString(),
          PoloniexUtils.toPairString(limitOrder.getCurrencyPair()), fillOrKill, immediateOrCancel, postOnly);
      return response;
    } catch (PoloniexException e) {
      throw new ExchangeException(e.getError(), e);
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new ExchangeException(e.getMessage(), e);
    }
  }

  public PoloniexMoveResponse move(String orderId, BigDecimal tradableAmount, BigDecimal limitPrice) throws IOException {

    try {
      return poloniexAuthenticated.moveOrder(apiKey, signatureCreator, exchange.getNonceFactory(), orderId, tradableAmount.toPlainString(),
          limitPrice.toPlainString());
    } catch (PoloniexException e) {
      throw new ExchangeException(e.getError(), e);
    }
  }

  public boolean cancel(String orderId) throws IOException {
    HashMap<String, String> response = poloniexAuthenticated.cancelOrder(apiKey, signatureCreator, exchange.getNonceFactory(), orderId);
    if (response.containsKey("error")) {
      throw new ExchangeException(response.get("error"));
    } else {
      return response.get("success").toString().equals(new Integer(1).toString()) ? true : false;
    }
  }

  public HashMap<String, String> getFeeInfo() throws IOException {
    HashMap<String, String> response = poloniexAuthenticated.returnFeeInfo(apiKey, signatureCreator, exchange.getNonceFactory());
    if (response.containsKey("error")) {
      throw new ExchangeException(response.get("error"));
    }
    return response;
  }

}

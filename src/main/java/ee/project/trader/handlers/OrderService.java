package ee.project.trader.handlers;

import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderType;
import com.ib.client.Types;
import ee.project.trader.Ticker;
import ee.project.trader.TraderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private ConnectionHandler connectionHandler;

    @Autowired
    private TraderService traderService;

    //This is taken straight from the API Documentation, with some minor modifications
    public void createOrder(String symbol, int parentOrderId, Types.Action action, int quantity, double limitPrice, double takeProfitLimitPrice, double stopLossPrice) {

        Contract contract = new Contract();
        contract.symbol(symbol);
        contract.secType("STK");
        contract.currency("USD");
        contract.exchange("SMART");

        //This will be our main or "parent" order
        Order parent = new Order();
        parent.orderId(parentOrderId);
        parent.action(action);
        parent.orderType(OrderType.LMT);
        parent.totalQuantity(quantity);
        parent.lmtPrice(limitPrice);

        //The parent and children orders will need this attribute set to false to prevent accidental executions.
        //The LAST CHILD will have it set to true.
        parent.transmit(false);
        Order takeProfit = new Order();
        takeProfit.orderId(parent.orderId() + 1);
        takeProfit.action(action.equals(Types.Action.BUY) ? Types.Action.SELL : Types.Action.BUY);
        takeProfit.orderType(OrderType.LMT);
        takeProfit.totalQuantity(quantity);
        takeProfit.lmtPrice(takeProfitLimitPrice);
        takeProfit.parentId(parentOrderId);
        takeProfit.transmit(false);
        Order stopLoss = new Order();
        stopLoss.orderId(parent.orderId() + 2);
        stopLoss.action(action.equals(Types.Action.BUY) ? Types.Action.SELL : Types.Action.BUY);
        stopLoss.orderType(OrderType.STP);
        //Stop trigger price
        stopLoss.auxPrice(stopLossPrice);
        stopLoss.totalQuantity(quantity);
        stopLoss.parentId(parentOrderId);
        //In this case, the low side order will be the last child being sent. Therefore, it needs to set this attribute to true
        //to activate all its predecessors
        stopLoss.transmit(true);

        Ticker ticker = new Ticker();
        ticker.setSymbol(symbol);
        //TODO muuda ära

        connectionHandler.placeOrModifyOrder(contract, parent, new OrderHandler(ticker, traderService, connectionHandler));
        connectionHandler.placeOrModifyOrder(contract, takeProfit, new OrderHandler(ticker, traderService, connectionHandler));
        connectionHandler.placeOrModifyOrder(contract, stopLoss, new OrderHandler(ticker, traderService, connectionHandler));



    }
}

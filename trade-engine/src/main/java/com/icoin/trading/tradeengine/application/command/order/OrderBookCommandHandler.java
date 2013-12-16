/*
 * Copyright (c) 2010-2012. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.icoin.trading.tradeengine.application.command.order;

import com.icoin.trading.tradeengine.application.executor.TradeExecutor;
import com.icoin.trading.tradeengine.domain.model.coin.CurrencyPair;
import com.icoin.trading.tradeengine.domain.model.order.AbstractOrder;
import com.icoin.trading.tradeengine.domain.model.order.BuyOrder;
import com.icoin.trading.tradeengine.domain.model.order.BuyOrderRepository;
import com.icoin.trading.tradeengine.domain.model.order.OrderBook;
import com.icoin.trading.tradeengine.domain.model.order.SellOrder;
import com.icoin.trading.tradeengine.domain.model.order.SellOrderRepository;
import org.axonframework.commandhandling.annotation.CommandHandler;
import org.axonframework.repository.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Allard Buijze
 */
@Component
public class OrderBookCommandHandler {

    private Repository<OrderBook> repository;
    private SellOrderRepository sellOrderRepository;
    private BuyOrderRepository buyOrderRepository;
    private TradeExecutor tradeExecutor;


    @CommandHandler
    public void handleBuyOrder(CreateBuyOrderCommand command) {
        OrderBook orderBook = repository.load(command.getOrderBookId(), null);
        final BuyOrder buyOrder = createBuyOrder(command, orderBook.getCurrencyPair());
        buyOrderRepository.save(buyOrder);

        orderBook.addBuyOrder(command.getOrderId(),
                command.getTransactionId(),
                command.getTradeAmount(),
                command.getItemPrice(),
                command.getPortfolioId(),
                command.getPlaceDate());
        tradeExecutor.execute(buyOrder);
    }

    private BuyOrder createBuyOrder(CreateBuyOrderCommand command, CurrencyPair currencyPair) {
        final BuyOrder buyOrder = new BuyOrder();
        return fillOrder(buyOrder, command, currencyPair);
    }

    @CommandHandler
    public void handleSellOrder(CreateSellOrderCommand command) {
        OrderBook orderBook = repository.load(command.getOrderBookId(), null);
        final SellOrder sellOrder = createSellOrder(command, orderBook.getCurrencyPair());

        sellOrderRepository.save(sellOrder);
        orderBook.addSellOrder(command.getOrderId(),
                command.getTransactionId(),
                command.getTradeAmount(),
                command.getItemPrice(),
                command.getPortfolioId(),
                command.getPlaceDate());

        tradeExecutor.execute(sellOrder);
    }

    private SellOrder createSellOrder(CreateSellOrderCommand command, CurrencyPair currencyPair) {
        SellOrder sellOrder = new SellOrder();
        return fillOrder(sellOrder, command, currencyPair);
    }

    private <T extends AbstractOrder> T fillOrder(T order,
                                                  AbstractOrderCommand command,
                                                  CurrencyPair currencyPair) {
        order.setPrimaryKey(command.getOrderId().toString());
        order.setOrderBookId(command.getOrderBookId());
        order.setTransactionId(command.getTransactionId());
        order.setCurrencyPair(currencyPair);
        order.setPlaceDate(command.getPlaceDate());
        order.setItemPrice(command.getItemPrice());
        order.setTradeAmount(command.getTradeAmount());
        order.setItemRemaining(command.getTradeAmount());
        order.setPortfolioId(command.getPortfolioId());

        return order;
    }

    @CommandHandler
    public void handleCreateOrderBook(CreateOrderBookCommand command) {
        OrderBook orderBook =
                new OrderBook(command.getOrderBookIdentifier(), command.getCurrencyPair());
        repository.add(orderBook);
    }

//    @CommandHandler
//    public void handleRefreshOrderBook(CreateOrderBookCommand command) {
//        OrderBook orderBook =
//                new OrderBook(command.getOrderBookIdentifier(), command.getCurrencyPair());
//        repository.add(orderBook);
//    }

    @Resource(name = "orderBookRepository")
    public void setRepository(Repository<OrderBook> orderBookRepository) {
        this.repository = orderBookRepository;
    }

    @Autowired
    public void setSellOrderRepository(SellOrderRepository sellOrderRepository) {
        this.sellOrderRepository = sellOrderRepository;
    }

    @Autowired
    public void setBuyOrderRepository(BuyOrderRepository buyOrderRepository) {
        this.buyOrderRepository = buyOrderRepository;
    }

    @Autowired
    public void setTradeExecutor(TradeExecutor tradeExecutor) {
        this.tradeExecutor = tradeExecutor;
    }
}

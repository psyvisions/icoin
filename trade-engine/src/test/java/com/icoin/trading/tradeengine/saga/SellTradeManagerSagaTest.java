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

package com.icoin.trading.tradeengine.saga;

import com.icoin.trading.tradeengine.domain.events.portfolio.coin.ItemsReservedEvent;
import com.icoin.trading.tradeengine.domain.events.portfolio.coin.NotEnoughItemsAvailableToReserveInPortfolio;
import com.icoin.trading.tradeengine.domain.events.trade.TradeExecutedEvent;
import com.icoin.trading.tradeengine.domain.events.transaction.SellTransactionCancelledEvent;
import com.icoin.trading.tradeengine.domain.events.transaction.SellTransactionConfirmedEvent;
import com.icoin.trading.tradeengine.domain.events.transaction.SellTransactionExecutedEvent;
import com.icoin.trading.tradeengine.domain.events.transaction.SellTransactionPartiallyExecutedEvent;
import com.icoin.trading.tradeengine.domain.events.transaction.SellTransactionStartedEvent;
import com.icoin.trading.tradeengine.domain.model.order.OrderBookId;
import com.icoin.trading.tradeengine.domain.model.order.OrderId;
import com.icoin.trading.tradeengine.domain.model.portfolio.PortfolioId;
import com.icoin.trading.tradeengine.domain.model.transaction.TransactionId;
import com.icoin.trading.tradeengine.saga.matchers.CancelItemReservationForPortfolioCommandMatcher;
import com.icoin.trading.tradeengine.saga.matchers.ConfirmItemReservationForPortfolioCommandMatcher;
import com.icoin.trading.tradeengine.saga.matchers.ConfirmTransactionCommandMatcher;
import com.icoin.trading.tradeengine.saga.matchers.CreateSellOrderCommandMatcher;
import com.icoin.trading.tradeengine.saga.matchers.DepositMoneyToPortfolioCommandMatcher;
import com.icoin.trading.tradeengine.saga.matchers.ExecutedTransactionCommandMatcher;
import com.icoin.trading.tradeengine.saga.matchers.ReservedItemsCommandMatcher;
import org.axonframework.test.saga.AnnotatedSagaTestFixture;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.axonframework.test.matchers.Matchers.andNoMore;
import static org.axonframework.test.matchers.Matchers.exactSequenceOf;

/**
 * @author Jettro Coenradie
 */
public class SellTradeManagerSagaTest {

    private TransactionId transactionIdentifier = new TransactionId();
    private OrderBookId orderbookIdentifier = new OrderBookId();
    private PortfolioId portfolioIdentifier = new PortfolioId();

    private AnnotatedSagaTestFixture fixture;

    @Before
    public void setUp() throws Exception {
        fixture = new AnnotatedSagaTestFixture(SellTradeManagerSaga.class);
    }

    @Test
    public void testHandle_SellTransactionStarted() throws Exception {
        fixture.givenAggregate(transactionIdentifier).published()
                .whenAggregate(transactionIdentifier).publishes(new SellTransactionStartedEvent(transactionIdentifier,
                orderbookIdentifier,
                portfolioIdentifier,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(10)))
                .expectActiveSagas(1)
                .expectDispatchedCommandsMatching(exactSequenceOf(new ReservedItemsCommandMatcher(orderbookIdentifier,
                        portfolioIdentifier,
                        100)));
    }

    @Test
    public void testHandle_ItemsReserved() {
        fixture.givenAggregate(transactionIdentifier).published(new SellTransactionStartedEvent(transactionIdentifier,
                orderbookIdentifier,
                portfolioIdentifier,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(10)))
                .whenAggregate(portfolioIdentifier).publishes(new ItemsReservedEvent(portfolioIdentifier, orderbookIdentifier,
                transactionIdentifier,
                BigDecimal.valueOf(100)))
                .expectActiveSagas(1)
                .expectDispatchedCommandsMatching(exactSequenceOf(new ConfirmTransactionCommandMatcher(
                        transactionIdentifier)));
    }

    @Test
    public void testHandle_TransactionConfirmed() {
        fixture.givenAggregate(transactionIdentifier).published(new SellTransactionStartedEvent(transactionIdentifier,
                orderbookIdentifier,
                portfolioIdentifier,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(10)))
                .andThenAggregate(portfolioIdentifier).published(new ItemsReservedEvent(portfolioIdentifier, orderbookIdentifier,
                transactionIdentifier,
                BigDecimal.valueOf(100)))
                .whenAggregate(transactionIdentifier).publishes(new SellTransactionConfirmedEvent(transactionIdentifier))
                .expectActiveSagas(1)
                .expectDispatchedCommandsMatching(exactSequenceOf(new CreateSellOrderCommandMatcher(portfolioIdentifier,
                        orderbookIdentifier,
                        100,
                        10)));
    }


    @Test
    public void testHandle_NotEnoughItemsToReserve() {
        fixture.givenAggregate(transactionIdentifier).published(new SellTransactionStartedEvent(transactionIdentifier,
                orderbookIdentifier,
                portfolioIdentifier,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(10)))
                .whenAggregate(portfolioIdentifier).publishes(new NotEnoughItemsAvailableToReserveInPortfolio(
                portfolioIdentifier,
                orderbookIdentifier,
                transactionIdentifier,
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(100)))
                .expectActiveSagas(0);
    }

    @Test
    public void testHandle_TransactionCancelled() {
        fixture.givenAggregate(transactionIdentifier).published(new SellTransactionStartedEvent(transactionIdentifier,
                orderbookIdentifier,
                portfolioIdentifier,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(10)))
                .whenAggregate(transactionIdentifier).publishes(new SellTransactionCancelledEvent(
                transactionIdentifier, BigDecimal.valueOf(50), BigDecimal.ZERO))
                .expectActiveSagas(0)
                .expectDispatchedCommandsMatching(exactSequenceOf(new CancelItemReservationForPortfolioCommandMatcher(
                        orderbookIdentifier,
                        portfolioIdentifier,
                        50)));
    }

    @Test
    public void testHandle_TradeExecutedPlaced() {
        OrderId buyOrderIdentifier = new OrderId();
        OrderId sellOrderIdentifier = new OrderId();
        TransactionId buyTransactionIdentifier = new TransactionId();
        fixture.givenAggregate(transactionIdentifier).published(new SellTransactionStartedEvent(transactionIdentifier,
                orderbookIdentifier,
                portfolioIdentifier,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(99)))
                .andThenAggregate(portfolioIdentifier).published(new ItemsReservedEvent(portfolioIdentifier, orderbookIdentifier,
                transactionIdentifier,
                BigDecimal.valueOf(100)))
                .andThenAggregate(transactionIdentifier).published(new SellTransactionConfirmedEvent(transactionIdentifier))
                .whenAggregate(orderbookIdentifier).publishes(new TradeExecutedEvent(orderbookIdentifier,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(102),
                buyOrderIdentifier,
                sellOrderIdentifier,
                buyTransactionIdentifier,
                transactionIdentifier))
                .expectActiveSagas(1)
                .expectDispatchedCommandsMatching(exactSequenceOf(new ExecutedTransactionCommandMatcher(100,
                        102,
                        transactionIdentifier),
                        andNoMore()));
    }

    @Test
    public void testHandle_SellTransactionExecuted() {
        OrderId buyOrderIdentifier = new OrderId();
        OrderId sellOrderIdentifier = new OrderId();
        TransactionId buyTransactionIdentifier = new TransactionId();
        fixture.givenAggregate(transactionIdentifier).published(new SellTransactionStartedEvent(transactionIdentifier,
                orderbookIdentifier,
                portfolioIdentifier,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(99)))
                .andThenAggregate(portfolioIdentifier).published(new ItemsReservedEvent(portfolioIdentifier, orderbookIdentifier,
                transactionIdentifier,
                BigDecimal.valueOf(100)))
                .andThenAggregate(transactionIdentifier).published(new SellTransactionConfirmedEvent(transactionIdentifier))
                .andThenAggregate(orderbookIdentifier).published(new TradeExecutedEvent(orderbookIdentifier,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(102),
                buyOrderIdentifier,
                sellOrderIdentifier,
                buyTransactionIdentifier,
                transactionIdentifier))
                .whenAggregate(transactionIdentifier).publishes(new SellTransactionExecutedEvent(transactionIdentifier, 100, 102))
                .expectActiveSagas(0)
                .expectDispatchedCommandsMatching(
                        exactSequenceOf(
                                new ConfirmItemReservationForPortfolioCommandMatcher(orderbookIdentifier,
                                        portfolioIdentifier,
                                        100),
                                new DepositMoneyToPortfolioCommandMatcher(portfolioIdentifier, 100 * 102)));
    }

    @Test
    public void testHandle_SellTransactionPartiallyExecuted() {
        OrderId buyOrderIdentifier = new OrderId();
        OrderId sellOrderIdentifier = new OrderId();
        TransactionId buyTransactionIdentifier = new TransactionId();

        fixture.givenAggregate(transactionIdentifier).published(new SellTransactionStartedEvent(transactionIdentifier,
                orderbookIdentifier,
                portfolioIdentifier,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(99)))
                .andThenAggregate(portfolioIdentifier).published(new ItemsReservedEvent(portfolioIdentifier, orderbookIdentifier,
                transactionIdentifier,
                BigDecimal.valueOf(100)))
                .andThenAggregate(transactionIdentifier).published(new SellTransactionConfirmedEvent(transactionIdentifier))
                .andThenAggregate(orderbookIdentifier).published(new TradeExecutedEvent(orderbookIdentifier,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(102),
                buyOrderIdentifier,
                sellOrderIdentifier,
                buyTransactionIdentifier,
                transactionIdentifier))
                .whenAggregate(transactionIdentifier).publishes(new SellTransactionPartiallyExecutedEvent(transactionIdentifier, 50, 75, 102))
                .expectActiveSagas(1)
                .expectDispatchedCommandsMatching(
                        exactSequenceOf(
                                new ConfirmItemReservationForPortfolioCommandMatcher(orderbookIdentifier,
                                        portfolioIdentifier,
                                        50),
                                new DepositMoneyToPortfolioCommandMatcher(portfolioIdentifier, 50 * 102)));
    }
}
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

package com.icoin.trading.tradeengine.saga.matchers;

import com.icoin.trading.tradeengine.application.command.portfolio.coin.ReserveItemsCommand;
import com.icoin.trading.tradeengine.domain.model.order.OrderBookId;
import com.icoin.trading.tradeengine.domain.model.portfolio.PortfolioId;
import org.hamcrest.Description;

/**
 * @author Jettro Coenradie
 */
public class ReservedItemsCommandMatcher extends BaseCommandMatcher<ReserveItemsCommand> {

    private OrderBookId orderbookIdentifier;
    private PortfolioId portfolioIdentifier;
    private int amountOfReservedItems;

    public ReservedItemsCommandMatcher(OrderBookId orderbookIdentifier, PortfolioId portfolioIdentifier,
                                       int amountOfReservedItems) {
        this.orderbookIdentifier = orderbookIdentifier;
        this.portfolioIdentifier = portfolioIdentifier;
        this.amountOfReservedItems = amountOfReservedItems;
    }

    @Override
    protected boolean doMatches(ReserveItemsCommand command) {
        return command.getOrderBookIdentifier().equals(orderbookIdentifier)
                && command.getPortfolioIdentifier().equals(portfolioIdentifier)
                && amountOfReservedItems == command.getAmountOfItemsToReserve();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("ReserveItemsCommand with amountOfReservedItems [")
                .appendValue(amountOfReservedItems)
                .appendText("] for OrderBook with identifier [")
                .appendValue(orderbookIdentifier)
                .appendText("] and for Portfolio with identifier [")
                .appendValue(portfolioIdentifier)
                .appendText("]");
    }
}

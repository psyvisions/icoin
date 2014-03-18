package com.icoin.trading.api.fee.command.commission;


import com.icoin.trading.api.coin.domain.CoinId;
import com.icoin.trading.api.fee.domain.FeeTransactionId;
import com.icoin.trading.api.tradeengine.domain.OrderBookId;
import com.icoin.trading.api.tradeengine.domain.PortfolioId;
import com.icoin.trading.api.tradeengine.domain.TradeType;
import com.icoin.trading.api.tradeengine.domain.TransactionId;
import org.joda.money.BigMoney;

import java.util.Date;


/**
 * Created with IntelliJ IDEA.
 * User: liougehooa
 * Date: 14-3-18
 * Time: PM10:05
 * To change this template use File | Settings | File Templates.
 */
public class StartSellCommissionTransactionCommand extends StartCommissionTransactionCommand<StartSellCommissionTransactionCommand> {
    public StartSellCommissionTransactionCommand(FeeTransactionId feeTransactionId,
                                                 BigMoney commissionAmount,
                                                 String orderId,
                                                 TransactionId orderTransactionId,
                                                 PortfolioId portfolioId,
                                                 Date tradeTime,
                                                 Date dueDate,
                                                 TradeType tradeType,
                                                 BigMoney tradedPrice,
                                                 BigMoney tradeAmount,
                                                 BigMoney executedMoney,
                                                 OrderBookId orderBookId,
                                                 CoinId coinId) {
        super(feeTransactionId,
                commissionAmount,
                orderId,
                orderTransactionId,
                portfolioId,
                tradeTime,
                dueDate,
                tradeType,
                tradedPrice,
                tradeAmount,
                executedMoney,
                orderBookId,
                coinId);
    }
}
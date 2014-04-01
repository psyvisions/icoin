package com.icoin.trading.fee.saga;

import com.google.common.collect.ImmutableList;
import com.icoin.trading.api.coin.domain.CoinId;
import com.icoin.trading.api.fee.command.offset.CancelOffsetCommand;
import com.icoin.trading.api.fee.command.offset.CreateOffsetCommand;
import com.icoin.trading.api.fee.command.offset.OffsetFeesCommand;
import com.icoin.trading.api.fee.command.paid.CancelPaidFeeCommand;
import com.icoin.trading.api.fee.command.paid.ConfirmPaidFeeCommand;
import com.icoin.trading.api.fee.command.paid.CreatePaidFeeCommand;
import com.icoin.trading.api.fee.command.paid.OffsetPaidFeeCommand;
import com.icoin.trading.api.fee.command.payable.CancelAccountPayableFeeCommand;
import com.icoin.trading.api.fee.command.payable.ConfirmAccountPayableFeeCommand;
import com.icoin.trading.api.fee.command.payable.CreateAccountPayableFeeCommand;
import com.icoin.trading.api.fee.command.payable.OffsetAccountPayableFeeCommand;
import com.icoin.trading.api.fee.domain.FeeTransactionId;
import com.icoin.trading.api.fee.domain.PaidMode;
import com.icoin.trading.api.fee.domain.fee.BusinessType;
import com.icoin.trading.api.fee.domain.fee.FeeId;
import com.icoin.trading.api.fee.domain.fee.FeeStatus;
import com.icoin.trading.api.fee.domain.fee.FeeType;
import com.icoin.trading.api.fee.domain.offset.CancelledReason;
import com.icoin.trading.api.fee.domain.offset.FeeItem;
import com.icoin.trading.api.fee.domain.offset.FeeItemType;
import com.icoin.trading.api.fee.domain.offset.OffsetId;
import com.icoin.trading.api.fee.domain.offset.OffsetType;
import com.icoin.trading.api.fee.events.execution.ExecutedPayCoinTransactionStartedEvent;
import com.icoin.trading.api.fee.events.fee.paid.PaidFeeCancelledEvent;
import com.icoin.trading.api.fee.events.fee.paid.PaidFeeConfirmedEvent;
import com.icoin.trading.api.fee.events.fee.paid.PaidFeeCreatedEvent;
import com.icoin.trading.api.fee.events.fee.paid.PaidFeeOffsetedEvent;
import com.icoin.trading.api.fee.events.fee.payable.AccountPayableFeeCancelledEvent;
import com.icoin.trading.api.fee.events.fee.payable.AccountPayableFeeConfirmedEvent;
import com.icoin.trading.api.fee.events.fee.payable.AccountPayableFeeCreatedEvent;
import com.icoin.trading.api.fee.events.fee.payable.AccountPayableFeeOffsetedEvent;
import com.icoin.trading.api.fee.events.offset.FeesOffsetedEvent;
import com.icoin.trading.api.fee.events.offset.OffsetAmountNotMatchedEvent;
import com.icoin.trading.api.fee.events.offset.OffsetCancelledEvent;
import com.icoin.trading.api.fee.events.offset.OffsetCreatedEvent;
import com.icoin.trading.api.tradeengine.domain.OrderBookId;
import com.icoin.trading.api.tradeengine.domain.PortfolioId;
import com.icoin.trading.api.tradeengine.domain.TradeType;
import com.icoin.trading.api.tradeengine.domain.TransactionId;
import org.axonframework.test.saga.AnnotatedSagaTestFixture;
import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static com.homhon.util.TimeUtils.currentTime;

/**
 * Created with IntelliJ IDEA.
 * User: liougehooa
 * Date: 14-3-31
 * Time: PM8:41
 * To change this template use File | Settings | File Templates.
 */
public class PaySellCoinFeeManagerSagaTest {
    private final FeeTransactionId feeTransactionId = new FeeTransactionId();
    private final String orderId = "orderId";
    private final CoinId coinId = new CoinId("BTC");
    private final TransactionId orderTransactionId = new TransactionId();
    private final PortfolioId portfolioId = new PortfolioId();
    private final Date tradeTime = currentTime();
    private final Date dueDate = new Date();
    private final OrderBookId orderBookId = new OrderBookId();
    private final FeeId paidFeeId = new FeeId();
    private final FeeId accountPayableFeeId = new FeeId();
    private final OffsetId offsetId = new OffsetId();
    private AnnotatedSagaTestFixture fixture;
    private BigMoney tradeAmount = BigMoney.parse("BTC 12.03467");

    @Before
    public void setUp() throws Exception {
        fixture = new AnnotatedSagaTestFixture(PaySellCoinFeeManagerSaga.class);
    }

    @Test
    public void testStarted() throws Exception {
        fixture.givenAggregate(feeTransactionId).published()
                .whenAggregate(feeTransactionId).publishes(
                new ExecutedPayCoinTransactionStartedEvent(
                        feeTransactionId,
                        paidFeeId,
                        accountPayableFeeId,
                        offsetId,
                        orderId,
                        orderTransactionId,
                        portfolioId,
                        tradeTime,
                        dueDate,
                        TradeType.BUY,
                        BigMoney.of(CurrencyUnit.EUR, 1009),
                        tradeAmount,
                        BigMoney.of(CurrencyUnit.EUR, 109),
                        orderBookId,
                        coinId))
                .expectActiveSagas(1)
                .expectDispatchedCommandsEqualTo(
                        new CreateAccountPayableFeeCommand(
                                feeTransactionId,
                                accountPayableFeeId,
                                FeeStatus.PENDING,
                                tradeAmount,
                                FeeType.PAY_COIN,
                                BusinessType.TRADE_EXECUTED,
                                tradeTime,
                                dueDate,
                                portfolioId.toString(),
                                orderTransactionId.toString()),
                        new CreatePaidFeeCommand(
                                feeTransactionId,
                                paidFeeId,
                                FeeStatus.PENDING,
                                tradeAmount,
                                FeeType.PAY_COIN,
                                BusinessType.TRADE_EXECUTED,
                                tradeTime,
                                dueDate,
                                portfolioId.toString(),
                                orderTransactionId.toString(),
                                PaidMode.INTERNAL),
                        new CreateOffsetCommand(
                                offsetId,
                                OffsetType.AP_PAID,
                                portfolioId.toString(),
                                ImmutableList.of(new FeeItem(accountPayableFeeId.toString(), FeeItemType.AP, tradeAmount)),
                                ImmutableList.of(new FeeItem(paidFeeId.toString(), FeeItemType.PAID, tradeAmount)),
                                tradeAmount,
                                tradeTime));

    }

    @Test
    public void testPayableCreated() throws Exception {
        fixture.givenAggregate(feeTransactionId).published(
                new ExecutedPayCoinTransactionStartedEvent(
                        feeTransactionId,
                        paidFeeId,
                        accountPayableFeeId,
                        offsetId,
                        orderId,
                        orderTransactionId,
                        portfolioId,
                        tradeTime,
                        dueDate,
                        TradeType.BUY,
                        BigMoney.of(CurrencyUnit.EUR, 1009),
                        tradeAmount,
                        BigMoney.of(CurrencyUnit.EUR, 109),
                        orderBookId,
                        coinId))
                .whenAggregate(accountPayableFeeId).publishes(
                new AccountPayableFeeCreatedEvent(
                        accountPayableFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString()))
                .expectActiveSagas(1)
                .expectDispatchedCommandsEqualTo(
                        new ConfirmAccountPayableFeeCommand(
                                accountPayableFeeId,
                                tradeTime));

    }

    @Test
    public void testPaidCreated() throws Exception {
        fixture.givenAggregate(feeTransactionId).published(
                new ExecutedPayCoinTransactionStartedEvent(
                        feeTransactionId,
                        paidFeeId,
                        accountPayableFeeId,
                        offsetId,
                        orderId,
                        orderTransactionId,
                        portfolioId,
                        tradeTime,
                        dueDate,
                        TradeType.BUY,
                        BigMoney.of(CurrencyUnit.EUR, 1009),
                        tradeAmount,
                        BigMoney.of(CurrencyUnit.EUR, 109),
                        orderBookId,
                        coinId))
                .whenAggregate(paidFeeId).publishes(
                new PaidFeeCreatedEvent(
                        paidFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString(),
                        PaidMode.INTERNAL))
                .expectActiveSagas(1)
                .expectDispatchedCommandsEqualTo(
                        new ConfirmPaidFeeCommand(
                                paidFeeId,
                                tradeTime));

    }

    @Test
    public void testOffsetCreated() throws Exception {
        fixture.givenAggregate(feeTransactionId).published(
                new ExecutedPayCoinTransactionStartedEvent(
                        feeTransactionId,
                        paidFeeId,
                        accountPayableFeeId,
                        offsetId,
                        orderId,
                        orderTransactionId,
                        portfolioId,
                        tradeTime,
                        dueDate,
                        TradeType.BUY,
                        BigMoney.of(CurrencyUnit.EUR, 1009),
                        tradeAmount,
                        BigMoney.of(CurrencyUnit.EUR, 109),
                        orderBookId,
                        coinId))
                .whenAggregate(offsetId).publishes(
                new OffsetCreatedEvent(
                        offsetId,
                        OffsetType.AP_PAID,
                        portfolioId.toString(),
                        ImmutableList.of(new FeeItem(accountPayableFeeId.toString(), FeeItemType.AP, tradeAmount)),
                        ImmutableList.of(new FeeItem(paidFeeId.toString(), FeeItemType.PAID, tradeAmount)),
                        tradeAmount,
                        tradeTime))
                .expectActiveSagas(1)
                .expectDispatchedCommandsEqualTo(
                        new OffsetFeesCommand(offsetId, tradeTime));

    }

    @Test
    public void testOffsetCreatedWithAllConfirmed() throws Exception {
        fixture.givenAggregate(feeTransactionId).published(
                new ExecutedPayCoinTransactionStartedEvent(
                        feeTransactionId,
                        paidFeeId,
                        accountPayableFeeId,
                        offsetId,
                        orderId,
                        orderTransactionId,
                        portfolioId,
                        tradeTime,
                        dueDate,
                        TradeType.BUY,
                        BigMoney.of(CurrencyUnit.EUR, 1009),
                        tradeAmount,
                        BigMoney.of(CurrencyUnit.EUR, 109),
                        orderBookId,
                        coinId),
                new OffsetCreatedEvent(
                        offsetId,
                        OffsetType.AP_PAID,
                        portfolioId.toString(),
                        ImmutableList.of(new FeeItem(accountPayableFeeId.toString(), FeeItemType.AP, tradeAmount)),
                        ImmutableList.of(new FeeItem(paidFeeId.toString(), FeeItemType.PAID, tradeAmount)),
                        tradeAmount,
                        tradeTime),

                new PaidFeeCreatedEvent(
                        paidFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString(),
                        PaidMode.INTERNAL),
                new AccountPayableFeeCreatedEvent(
                        accountPayableFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString()),
                new AccountPayableFeeConfirmedEvent(
                        accountPayableFeeId,
                        tradeTime),
                new PaidFeeConfirmedEvent(
                        paidFeeId,
                        tradeTime))
                .whenAggregate(offsetId).publishes(
                new FeesOffsetedEvent(
                        offsetId,
                        tradeTime
                ))
                .expectActiveSagas(1)
                .expectDispatchedCommandsEqualTo(
                        new OffsetAccountPayableFeeCommand(accountPayableFeeId, tradeTime),
                        new OffsetPaidFeeCommand(paidFeeId, tradeTime));
    }

    @Test
    public void testOffsetCreatedWithAllConfirmed2() throws Exception {
        fixture.givenAggregate(feeTransactionId).published(
                new ExecutedPayCoinTransactionStartedEvent(
                        feeTransactionId,
                        paidFeeId,
                        accountPayableFeeId,
                        offsetId,
                        orderId,
                        orderTransactionId,
                        portfolioId,
                        tradeTime,
                        dueDate,
                        TradeType.BUY,
                        BigMoney.of(CurrencyUnit.EUR, 1009),
                        tradeAmount,
                        BigMoney.of(CurrencyUnit.EUR, 109),
                        orderBookId,
                        coinId),
                new OffsetCreatedEvent(
                        offsetId,
                        OffsetType.AP_PAID,
                        portfolioId.toString(),
                        ImmutableList.of(new FeeItem(accountPayableFeeId.toString(), FeeItemType.AP, tradeAmount)),
                        ImmutableList.of(new FeeItem(paidFeeId.toString(), FeeItemType.PAID, tradeAmount)),
                        tradeAmount,
                        tradeTime),

                new PaidFeeCreatedEvent(
                        paidFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString(),
                        PaidMode.INTERNAL),

                new AccountPayableFeeCreatedEvent(
                        accountPayableFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString()),
                new FeesOffsetedEvent(
                        offsetId,
                        tradeTime
                ),
                new AccountPayableFeeConfirmedEvent(
                        accountPayableFeeId,
                        tradeTime))
                .whenAggregate(paidFeeId).publishes(
                new PaidFeeConfirmedEvent(
                        paidFeeId,
                        tradeTime))
                .expectActiveSagas(1)
                .expectDispatchedCommandsEqualTo(
                        new OffsetAccountPayableFeeCommand(accountPayableFeeId, tradeTime),
                        new OffsetPaidFeeCommand(paidFeeId, tradeTime));
    }


    @Test
    public void testOffsetCreatedWithPartiallyConfirmed() throws Exception {
        fixture.givenAggregate(feeTransactionId).published(
                new ExecutedPayCoinTransactionStartedEvent(
                        feeTransactionId,
                        paidFeeId,
                        accountPayableFeeId,
                        offsetId,
                        orderId,
                        orderTransactionId,
                        portfolioId,
                        tradeTime,
                        dueDate,
                        TradeType.BUY,
                        BigMoney.of(CurrencyUnit.EUR, 1009),
                        tradeAmount,
                        BigMoney.of(CurrencyUnit.EUR, 109),
                        orderBookId,
                        coinId),
                new OffsetCreatedEvent(
                        offsetId,
                        OffsetType.AP_PAID,
                        portfolioId.toString(),
                        ImmutableList.of(new FeeItem(accountPayableFeeId.toString(), FeeItemType.AP, tradeAmount)),
                        ImmutableList.of(new FeeItem(paidFeeId.toString(), FeeItemType.PAID, tradeAmount)),
                        tradeAmount,
                        tradeTime),

                new PaidFeeCreatedEvent(
                        paidFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString(),
                        PaidMode.INTERNAL),

                new AccountPayableFeeCreatedEvent(
                        accountPayableFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString()),
                new PaidFeeConfirmedEvent(
                        paidFeeId,
                        tradeTime))
                .whenAggregate(offsetId).publishes(
                new FeesOffsetedEvent(
                        offsetId,
                        tradeTime
                ))
                .expectActiveSagas(1)
                .expectNoDispatchedCommands();
    }

    @Test
    public void testPayableOffseted() throws Exception {
        fixture.givenAggregate(feeTransactionId).published(
                new ExecutedPayCoinTransactionStartedEvent(
                        feeTransactionId,
                        paidFeeId,
                        accountPayableFeeId,
                        offsetId,
                        orderId,
                        orderTransactionId,
                        portfolioId,
                        tradeTime,
                        dueDate,
                        TradeType.BUY,
                        BigMoney.of(CurrencyUnit.EUR, 1009),
                        tradeAmount,
                        BigMoney.of(CurrencyUnit.EUR, 109),
                        orderBookId,
                        coinId),
                new OffsetCreatedEvent(
                        offsetId,
                        OffsetType.AP_PAID,
                        portfolioId.toString(),
                        ImmutableList.of(new FeeItem(accountPayableFeeId.toString(), FeeItemType.AP, tradeAmount)),
                        ImmutableList.of(new FeeItem(paidFeeId.toString(), FeeItemType.PAID, tradeAmount)),
                        tradeAmount,
                        tradeTime),
                new PaidFeeCreatedEvent(
                        paidFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString(),
                        PaidMode.INTERNAL),
                new AccountPayableFeeCreatedEvent(
                        accountPayableFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString()),
                new PaidFeeConfirmedEvent(
                        paidFeeId,
                        tradeTime),
                new FeesOffsetedEvent(
                        offsetId,
                        tradeTime
                ))
                .whenAggregate(accountPayableFeeId).publishes(
                new AccountPayableFeeOffsetedEvent(
                        accountPayableFeeId,
                        tradeTime
                ))
                .expectActiveSagas(1)
                .expectNoDispatchedCommands();
    }

    @Test
    public void testPaidOffseted() throws Exception {
        fixture.givenAggregate(feeTransactionId).published(
                new ExecutedPayCoinTransactionStartedEvent(
                        feeTransactionId,
                        paidFeeId,
                        accountPayableFeeId,
                        offsetId,
                        orderId,
                        orderTransactionId,
                        portfolioId,
                        tradeTime,
                        dueDate,
                        TradeType.BUY,
                        BigMoney.of(CurrencyUnit.EUR, 1009),
                        tradeAmount,
                        BigMoney.of(CurrencyUnit.EUR, 109),
                        orderBookId,
                        coinId),
                new OffsetCreatedEvent(
                        offsetId,
                        OffsetType.AP_PAID,
                        portfolioId.toString(),
                        ImmutableList.of(new FeeItem(accountPayableFeeId.toString(), FeeItemType.AP, tradeAmount)),
                        ImmutableList.of(new FeeItem(paidFeeId.toString(), FeeItemType.PAID, tradeAmount)),
                        tradeAmount,
                        tradeTime),
                new PaidFeeCreatedEvent(
                        paidFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString(),
                        PaidMode.INTERNAL),
                new AccountPayableFeeCreatedEvent(
                        accountPayableFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString()),
                new PaidFeeConfirmedEvent(
                        paidFeeId,
                        tradeTime),
                new FeesOffsetedEvent(
                        offsetId,
                        tradeTime
                ))
                .whenAggregate(accountPayableFeeId).publishes(
                new PaidFeeOffsetedEvent(
                        accountPayableFeeId,
                        tradeTime
                ))
                .expectActiveSagas(1)
                .expectNoDispatchedCommands();
    }

    @Test
    public void testAllOffseted() throws Exception {
        fixture.givenAggregate(feeTransactionId).published(
                new ExecutedPayCoinTransactionStartedEvent(
                        feeTransactionId,
                        paidFeeId,
                        accountPayableFeeId,
                        offsetId,
                        orderId,
                        orderTransactionId,
                        portfolioId,
                        tradeTime,
                        dueDate,
                        TradeType.BUY,
                        BigMoney.of(CurrencyUnit.EUR, 1009),
                        tradeAmount,
                        BigMoney.of(CurrencyUnit.EUR, 109),
                        orderBookId,
                        coinId),
                new OffsetCreatedEvent(
                        offsetId,
                        OffsetType.AP_PAID,
                        portfolioId.toString(),
                        ImmutableList.of(new FeeItem(accountPayableFeeId.toString(), FeeItemType.AP, tradeAmount)),
                        ImmutableList.of(new FeeItem(paidFeeId.toString(), FeeItemType.PAID, tradeAmount)),
                        tradeAmount,
                        tradeTime),

                new PaidFeeCreatedEvent(
                        paidFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString(),
                        PaidMode.INTERNAL),
                new AccountPayableFeeCreatedEvent(
                        accountPayableFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString()),
                new PaidFeeConfirmedEvent(
                        paidFeeId,
                        tradeTime),
                new FeesOffsetedEvent(
                        offsetId,
                        tradeTime
                ),
                new PaidFeeOffsetedEvent(
                        paidFeeId,
                        tradeTime))
                .whenAggregate(accountPayableFeeId).publishes(
                new AccountPayableFeeOffsetedEvent(
                        accountPayableFeeId,
                        tradeTime
                ))
                .expectActiveSagas(0)
                .expectNoDispatchedCommands();
    }

    @Test
    public void testOffsetAmountNotMatched() throws Exception {
        fixture.givenAggregate(feeTransactionId).published(
                new ExecutedPayCoinTransactionStartedEvent(
                        feeTransactionId,
                        paidFeeId,
                        accountPayableFeeId,
                        offsetId,
                        orderId,
                        orderTransactionId,
                        portfolioId,
                        tradeTime,
                        dueDate,
                        TradeType.BUY,
                        BigMoney.of(CurrencyUnit.EUR, 1009),
                        tradeAmount,
                        BigMoney.of(CurrencyUnit.EUR, 109),
                        orderBookId,
                        coinId),
                new OffsetCreatedEvent(
                        offsetId,
                        OffsetType.AP_PAID,
                        portfolioId.toString(),
                        ImmutableList.of(new FeeItem(accountPayableFeeId.toString(), FeeItemType.AP, tradeAmount)),
                        ImmutableList.of(new FeeItem(paidFeeId.toString(), FeeItemType.PAID, tradeAmount)),
                        tradeAmount,
                        tradeTime),
                new PaidFeeCreatedEvent(
                        paidFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString(),
                        PaidMode.INTERNAL),


                new AccountPayableFeeCreatedEvent(
                        accountPayableFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString()),
                new PaidFeeConfirmedEvent(
                        paidFeeId,
                        tradeTime))
                .whenAggregate(offsetId).publishes(
                new OffsetAmountNotMatchedEvent(
                        offsetId,
                        tradeAmount,
                        BigMoney.of(CurrencyUnit.EUR, 10),
                        BigMoney.of(CurrencyUnit.EUR, 10.6),
                        tradeTime))
                .expectActiveSagas(1)
                .expectDispatchedCommandsEqualTo(
                        new CancelOffsetCommand(offsetId, CancelledReason.AMOUNT_NOT_MATCHED, tradeTime)
                );
    }

    @Test
    public void testOffsetCancelled() throws Exception {
        fixture.givenAggregate(feeTransactionId).published(
                new ExecutedPayCoinTransactionStartedEvent(
                        feeTransactionId,
                        paidFeeId,
                        accountPayableFeeId,
                        offsetId,
                        orderId,
                        orderTransactionId,
                        portfolioId,
                        tradeTime,
                        dueDate,
                        TradeType.BUY,
                        BigMoney.of(CurrencyUnit.EUR, 1009),
                        tradeAmount,
                        BigMoney.of(CurrencyUnit.EUR, 109),
                        orderBookId,
                        coinId),
                new OffsetCreatedEvent(
                        offsetId,
                        OffsetType.AP_PAID,
                        portfolioId.toString(),
                        ImmutableList.of(new FeeItem(accountPayableFeeId.toString(), FeeItemType.AP, tradeAmount)),
                        ImmutableList.of(new FeeItem(paidFeeId.toString(), FeeItemType.PAID, tradeAmount)),
                        tradeAmount,
                        tradeTime),

                new PaidFeeCreatedEvent(
                        paidFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString(),
                        PaidMode.INTERNAL),


                new AccountPayableFeeCreatedEvent(
                        accountPayableFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString()),
                new PaidFeeConfirmedEvent(
                        paidFeeId,
                        tradeTime),
                new OffsetAmountNotMatchedEvent(
                        offsetId,
                        tradeAmount,
                        BigMoney.of(CurrencyUnit.EUR, 10),
                        BigMoney.of(CurrencyUnit.EUR, 10.6),
                        tradeTime))
                .whenAggregate(offsetId).publishes(
                new OffsetCancelledEvent(
                        offsetId,
                        CancelledReason.AMOUNT_NOT_MATCHED,
                        tradeTime))
                .expectActiveSagas(1)
                .expectDispatchedCommandsEqualTo(
                        new CancelAccountPayableFeeCommand(accountPayableFeeId, com.icoin.trading.api.fee.domain.fee.CancelledReason.OFFSET_ERROR, tradeTime),
                        new CancelPaidFeeCommand(paidFeeId, com.icoin.trading.api.fee.domain.fee.CancelledReason.OFFSET_ERROR, tradeTime));
    }

    @Test
    public void testPaidCancelled() throws Exception {
        fixture.givenAggregate(feeTransactionId).published(
                new ExecutedPayCoinTransactionStartedEvent(
                        feeTransactionId,
                        paidFeeId,
                        accountPayableFeeId,
                        offsetId,
                        orderId,
                        orderTransactionId,
                        portfolioId,
                        tradeTime,
                        dueDate,
                        TradeType.BUY,
                        BigMoney.of(CurrencyUnit.EUR, 1009),
                        tradeAmount,
                        BigMoney.of(CurrencyUnit.EUR, 109),
                        orderBookId,
                        coinId),
                new OffsetCreatedEvent(
                        offsetId,
                        OffsetType.AP_PAID,
                        portfolioId.toString(),
                        ImmutableList.of(new FeeItem(accountPayableFeeId.toString(), FeeItemType.AP, tradeAmount)),
                        ImmutableList.of(new FeeItem(paidFeeId.toString(), FeeItemType.PAID, tradeAmount)),
                        tradeAmount,
                        tradeTime),

                new PaidFeeCreatedEvent(
                        paidFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString(),
                        PaidMode.INTERNAL),


                new AccountPayableFeeCreatedEvent(
                        accountPayableFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString()),
                new PaidFeeConfirmedEvent(
                        paidFeeId,
                        tradeTime),
                new OffsetAmountNotMatchedEvent(
                        offsetId,
                        tradeAmount,
                        BigMoney.of(CurrencyUnit.EUR, 10),
                        BigMoney.of(CurrencyUnit.EUR, 10.6),
                        tradeTime),
                new OffsetCancelledEvent(
                        offsetId,
                        CancelledReason.AMOUNT_NOT_MATCHED,
                        tradeTime))
                .whenAggregate(paidFeeId).publishes(
                new PaidFeeCancelledEvent(paidFeeId, com.icoin.trading.api.fee.domain.fee.CancelledReason.OFFSET_ERROR, tradeTime))
                .expectActiveSagas(1)
                .expectNoDispatchedCommands();
    }


    @Test
    public void testPayableCancelled() throws Exception {
        fixture.givenAggregate(feeTransactionId).published(
                new ExecutedPayCoinTransactionStartedEvent(
                        feeTransactionId,
                        paidFeeId,
                        accountPayableFeeId,
                        offsetId,
                        orderId,
                        orderTransactionId,
                        portfolioId,
                        tradeTime,
                        dueDate,
                        TradeType.BUY,
                        BigMoney.of(CurrencyUnit.EUR, 1009),
                        tradeAmount,
                        BigMoney.of(CurrencyUnit.EUR, 109),
                        orderBookId,
                        coinId),
                new OffsetCreatedEvent(
                        offsetId,
                        OffsetType.AP_PAID,
                        portfolioId.toString(),
                        ImmutableList.of(new FeeItem(accountPayableFeeId.toString(), FeeItemType.AP, tradeAmount)),
                        ImmutableList.of(new FeeItem(paidFeeId.toString(), FeeItemType.PAID, tradeAmount)),
                        tradeAmount,
                        tradeTime),

                new PaidFeeCreatedEvent(
                        paidFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString(),
                        PaidMode.INTERNAL),


                new AccountPayableFeeCreatedEvent(
                        accountPayableFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString()),
                new PaidFeeConfirmedEvent(
                        paidFeeId,
                        tradeTime),
                new OffsetAmountNotMatchedEvent(
                        offsetId,
                        tradeAmount,
                        BigMoney.of(CurrencyUnit.EUR, 10),
                        BigMoney.of(CurrencyUnit.EUR, 10.6),
                        tradeTime),
                new OffsetCancelledEvent(
                        offsetId,
                        CancelledReason.AMOUNT_NOT_MATCHED,
                        tradeTime))
                .whenAggregate(accountPayableFeeId).publishes(
                new AccountPayableFeeCancelledEvent(accountPayableFeeId, com.icoin.trading.api.fee.domain.fee.CancelledReason.OFFSET_ERROR, tradeTime))
                .expectActiveSagas(1)
                .expectNoDispatchedCommands();
    }


    @Test
    public void testAllCancelled() throws Exception {
        fixture.givenAggregate(feeTransactionId).published(
                new ExecutedPayCoinTransactionStartedEvent(
                        feeTransactionId,
                        paidFeeId,
                        accountPayableFeeId,
                        offsetId,
                        orderId,
                        orderTransactionId,
                        portfolioId,
                        tradeTime,
                        dueDate,
                        TradeType.BUY,
                        BigMoney.of(CurrencyUnit.EUR, 1009),
                        tradeAmount,
                        BigMoney.of(CurrencyUnit.EUR, 109),
                        orderBookId,
                        coinId),
                new OffsetCreatedEvent(
                        offsetId,
                        OffsetType.AP_PAID,
                        portfolioId.toString(),
                        ImmutableList.of(new FeeItem(accountPayableFeeId.toString(), FeeItemType.AP, tradeAmount)),
                        ImmutableList.of(new FeeItem(paidFeeId.toString(), FeeItemType.PAID, tradeAmount)),
                        tradeAmount,
                        tradeTime),

                new PaidFeeCreatedEvent(
                        paidFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString(),
                        PaidMode.INTERNAL),

                new AccountPayableFeeCreatedEvent(
                        accountPayableFeeId,
                        FeeStatus.PENDING,
                        tradeAmount,
                        FeeType.PAY_COIN,
                        dueDate,
                        tradeTime,
                        portfolioId.toString(),
                        BusinessType.TRADE_EXECUTED,
                        orderTransactionId.toString()),
                new PaidFeeConfirmedEvent(
                        paidFeeId,
                        tradeTime),
                new OffsetAmountNotMatchedEvent(
                        offsetId,
                        tradeAmount,
                        BigMoney.of(CurrencyUnit.EUR, 10),
                        BigMoney.of(CurrencyUnit.EUR, 10.6),
                        tradeTime),
                new OffsetCancelledEvent(
                        offsetId,
                        CancelledReason.AMOUNT_NOT_MATCHED,
                        tradeTime),
                new PaidFeeCancelledEvent(paidFeeId, com.icoin.trading.api.fee.domain.fee.CancelledReason.OFFSET_ERROR, tradeTime)
        )
                .whenAggregate(accountPayableFeeId).publishes(
                new AccountPayableFeeCancelledEvent(accountPayableFeeId, com.icoin.trading.api.fee.domain.fee.CancelledReason.OFFSET_ERROR, tradeTime))
                .expectActiveSagas(0)
                .expectNoDispatchedCommands();
    }
}

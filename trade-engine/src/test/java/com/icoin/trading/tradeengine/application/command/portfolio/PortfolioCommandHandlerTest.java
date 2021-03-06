package com.icoin.trading.tradeengine.application.command.portfolio;

import com.icoin.trading.api.coin.domain.CoinId;
import com.icoin.trading.api.tradeengine.command.portfolio.CreatePortfolioCommand;
import com.icoin.trading.api.tradeengine.command.portfolio.cash.CancelCashReservationCommand;
import com.icoin.trading.api.tradeengine.command.portfolio.cash.ClearReservedCashCommand;
import com.icoin.trading.api.tradeengine.command.portfolio.cash.ConfirmCashReservationCommand;
import com.icoin.trading.api.tradeengine.command.portfolio.cash.DepositCashCommand;
import com.icoin.trading.api.tradeengine.command.portfolio.cash.ReserveCashCommand;
import com.icoin.trading.api.tradeengine.command.portfolio.cash.WithdrawCashCommand;
import com.icoin.trading.api.tradeengine.command.portfolio.coin.AddAmountToPortfolioCommand;
import com.icoin.trading.api.tradeengine.command.portfolio.coin.CancelAmountReservationForPortfolioCommand;
import com.icoin.trading.api.tradeengine.command.portfolio.coin.ConfirmAmountReservationForPortfolioCommand;
import com.icoin.trading.api.tradeengine.command.portfolio.coin.ReserveAmountCommand;
import com.icoin.trading.api.tradeengine.domain.OrderBookId;
import com.icoin.trading.api.tradeengine.domain.PortfolioId;
import com.icoin.trading.api.tradeengine.domain.TransactionId;
import com.icoin.trading.api.tradeengine.events.portfolio.PortfolioCreatedEvent;
import com.icoin.trading.api.tradeengine.events.portfolio.cash.CashDepositedEvent;
import com.icoin.trading.api.tradeengine.events.portfolio.cash.CashReservationCancelledEvent;
import com.icoin.trading.api.tradeengine.events.portfolio.cash.CashReservationConfirmedEvent;
import com.icoin.trading.api.tradeengine.events.portfolio.cash.CashReservationRejectedEvent;
import com.icoin.trading.api.tradeengine.events.portfolio.cash.CashReservedClearedEvent;
import com.icoin.trading.api.tradeengine.events.portfolio.cash.CashReservedEvent;
import com.icoin.trading.api.tradeengine.events.portfolio.cash.CashWithdrawnEvent;
import com.icoin.trading.api.tradeengine.events.portfolio.coin.ItemAddedToPortfolioEvent;
import com.icoin.trading.api.tradeengine.events.portfolio.coin.ItemReservationCancelledForPortfolioEvent;
import com.icoin.trading.api.tradeengine.events.portfolio.coin.ItemReservationConfirmedForPortfolioEvent;
import com.icoin.trading.api.tradeengine.events.portfolio.coin.ItemReservedEvent;
import com.icoin.trading.api.tradeengine.events.portfolio.coin.ItemToReserveNotAvailableInPortfolioEvent;
import com.icoin.trading.api.tradeengine.events.portfolio.coin.NotEnoughItemAvailableToReserveInPortfolio;
import com.icoin.trading.api.users.domain.UserId;
import com.icoin.trading.tradeengine.Constants;
import com.icoin.trading.tradeengine.EqualsWithMoneyFieldMatcher;
import com.icoin.trading.tradeengine.domain.model.coin.Currencies;
import com.icoin.trading.tradeengine.domain.model.portfolio.Portfolio;
import org.axonframework.test.FixtureConfiguration;
import org.axonframework.test.Fixtures;
import org.joda.money.BigMoney;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Date;

import static com.homhon.util.TimeUtils.currentTime;
import static org.axonframework.test.matchers.Matchers.sequenceOf;

/**
 * @author Jettro Coenradie
 */
public class PortfolioCommandHandlerTest {

    private FixtureConfiguration<Portfolio> fixture;
    private PortfolioId portfolioIdentifier;
    private OrderBookId orderBookIdentifier;
    private CoinId coinId;
    private TransactionId transactionIdentifier;
    private UserId userIdentifier;

    @Before
    public void setUp() {
        fixture = Fixtures.newGivenWhenThenFixture(Portfolio.class);
        PortfolioCommandHandler commandHandler = new PortfolioCommandHandler();
        commandHandler.setRepository(fixture.getRepository());
        fixture.registerAnnotatedCommandHandler(commandHandler);
        portfolioIdentifier = new PortfolioId();
        coinId = new CoinId();
        orderBookIdentifier = new OrderBookId();
        transactionIdentifier = new TransactionId();
        userIdentifier = new UserId();
    }

    @Test
    public void testCreatePortfolio() {
        final Date time = currentTime();
        CreatePortfolioCommand command = new CreatePortfolioCommand(portfolioIdentifier, userIdentifier, time);
        fixture.given()
                .when(command)
                .expectEvents(new PortfolioCreatedEvent(portfolioIdentifier, userIdentifier, time));
    }

    /* Items related test methods */
    @Test
    public void testAddItemsToPortfolio() {
        final Date time = currentTime();
        AddAmountToPortfolioCommand command =
                new AddAmountToPortfolioCommand(portfolioIdentifier,
                        coinId,
                        BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(100)),
                        time);
        fixture.given(new PortfolioCreatedEvent(portfolioIdentifier, userIdentifier, time))
                .when(command)
                .expectEvents(
                        new ItemAddedToPortfolioEvent(
                                portfolioIdentifier,
                                coinId,
                                BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(100)),
                                time));
    }

    @Test
    public void testReserveItems_noItemsAvailable() {
        final Date time = currentTime();
        ReserveAmountCommand command = new ReserveAmountCommand(
                portfolioIdentifier,
                coinId,
                transactionIdentifier,
                BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(200)),
                BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(5)),
                time);
        fixture.given(new PortfolioCreatedEvent(portfolioIdentifier, userIdentifier, time))
                .when(command)
                .expectEvents(new ItemToReserveNotAvailableInPortfolioEvent(
                        portfolioIdentifier, coinId, transactionIdentifier, time));
    }

    @Test
    public void testReserveItems_notEnoughItemsAvailable() {
        final Date time = currentTime();
        ReserveAmountCommand command = new ReserveAmountCommand(
                portfolioIdentifier,
                coinId,
                transactionIdentifier,
                BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(200)),
                BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(5)),
                time);
        fixture.given(new PortfolioCreatedEvent(portfolioIdentifier, userIdentifier,
                time),
                new ItemAddedToPortfolioEvent(
                        portfolioIdentifier,
                        coinId,
                        BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(100)),
                        time))
                .when(command)
                .expectEvents(new NotEnoughItemAvailableToReserveInPortfolio(
                        portfolioIdentifier,
                        coinId,
                        transactionIdentifier,
                        Money.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(100)).toBigMoney(),
                        BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(205)),
                        time));
    }

    @Test
    public void testReserveItems() {
        final Date time = currentTime();
        ReserveAmountCommand command = new ReserveAmountCommand(
                portfolioIdentifier,
                coinId,
                transactionIdentifier,
                BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(200)),
                BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(5)),
                time);
        fixture.given(new PortfolioCreatedEvent(portfolioIdentifier, userIdentifier,
                time),
                new ItemAddedToPortfolioEvent(
                        portfolioIdentifier,
                        coinId,
                        BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(400)),
                        time))
                .when(command)
                .expectEvents(new ItemReservedEvent(
                        portfolioIdentifier,
                        coinId,
                        transactionIdentifier,
                        BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(205)),
                        time));
    }

    @Test
    public void testConfirmationOfReservation() {
        final Date time = currentTime();
        ConfirmAmountReservationForPortfolioCommand command =
                new ConfirmAmountReservationForPortfolioCommand(
                        portfolioIdentifier,
                        coinId,
                        transactionIdentifier,
                        BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(100)),
                        BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(5)),
                        time);
        fixture.given(new PortfolioCreatedEvent(portfolioIdentifier, userIdentifier,
                time),
                new ItemAddedToPortfolioEvent(
                        portfolioIdentifier,
                        coinId,
                        BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(400)),
                        time),
                new ItemReservedEvent(portfolioIdentifier,
                        coinId,
                        transactionIdentifier,
                        BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(105)),
                        time))
                .when(command)
                .expectEvents(new ItemReservationConfirmedForPortfolioEvent(
                        portfolioIdentifier,
                        coinId,
                        transactionIdentifier,
                        BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(100)),
                        BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(5)),
                        time));
    }

    @Test
    public void testCancellationOfReservation() {
        final Date time = currentTime();
        CancelAmountReservationForPortfolioCommand command =
                new CancelAmountReservationForPortfolioCommand(
                        portfolioIdentifier,
                        coinId,
                        transactionIdentifier,
                        BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(100)),
                        BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(5)),
                        time);
        fixture.given(new PortfolioCreatedEvent(portfolioIdentifier, userIdentifier, time),
                new ItemAddedToPortfolioEvent(
                        portfolioIdentifier,
                        coinId,
                        BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(400)),
                        time),
                new ItemReservedEvent(portfolioIdentifier,
                        coinId,
                        transactionIdentifier,
                        BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(100)),
                        time))
                .when(command)
                .expectEvents(new ItemReservationCancelledForPortfolioEvent(portfolioIdentifier,
                        coinId,
                        transactionIdentifier,
                        BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(100)),
                        BigMoney.of(CurrencyUnit.of(Currencies.BTC), BigDecimal.valueOf(5)),
                        time));
    }

    /* Money related test methods */
    @Test
    public void testDepositingMoneyToThePortfolio() {
        final Date time = currentTime();
        DepositCashCommand command =
                new DepositCashCommand(
                        portfolioIdentifier,
                        BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 2000L),
                        time);
        fixture.given(new PortfolioCreatedEvent(portfolioIdentifier, userIdentifier,
                time))
                .when(command)
                .expectEvents(new CashDepositedEvent(portfolioIdentifier,
                        BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 2000L),
                        time));
    }

    @Test
    public void testWithdrawingMoneyFromPortfolio() {
        Date current = currentTime();
        WithdrawCashCommand command = new WithdrawCashCommand(
                portfolioIdentifier,
                BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, BigDecimal.valueOf(300)),
                current);
        fixture.given(new PortfolioCreatedEvent(
                portfolioIdentifier,
                userIdentifier,
                current),
                new CashDepositedEvent(
                        portfolioIdentifier,
                        BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 400), current))
                .when(command)
                .expectEventsMatching(sequenceOf(
                        new EqualsWithMoneyFieldMatcher<CashWithdrawnEvent>(
                                new CashWithdrawnEvent(portfolioIdentifier,
                                        BigMoney.ofScale(Constants.DEFAULT_CURRENCY_UNIT, BigDecimal.valueOf(300), 1),
                                        current))
                ));
    }

    @Test
    public void testWithdrawingMoneyFromPortfolio_withoutEnoughMoney() {
        Date current = currentTime();
        WithdrawCashCommand command = new WithdrawCashCommand(portfolioIdentifier,
                BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 300L), current);
        fixture.given(new PortfolioCreatedEvent(portfolioIdentifier, userIdentifier, current),
                new CashDepositedEvent(portfolioIdentifier,
                        BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 200),
                        current))
                .when(command)
                .expectEvents(new CashWithdrawnEvent(portfolioIdentifier,
                        BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 300L), current));
    }

    @Test
    public void testMakingMoneyReservation() {
        final Date time = currentTime();
        ReserveCashCommand command = new ReserveCashCommand(
                portfolioIdentifier,
                transactionIdentifier,
                BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 300L),
                BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 10),
                time);
        fixture.given(new PortfolioCreatedEvent(portfolioIdentifier, userIdentifier,
                time),
                new CashDepositedEvent(portfolioIdentifier,
                        BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 400L),
                        time))
                .when(command)
                .expectEvents(new CashReservedEvent(
                        portfolioIdentifier,
                        transactionIdentifier,
                        BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 300L),
                        BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 10),
                        time));
    }

    @Test
    public void testMakingMoneyReservation_withoutEnoughMoney() {
        final Date time = currentTime();
        ReserveCashCommand command = new ReserveCashCommand(portfolioIdentifier,
                transactionIdentifier,
                BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 600L),
                BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 6),
                time);
        fixture.given(new PortfolioCreatedEvent(portfolioIdentifier, userIdentifier,
                time),
                new CashDepositedEvent(portfolioIdentifier,
                        BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 400),
                        time))
                .when(command)
                .expectEvents(new CashReservationRejectedEvent(portfolioIdentifier,
                        transactionIdentifier,
                        BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 600),
                        BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 6),
                        time));
    }

    @Test
    public void testCancelMoneyReservation() {
        final Date time = currentTime();
        CancelCashReservationCommand command = new CancelCashReservationCommand(
                portfolioIdentifier,
                transactionIdentifier,
                BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 200L),
                BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 5),
                time);
        fixture.given(new PortfolioCreatedEvent(portfolioIdentifier, userIdentifier,
                time),
                new CashDepositedEvent(portfolioIdentifier,
                        BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 400L),
                        time))
                .when(command)
                .expectEvents(
                        new CashReservationCancelledEvent(
                                portfolioIdentifier,
                                transactionIdentifier,
                                BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 200L),
                                BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 5),
                                time));
    }

    @Test
    public void testConfirmMoneyReservation() {
        final Date time = currentTime();
        ConfirmCashReservationCommand command = new ConfirmCashReservationCommand(
                portfolioIdentifier,
                transactionIdentifier,
                BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 500L),
                BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 200L),
                time);
        fixture.given(
                new PortfolioCreatedEvent(portfolioIdentifier, userIdentifier,
                        time),
                new CashDepositedEvent(portfolioIdentifier,
                        BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 700L),
                        time))
                .when(command)
                .expectEvents(new CashReservationConfirmedEvent(
                        portfolioIdentifier,
                        transactionIdentifier,
                        BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 500),
                        BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 200),
                        time));
    }

    @Test
    public void testClearReservedCash() {
        final Date time = currentTime();
        ClearReservedCashCommand command = new ClearReservedCashCommand(
                portfolioIdentifier,
                transactionIdentifier,
                orderBookIdentifier,
                BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 5L),
                BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 2L),
                time);
        fixture.given(
                new PortfolioCreatedEvent(portfolioIdentifier, userIdentifier,
                        time),
                new CashDepositedEvent(portfolioIdentifier,
                        BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 700L),
                        time),
                new CashReservationConfirmedEvent(
                        portfolioIdentifier,
                        transactionIdentifier,
                        BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 495),
                        BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 198),
                        time))
                .when(command)
                .expectEvents(new CashReservedClearedEvent(
                        portfolioIdentifier,
                        transactionIdentifier,
                        BigMoney.of(Constants.DEFAULT_CURRENCY_UNIT, 7L),
                        time));
    }
}
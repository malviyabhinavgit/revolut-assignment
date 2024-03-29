package com.revolut.money.transfer.api.repository.inmemory;

import com.revolut.money.transfer.api.model.Account;
import com.revolut.money.transfer.api.model.Transaction;
import com.revolut.money.transfer.api.model.User;
import com.revolut.money.transfer.api.repository.AccountRepository;
import com.revolut.money.transfer.api.repository.TransactionRepository;
import com.revolut.money.transfer.api.validation.TransactionValidation;
import com.revolut.money.transfer.api.validation.exception.NotEnoughMoneyException;
import java.util.Optional;
import java.util.UUID;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InMemoryTransactionRepositoryTest {

  private TransactionRepository transactionRepository;

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private TransactionValidation transactionValidation;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() {
    transactionRepository = new InMemoryTransactionRepository(
        accountRepository, transactionValidation
    );
  }

  @Test
  public void shouldNotCommitTransactionWhenValidationDetectedError() throws Exception {
    // given
    Account sender = createSenderAccount("AC1", Money.of(CurrencyUnit.EUR, 100));
    Account receiver = createReceiverAccount("AC2", Money.of(CurrencyUnit.EUR, 50));

    Transaction transaction = Transaction
        .builder()
        .id("TR1")
        .from(sender)
        .to(receiver)
        .money(Money.of(CurrencyUnit.EUR, 600))
        .build();

    when(transactionValidation.validate(transaction)).thenReturn(
        Optional.of(new NotEnoughMoneyException(sender.number()))
    );

    // when
    expectedException.expect(NotEnoughMoneyException.class);
    expectedException.expectMessage(new NotEnoughMoneyException(sender.number()).getMessage());

    // then
    transactionRepository.commit(transaction);
  }

  @Test
  @SuppressWarnings("OptionalGetWithoutIsPresent") // it's not relevant for this test
  public void shouldCommitTransaction() throws Exception {
    // given
    Account sender = spy(createSenderAccount("AC1", Money.of(CurrencyUnit.EUR, 100)));
    Account receiver = spy(createReceiverAccount("AC2", Money.of(CurrencyUnit.EUR, 50)));

    Transaction transaction = Transaction
        .builder()
        .id("TR1")
        .from(sender)
        .to(receiver)
        .money(Money.of(CurrencyUnit.EUR, 10))
        .build();

    when(transactionValidation.validate(transaction)).thenReturn(Optional.empty());
    when(accountRepository.get(sender.number())).thenReturn(Optional.of(sender));
    when(accountRepository.get(receiver.number())).thenReturn(Optional.of(receiver));

    // when
    transactionRepository.commit(transaction);

    // then
    verify(accountRepository).withdrawMoney(sender, transaction.money());
    verify(accountRepository).putMoney(receiver, transaction.money());
    assertThat(transactionRepository.get().isEmpty()).isFalse();
  }

  @Test
  @SuppressWarnings("OptionalGetWithoutIsPresent") // it's not relevant for this test
  public void shouldGetCreatedTransaction() throws Exception {
    // given
    Account sender = createSenderAccount("AC1", Money.of(CurrencyUnit.EUR, 100));
    Account receiver = createReceiverAccount("AC2", Money.of(CurrencyUnit.EUR, 50));

    Transaction transaction = Transaction
        .builder()
        .id("TR1")
        .from(sender)
        .to(receiver)
        .money(Money.of(CurrencyUnit.EUR, 10))
        .build();

    when(transactionValidation.validate(transaction)).thenReturn(Optional.empty());
    when(accountRepository.get(sender.number())).thenReturn(Optional.of(sender));
    when(accountRepository.get(receiver.number())).thenReturn(Optional.of(receiver));

    // when
    transactionRepository.commit(transaction);

    // then
    Transaction createdTransaction = transactionRepository.get(transaction.id()).get();
    assertThat(createdTransaction.equals(transaction)).isTrue();
    assertThat(createdTransaction.id()).isEqualTo(transaction.id());
    assertThat(createdTransaction.from()).isEqualTo(transaction.from());
    assertThat(createdTransaction.to()).isEqualTo(transaction.to());
    assertThat(createdTransaction.money()).isEqualTo(transaction.money());
  }

  @Test
  public void shouldGetAllTransactions() throws Exception {
    // given
    Account sender = createSenderAccount("AC1", Money.of(CurrencyUnit.EUR, 100));
    Account receiver = createReceiverAccount("AC2", Money.of(CurrencyUnit.EUR, 50));

    Transaction transactionOne = Transaction
        .builder()
        .id("TR1")
        .from(sender)
        .to(receiver)
        .money(Money.of(CurrencyUnit.EUR, 10))
        .build();

    Transaction transactionTwo = Transaction
        .builder()
        .id("TR2")
        .from(sender)
        .to(receiver)
        .money(Money.of(CurrencyUnit.EUR, 10))
        .build();

    when(transactionValidation.validate(transactionOne)).thenReturn(Optional.empty());
    when(transactionValidation.validate(transactionTwo)).thenReturn(Optional.empty());
    when(accountRepository.get(sender.number())).thenReturn(Optional.of(sender));
    when(accountRepository.get(receiver.number())).thenReturn(Optional.of(receiver));

    // when
    transactionRepository.commit(transactionOne);
    transactionRepository.commit(transactionTwo);

    // then
    assertThat(transactionRepository.get().size()).isEqualTo(2);
  }

  @Test
  @SuppressWarnings("OptionalGetWithoutIsPresent") // it's not relevant for this test
  public void shouldClearTransactions() throws Exception {
    // given
    Account sender = createSenderAccount("AC1", Money.of(CurrencyUnit.EUR, 100));
    Account receiver = createReceiverAccount("AC2", Money.of(CurrencyUnit.EUR, 50));

    Transaction transaction = Transaction
        .builder()
        .id("TR1")
        .from(sender)
        .to(receiver)
        .money(Money.of(CurrencyUnit.EUR, 10))
        .build();

    when(transactionValidation.validate(transaction)).thenReturn(Optional.empty());
    when(accountRepository.get(sender.number())).thenReturn(Optional.of(sender));
    when(accountRepository.get(receiver.number())).thenReturn(Optional.of(receiver));

    // when
    transactionRepository.commit(transaction);
    transactionRepository.commit(transaction);
    transactionRepository.clear();

    // then
    assertThat(transactionRepository.get().isEmpty()).isTrue();
  }

  @Test
  public void shouldGetErrorWhenSenderBalanceIsLessThanMoneyToBeSend() throws Exception {
    // given
    Account sender = createSenderAccount("PL1", Money.of(CurrencyUnit.EUR, 100));
    Account receiver = createReceiverAccount("PL2", Money.of(CurrencyUnit.EUR, 0));

    when(accountRepository.get(sender.number())).thenReturn(Optional.of(sender));
    when(accountRepository.get(receiver.number())).thenReturn(Optional.of(receiver));

    Transaction transaction = Transaction
        .builder()
        .id("TR1")
        .from(sender)
        .to(receiver)
        .money(Money.of(CurrencyUnit.EUR, 120))
        .build();

    // when
    expectedException.expect(NotEnoughMoneyException.class);
    expectedException.expectMessage(new NotEnoughMoneyException(sender.number()).getMessage());

    // then
    transactionRepository.commit(transaction);
  }

  private Account createSenderAccount(final String number, final Money money) {
    return Account
        .builder()
        .user(createSenderUser())
        .number(number)
        .money(money)
        .build();
  }

  private User createSenderUser() {
    return User
        .builder()
        .id(UUID.randomUUID().toString())
        .name("TestSender")
        .surname("TestSenderSurname")
        .build();
  }

  private Account createReceiverAccount(final String number, final Money money) {
    return Account
        .builder()
        .user(createReceiverUser())
        .number(number)
        .money(money)
        .build();
  }

  private User createReceiverUser() {
    return User
        .builder()
        .id(UUID.randomUUID().toString())
        .name("testReceiver")
        .surname("testReceiverSurname")
        .build();
  }
}
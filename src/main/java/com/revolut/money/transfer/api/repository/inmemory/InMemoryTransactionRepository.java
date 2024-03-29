package com.revolut.money.transfer.api.repository.inmemory;

import com.revolut.money.transfer.api.model.Account;
import com.revolut.money.transfer.api.model.Transaction;
import com.revolut.money.transfer.api.repository.AccountRepository;
import com.revolut.money.transfer.api.repository.TransactionRepository;
import com.revolut.money.transfer.api.validation.TransactionValidation;
import com.revolut.money.transfer.api.validation.exception.NotEnoughMoneyException;

import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

public class InMemoryTransactionRepository implements TransactionRepository {

  private static final Random random = new Random();
  private static final long FIXED_DELAY = 1;
  private static final long RANDOM_DELAY = 2;
  private static final long TIMEOUT = TimeUnit.SECONDS.toNanos(2);

  private final Queue<Transaction> transactions = new LinkedList<>();
  private AccountRepository accountRepository;
  private TransactionValidation transactionValidation;

  @Inject
  public InMemoryTransactionRepository(final AccountRepository accountRepository,
      final TransactionValidation transactionValidation) {
    this.accountRepository = accountRepository;
    this.transactionValidation = transactionValidation;
  }

  @Override public Optional<Transaction> get(final String id) {
    return transactions
        .stream()
        .filter(transaction -> transaction.id().equals(id))
        .findFirst();
  }

  @Override public Queue<Transaction> get() {
    return transactions;
  }

  @Override
  public Transaction commit(final Transaction transaction) throws Exception {

    Account sender;
    Account receiver;

    long stopTime = System.nanoTime() + TIMEOUT;

    while (transaction.isRunning().get()) {
      synchronized (this) {
        final Optional<Exception> error = transactionValidation.validate(transaction);
        if (error.isPresent()) {
          throw error.get();
        }

        //noinspection OptionalGetWithoutIsPresent
        sender = accountRepository.get(transaction.from().number()).get();
        //noinspection OptionalGetWithoutIsPresent
        receiver = accountRepository.get(transaction.to().number()).get();

        if (sender.money().isLessThan(transaction.money())) {
          throw new NotEnoughMoneyException(sender.number());
        }
      }

      if (sender.lock().tryLock()) {
        try {
          if (receiver.lock().tryLock()) {
            try {
              accountRepository.withdrawMoney(sender, transaction.money());
              accountRepository.putMoney(receiver, transaction.money());
              transactions.add(transaction);
              transaction.isRunning().set(false);
            } finally {
              receiver.lock().unlock();
            }
          }
        } finally {
          sender.lock().unlock();
        }
      }

      if (System.nanoTime() > stopTime) {
        transaction.isRunning().set(false);
      }
      try {
        TimeUnit.NANOSECONDS.sleep(FIXED_DELAY + random.nextLong() % RANDOM_DELAY);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(exception);
      }
    }

    return transaction;
  }

  @Override public void clear() {
    transactions.clear();
  }
}

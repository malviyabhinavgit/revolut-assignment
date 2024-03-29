package com.revolut.money.transfer.api.validation.implementation;

import com.revolut.money.transfer.api.model.Transaction;
import com.revolut.money.transfer.api.repository.AccountRepository;
import com.revolut.money.transfer.api.validation.TransactionValidation;
import com.revolut.money.transfer.api.validation.exception.AccountNotExistsException;
import com.revolut.money.transfer.api.validation.exception.DifferentCurrencyException;
import com.revolut.money.transfer.api.validation.exception.TransferToTheSameAccountException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;

public class DefaultTransactionValidation implements TransactionValidation {

  private AccountRepository accountRepository;

  @Inject
  public DefaultTransactionValidation(final AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  @Override public Optional<Exception> validate(final Transaction transaction) {
    return createCommitValidationRules(transaction)
        .entrySet()
        .stream()
        .filter(entry -> entry.getKey().equals(Boolean.TRUE))
        .findFirst()
        .map(Map.Entry::getValue);
  }

  private Map<Boolean, Exception> createCommitValidationRules(final Transaction transaction) {
    final Map<Boolean, Exception> rules = new HashMap<>();

    rules.put(
        !accountRepository.get(transaction.from().number()).isPresent(),
        new AccountNotExistsException(transaction.from().number())
    );

    rules.put(
        !accountRepository.get(transaction.to().number()).isPresent(),
        new AccountNotExistsException(transaction.to().number())
    );

    rules.put(
        !transaction.from().money().isSameCurrency(transaction.to().money()),
        new DifferentCurrencyException(transaction.from().number(), transaction.to().number())
    );

    rules.put(
        transaction.from().number().equals(transaction.to().number()),
        new TransferToTheSameAccountException()
    );

    return rules;
  }
}

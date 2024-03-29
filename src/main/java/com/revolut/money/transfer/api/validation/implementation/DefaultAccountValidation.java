package com.revolut.money.transfer.api.validation.implementation;

import com.revolut.money.transfer.api.model.Account;
import com.revolut.money.transfer.api.validation.AccountValidation;
import com.revolut.money.transfer.api.validation.exception.EmptyAccountNumberException;
import com.revolut.money.transfer.api.validation.exception.EmptyUserIdException;
import com.revolut.money.transfer.api.validation.exception.EmptyUserNameException;
import com.revolut.money.transfer.api.validation.exception.EmptyUserSurnameException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DefaultAccountValidation implements AccountValidation {

  @Override public Optional<Exception> validate(final Account account) {
    return createAccountValidationRules(account)
        .entrySet()
        .stream()
        .filter(entry -> entry.getKey().equals(Boolean.TRUE))
        .findFirst()
        .map(Map.Entry::getValue);
  }

  private Map<Boolean, Exception> createAccountValidationRules(final Account account) {
    final Map<Boolean, Exception> rules = new HashMap<>();

    rules.put(
        account.number() == null || account.number().isEmpty(),
        new EmptyAccountNumberException()
    );

    rules.put(
        account.user().id() == null || account.user().id().isEmpty(),
        new EmptyUserIdException()
    );

    rules.put(
        account.user().name() == null || account.user().name().isEmpty(),
        new EmptyUserNameException()
    );

    rules.put(
        account.user().surname() == null || account.user().surname().isEmpty(),
        new EmptyUserSurnameException()
    );

    return rules;
  }
}

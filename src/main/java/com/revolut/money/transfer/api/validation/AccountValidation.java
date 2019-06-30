package com.revolut.money.transfer.api.validation;

import com.revolut.money.transfer.api.model.Account;
import java.util.Optional;

public interface AccountValidation {
  Optional<Exception> validate(Account account);
}

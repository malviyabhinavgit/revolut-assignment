package com.revolut.money.transfer.api.validation;

import com.revolut.money.transfer.api.model.Transaction;
import java.util.Optional;

public interface TransactionValidation {
  Optional<Exception> validate(Transaction transaction);
}

package com.revolut.money.transfer.api.validation.exception;

public class EmptyAccountNumberException extends RuntimeException {
  @Override public String getMessage() {
    return "Empty account number";
  }
}

package com.revolut.money.transfer.api.validation.exception;

public class AccountAlreadyExistsException extends RuntimeException {

  private final String number;

  public AccountAlreadyExistsException(String number) {
    this.number = number;
  }

  @Override public String getMessage() {
    return String.format("Account with number %s already exists", number);
  }
}

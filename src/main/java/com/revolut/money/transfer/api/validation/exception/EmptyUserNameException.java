package com.revolut.money.transfer.api.validation.exception;

public class EmptyUserNameException extends RuntimeException {
  @Override public String getMessage() {
    return "User name is empty";
  }
}

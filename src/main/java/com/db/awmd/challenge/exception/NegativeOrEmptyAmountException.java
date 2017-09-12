package com.db.awmd.challenge.exception;

public class NegativeOrEmptyAmountException extends RuntimeException {

  public NegativeOrEmptyAmountException(String message) {
  	super(message);
  }
}

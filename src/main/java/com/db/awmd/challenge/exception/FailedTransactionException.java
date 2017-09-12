package com.db.awmd.challenge.exception;

public class FailedTransactionException extends RuntimeException {
	
	public FailedTransactionException(String message) {
		super(message);
	}
}

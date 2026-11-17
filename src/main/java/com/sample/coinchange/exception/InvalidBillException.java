package com.sample.coinchange.exception;

/**
 * Business-rule violation: the bill is a valid number, but not one we accept.
 **/
public class InvalidBillException extends RuntimeException {

    public InvalidBillException(int bill) {
        super("Invalid bill: " + bill + ". Accepted bills are 1, 2, 5, 10, 20, 50, 100.");
    }
}

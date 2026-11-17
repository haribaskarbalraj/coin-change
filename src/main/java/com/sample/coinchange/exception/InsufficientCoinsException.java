package com.sample.coinchange.exception;

/**
 * Business-rule violation: the request is valid, but the coins currently on hand
 * can't add up to exactly the bill's value.
 **/
public class InsufficientCoinsException extends RuntimeException {

    public InsufficientCoinsException(int bill) {
        super("Not enough coins to make change for $" + bill + ". Please try a smaller bill.");
    }
}

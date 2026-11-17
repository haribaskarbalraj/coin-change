package com.sample.coinchange.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public enum CoinType {
    PENNY(new BigDecimal("0.01")),
    NICKEL(new BigDecimal("0.05")),
    DIME(new BigDecimal("0.10")),
    QUARTER(new BigDecimal("0.25"));

    @JsonValue
    private final BigDecimal amount;

    private final int cents;

    CoinType(BigDecimal amount) {
        this.amount = amount;
        // intValueExact() throws on fractions of a cent, so a typo fails at startup.
        this.cents = amount.movePointRight(2).intValueExact();
    }
}

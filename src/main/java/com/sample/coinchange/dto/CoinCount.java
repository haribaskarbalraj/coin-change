package com.sample.coinchange.dto;

import lombok.Value;

import java.math.BigDecimal;

/** One response line, e.g. {"coin":"QUARTER","value":0.25,"count":4}. */
@Value
public class CoinCount {

    String coin;
    BigDecimal value;
    int count;

    public static CoinCount of(CoinType coin, int count) {
        return new CoinCount(coin.name(), coin.getAmount(), count);
    }
}

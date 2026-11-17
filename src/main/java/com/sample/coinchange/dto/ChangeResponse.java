package com.sample.coinchange.dto;

import lombok.Value;

import java.util.List;

@Value
public class ChangeResponse {

    int bill;
    int totalCoins;
    List<CoinCount> change;
}

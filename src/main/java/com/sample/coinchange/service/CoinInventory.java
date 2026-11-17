package com.sample.coinchange.service;

import com.sample.coinchange.dto.CoinType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds how many coins of each type the machine has, in memory.
 */
@Slf4j
@Component
public class CoinInventory {

    private final Map<CoinType, Integer> coinsMap = new HashMap<>();
    private final int initialCoinCount;

    /**
      initial-coin-count from application.properties
     */
    public CoinInventory(@Value("${coin-change.initial-coin-count:100}") int initialCoinCount) {
        if (initialCoinCount < 0) {
            throw new IllegalArgumentException("coin-change.initial-coin-count must not be negative: " + initialCoinCount);
        }
        this.initialCoinCount = initialCoinCount;
        for (CoinType coin : CoinType.values()) {
            coinsMap.put(coin, initialCoinCount);
        }
        log.info("Coin inventory initialised: {}", coinsMap);
    }

    /**
     * Returns a COPY of the counts.
     */
    Map<CoinType, Integer> snapshot() {
        return new HashMap<>(coinsMap);
    }

    /**
     * Removes the given coins from the inventory.
     */
    public void deduct(Map<CoinType, Integer> toRemove) {
        // calculator should never ask for greater value if it happens, it's a bug and fail deduct.
        for (Map.Entry<CoinType, Integer> entry : toRemove.entrySet()) {
            int have = coinsMap.get(entry.getKey());
            if (entry.getValue() > have) {
                throw new IllegalStateException(
                        "Cannot remove " + entry.getValue() + " " + entry.getKey() + ", only " + have + " left");
            }
        }
        // safe to change the state.
        for (Map.Entry<CoinType, Integer> entry : toRemove.entrySet()) {
            coinsMap.merge(entry.getKey(), -entry.getValue(), Integer::sum);
        }
        log.info("Deducted {}, inventory now {}", toRemove, coinsMap);
    }
}

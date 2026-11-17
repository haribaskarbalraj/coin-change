package com.sample.coinchange.service;

import com.sample.coinchange.dto.CoinType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.sample.coinchange.dto.CoinType.DIME;
import static com.sample.coinchange.dto.CoinType.NICKEL;
import static com.sample.coinchange.dto.CoinType.PENNY;
import static com.sample.coinchange.dto.CoinType.QUARTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class CoinInventoryTest {

    private CoinInventory inventory;

    @BeforeEach
    void setUp() {
        inventory = new CoinInventory(100);
    }

    @Test
    void startsWithConfiguredCountOfEveryCoinType() {
        assertThat(inventory.snapshot())
                .containsOnlyKeys(CoinType.values())
                .containsValues(100);
        assertThat(inventory.snapshot().values()).containsOnly(100);
    }

    @Test
    void deductReducesOnlyTheGivenCoins() {
        inventory.deduct(Map.of(QUARTER, 4, DIME, 1));

        assertThat(inventory.snapshot()).containsEntry(QUARTER, 96)
                .containsEntry(DIME, 99)
                .containsEntry(NICKEL, 100)
                .containsEntry(PENNY, 100);
    }

    @Test
    void stateIsKeptBetweenCalls() {
        inventory.deduct(Map.of(QUARTER, 10));
        inventory.deduct(Map.of(QUARTER, 10));

        assertThat(inventory.snapshot()).containsEntry(QUARTER, 80);
    }

    @Test
    void snapshotIsACopy() {
        inventory.snapshot().put(QUARTER, 9999); // changing the copy...

        assertThat(inventory.snapshot()).containsEntry(QUARTER, 100); // ...does not change the inventory
    }

    @Test
    void deductingMoreThanWeHaveFailsAndChangesNothing() {
        // DIME is fine (1 <= 100) but QUARTER is not (101 > 100): nothing may be deducted, not even the dimes.
        assertThatIllegalStateException()
                .isThrownBy(() -> inventory.deduct(Map.of(DIME, 1, QUARTER, 101)));

        assertThat(inventory.snapshot().values()).containsOnly(100);
    }

    @Test
    void negativeStartingCountIsRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> new CoinInventory(-1));
    }
}

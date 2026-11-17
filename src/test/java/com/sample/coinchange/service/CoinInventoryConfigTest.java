package com.sample.coinchange.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks that @Value really reads the property from application.properties (or an override). */
class CoinInventoryConfigTest {

    @Nested
    @SpringBootTest
    class FromApplicationProperties {
        @Autowired
        CoinInventory inventory;

        @Test
        void startsWithTheValueInApplicationProperties() {
            assertThat(inventory.snapshot().values()).hasSize(4).containsOnly(100);
        }
    }

    @Nested
    @SpringBootTest(properties = "coin-change.initial-coin-count=7")
    class Overridden {
        @Autowired
        CoinInventory inventory;

        @Test
        void propertyCanBeOverridden() {
            assertThat(inventory.snapshot().values()).hasSize(4).containsOnly(7);
        }
    }
}

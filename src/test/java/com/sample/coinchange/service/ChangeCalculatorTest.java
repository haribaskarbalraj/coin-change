package com.sample.coinchange.service;

import com.sample.coinchange.dto.CoinType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static com.sample.coinchange.dto.CoinType.DIME;
import static com.sample.coinchange.dto.CoinType.NICKEL;
import static com.sample.coinchange.dto.CoinType.PENNY;
import static com.sample.coinchange.dto.CoinType.QUARTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Plain unit tests: no Spring, no HTTP. We create the calculator with 'new' and call it.
 * Each test gives a coin inventory and an amount (in cents) and checks the result.
 */
class ChangeCalculatorTest {

    private final ChangeCalculator calculator = new ChangeCalculator();

    /** Helper: build an inventory. Argument order is biggest coin first. */
    private static Map<CoinType, Integer> coins(int quarters, int dimes, int nickels, int pennies) {
        Map<CoinType, Integer> inventory = new HashMap<>();
        inventory.put(QUARTER, quarters);
        inventory.put(DIME, dimes);
        inventory.put(NICKEL, nickels);
        inventory.put(PENNY, pennies);
        return inventory;
    }

    @Test
    void usesBiggestCoinsWhenSupplyIsPlentiful() {
        // $1 = 100 cents = 4 quarters
        assertThat(calculator.calculate(100, coins(100, 100, 100, 100)))
                .contains(Map.of(QUARTER, 4));
    }

    @Test
    void usesSmallerCoinsWhenBiggerOnesRunOut() {
        // No quarters left: 100 cents = 10 dimes
        assertThat(calculator.calculate(100, coins(0, 100, 100, 100)))
                .contains(Map.of(DIME, 10));
    }

    @Test
    void mixesCoinTypesWhenSupplyIsPartial() {
        // Only 2 quarters (50) + 5 dimes (50) = 100 cents with 7 coins
        assertThat(calculator.calculate(100, coins(2, 5, 100, 100)))
                .contains(Map.of(QUARTER, 2, DIME, 5));
    }

    @Test
    void findsAnswerWhereGreedyWouldGetStuck() {
        // Greedy takes the quarter and is stuck with 5 cents. Correct answer: 3 dimes.
        assertThat(calculator.calculate(30, coins(1, 3, 0, 0)))
                .contains(Map.of(DIME, 3));
    }

    @Test
    void usesFewerCoinsThanGreedy() {
        // Greedy: quarter + 5 pennies = 6 coins. Best: 3 dimes = 3 coins.
        assertThat(calculator.calculate(30, coins(1, 3, 0, 5)))
                .contains(Map.of(DIME, 3));
    }

    @Test
    void returnsEmptyWhenAllCoinsTogetherAreWorthTooLittle() {
        // A full machine holds 100 x (25+10+5+1) = 4100 cents = $41. $100 is impossible.
        assertThat(calculator.calculate(10_000, coins(100, 100, 100, 100))).isEmpty();
    }

    @Test
    void returnsEmptyWhenExactAmountCannotBeMade() {
        // Enough value (25 >= 3) but a quarter can't make exactly 3 cents.
        assertThat(calculator.calculate(3, coins(1, 0, 0, 0))).isEmpty();
    }

    @Test
    void zeroAmountNeedsNoCoins() {
        assertThat(calculator.calculate(0, coins(0, 0, 0, 0))).contains(Map.of());
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> calculator.calculate(-1, coins(1, 1, 1, 1)));
    }

    /**
     * Correctness proof: compare the DP result with a brute force that simply tries EVERY combination
     * (slow, but obviously correct). Thousands of random inventories and amounts must agree on
     * (a) whether change is possible and (b) the fewest number of coins.
     * The seed is fixed, so a failure can be reproduced.
     */
    @Test
    void matchesBruteForceOnRandomInputs() {
        Random random = new Random(42);

        for (int round = 0; round < 3000; round++) {
            int quarters = random.nextInt(6), dimes = random.nextInt(6);
            int nickels = random.nextInt(6), pennies = random.nextInt(6);
            int amount = random.nextInt(201);
            Map<CoinType, Integer> inventory = coins(quarters, dimes, nickels, pennies);

            Optional<Map<CoinType, Integer>> actual = calculator.calculate(amount, inventory);
            int expectedFewest = bruteForceFewestCoins(amount, quarters, dimes, nickels, pennies);

            String input = "amount=" + amount + " inventory=" + inventory;
            if (expectedFewest == -1) {
                assertThat(actual).as(input).isEmpty();
            } else {
                assertThat(actual).as(input).isPresent();
                Map<CoinType, Integer> used = actual.get();
                int coinCount = 0, cents = 0;
                for (Map.Entry<CoinType, Integer> entry : used.entrySet()) {
                    assertThat(entry.getValue()).as(input + " uses more " + entry.getKey() + " than available")
                            .isLessThanOrEqualTo(inventory.get(entry.getKey()));
                    coinCount += entry.getValue();
                    cents += entry.getValue() * entry.getKey().getCents();
                }
                assertThat(cents).as(input + " must add up exactly").isEqualTo(amount);
                assertThat(coinCount).as(input + " must use the fewest coins").isEqualTo(expectedFewest);
            }
        }
    }

    /** Tries every possible count of each coin. Returns the fewest coins, or -1 if the amount can't be made. */
    private static int bruteForceFewestCoins(int amount, int quarters, int dimes, int nickels, int pennies) {
        int fewest = -1;
        for (int q = 0; q <= quarters; q++) {
            for (int d = 0; d <= dimes; d++) {
                for (int n = 0; n <= nickels; n++) {
                    int pennyCents = amount - q * 25 - d * 10 - n * 5;   // what pennies would have to cover
                    if (pennyCents >= 0 && pennyCents <= pennies) {
                        int total = q + d + n + pennyCents;
                        if (fewest == -1 || total < fewest) {
                            fewest = total;
                        }
                    }
                }
            }
        }
        return fewest;
    }
}

package com.sample.coinchange.service;

import com.sample.coinchange.dto.CoinType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Finds the fewest coins that make an exact amount (bottom-up DP over coin types); stateless, so thread-safe. */
@Slf4j
@Component
public class ChangeCalculator {

    /** Marker for "this amount can't be made"; MAX_VALUE so it never wins a min. */
    private static final int IMPOSSIBLE = Integer.MAX_VALUE;

    /** Returns the coins to give (count > 0 only) for {@code amountCents}, or empty if it can't be made exactly. */
    public Optional<Map<CoinType, Integer>> calculate(int amountCents, Map<CoinType, Integer> availableCoins) {

        if (amountCents < 0) {
            throw new IllegalArgumentException("Amount must not be negative: " + amountCents);
        }

        long totalValueOnHand = totalValueInCents(availableCoins);
        if (totalValueOnHand < amountCents) {
            return Optional.empty();
        }

        CoinType[] coins = CoinType.values();
        int coinTypes = coins.length;

        // dp[row][a] = fewest coins to make a cents using only the first `row` coin types.
        int[][] dp = new int[coinTypes + 1][amountCents + 1];

        Arrays.fill(dp[0], IMPOSSIBLE);
        dp[0][0] = 0;

        for (int row = 1; row <= coinTypes; row++) {
            CoinType coin = coins[row - 1];
            int value = coin.getCents();
            int specificCoinAvailability = availableCoins.getOrDefault(coin, 0);

            for (int amount = 0; amount <= amountCents; amount++) {
                int fewest = IMPOSSIBLE;

                // Try k coins of this type, limited by stock and by the amount.
                int maxK = Math.min(specificCoinAvailability, amount / value);
                for (int k = 0; k <= maxK; k++) {
                    int coinsForRest = dp[row - 1][amount - k * value];
                    if (coinsForRest != IMPOSSIBLE && coinsForRest + k < fewest) {
                        fewest = coinsForRest + k;
                    }
                }
                dp[row][amount] = fewest;
            }
        }

        int fewestCoins = dp[coinTypes][amountCents];
        if (fewestCoins == IMPOSSIBLE) {
            return Optional.empty();
        }

        Map<CoinType, Integer> result = workOutWhichCoins(dp, coins, availableCoins, amountCents);
        log.debug("Fewest coins for {} cents is {}: {}", amountCents, fewestCoins, result);
        return Optional.of(result);
    }

    /** Walks the table backwards from the last row to recover WHICH coins were used. */
    private Map<CoinType, Integer> workOutWhichCoins(int[][] dp, CoinType[] coins,
                                                     Map<CoinType, Integer> available, int amountCents) {
        Map<CoinType, Integer> result = new HashMap<>();
        int remaining = amountCents;

        for (int row = coins.length; row >= 1; row--) {
            CoinType coin = coins[row - 1];
            int value = coin.getCents();
            int maxK = Math.min(available.getOrDefault(coin, 0), remaining / value);

            // Pick the first k that explains the stored count; counting down prefers larger coins on ties.
            for (int k = maxK; k >= 0; k--) {
                int coinsForRest = dp[row - 1][remaining - k * value];
                boolean explainsStoredAnswer =
                        coinsForRest != IMPOSSIBLE && coinsForRest + k == dp[row][remaining];
                if (explainsStoredAnswer) {
                    if (k > 0) {
                        result.put(coin, k);
                    }
                    remaining -= k * value;
                    break;
                }
            }
        }
        return result;
    }

    /** Total worth, in cents, of a coin map. */
    public static long totalValueInCents(Map<CoinType, Integer> coins) {
        long total = 0;
        for (Map.Entry<CoinType, Integer> entry : coins.entrySet()) {
            total += (long) entry.getKey().getCents() * entry.getValue();
        }
        return total;
    }
}

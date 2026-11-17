package com.sample.coinchange.service;

import com.sample.coinchange.dto.*;
import com.sample.coinchange.exception.InsufficientCoinsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Workflow for  change request: validate bill, calculate fewest coins, deduct them from inventory. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChangeService {

    private final ChangeCalculator changeCalculator;
    private final CoinInventory coinInventory;

    private final Object lock = new Object();

    public ChangeResponse dispenseChange(int requestBill) {

        // Validate accepted bills.
        BillType billType = BillType.checkValidBill(requestBill);

        Map<CoinType, Integer> responseCoinsMap;
        synchronized (lock) {
            // Fewest coins calculation.
            Map<CoinType, Integer> available = coinInventory.snapshot();
            responseCoinsMap = changeCalculator.calculate(billType.getCents(), available)
                              .orElseThrow(() -> {
                               log.warn("Cannot make change for ${} with inventory {}", requestBill, available);
                               return new InsufficientCoinsException(requestBill);
                              });

            // takes the coins out of inventory.
            coinInventory.deduct(responseCoinsMap);
        }

        ChangeResponse changeResponse = processChangeResponse(requestBill, responseCoinsMap);
        log.info("Change for ${}: {} coins {}", requestBill, changeResponse.getTotalCoins(), responseCoinsMap);
        return changeResponse;
    }

    /**  Parse the internal map into the API response. */
    private ChangeResponse processChangeResponse(int billDollars, Map<CoinType, Integer> coins) {
        List<Map.Entry<CoinType, Integer>> entries = new ArrayList<>(coins.entrySet());
        entries.sort(largestCoinFirst());

        List<CoinCount> changeList = new ArrayList<>();
        int totalCoins = 0;
        for (Map.Entry<CoinType, Integer> entry : entries) {
            changeList.add(CoinCount.of(entry.getKey(), entry.getValue()));
            totalCoins += entry.getValue();
        }
        return new ChangeResponse(billDollars, totalCoins, changeList);
    }

    private static Comparator<Map.Entry<CoinType, Integer>> largestCoinFirst() {
        return Comparator.comparingInt((Map.Entry<CoinType, Integer> e) -> e.getKey().getCents()).reversed();
    }

}

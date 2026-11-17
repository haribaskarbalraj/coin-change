package com.sample.coinchange.service;

import com.sample.coinchange.dto.ChangeResponse;
import com.sample.coinchange.dto.CoinCount;
import com.sample.coinchange.dto.CoinType;
import com.sample.coinchange.exception.InsufficientCoinsException;
import com.sample.coinchange.exception.InvalidBillException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** No Spring: constructor injection lets us build the service with 'new'. */
class ChangeServiceTest {

    private ChangeService service;
    private CoinInventory inventory;

    @BeforeEach
    void setUp() {
        inventory = new CoinInventory(100);
        service = new ChangeService(new ChangeCalculator(), inventory);
    }

    private int countOf(String coin) {
        return inventory.snapshot().get(CoinType.valueOf(coin));
    }

    private int totalCentsOnHand() {
        return (int) ChangeCalculator.totalValueInCents(inventory.snapshot());
    }

    @Test
    void returnsChangeAndDeductsItFromInventory() {
        ChangeResponse response = service.dispenseChange(1);

        assertThat(response.getBill()).isEqualTo(1);
        assertThat(response.getTotalCoins()).isEqualTo(4);
        assertThat(response.getChange()).hasSize(1);
        assertThat(response.getChange().get(0).getCoin()).isEqualTo("QUARTER");
        assertThat(response.getChange().get(0).getCount()).isEqualTo(4);
        assertThat(countOf("QUARTER")).isEqualTo(96);
        assertThat(countOf("DIME")).isEqualTo(100);
    }

    @Test
    void stateIsKeptBetweenRequests() {
        for (int i = 0; i < 25; i++) {
            service.dispenseChange(1);            // 25 x 4 quarters = all 100 quarters
        }
        assertThat(countOf("QUARTER")).isZero();

        ChangeResponse next = service.dispenseChange(1);   // no quarters left -> 10 dimes
        assertThat(next.getChange()).extracting(CoinCount::getCoin).containsExactly("DIME");
        assertThat(next.getTotalCoins()).isEqualTo(10);
        assertThat(countOf("DIME")).isEqualTo(90);
    }

    @Test
    void responseListsLargestCoinFirst() {
        // Only 5 of each coin (205 cents in total): $2 needs every quarter, dime and nickel.
        ChangeService small = new ChangeService(new ChangeCalculator(), new CoinInventory(5));

        ChangeResponse response = small.dispenseChange(2);

        assertThat(response.getChange()).extracting(CoinCount::getCoin).containsExactly("QUARTER", "DIME", "NICKEL");
        assertThat(response.getTotalCoins()).isEqualTo(15);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 7, -5, 1000})
    void rejectsBillsThatAreNotAccepted(int bill) {
        assertThatThrownBy(() -> service.dispenseChange(bill)).isInstanceOf(InvalidBillException.class);
        assertThat(totalCentsOnHand()).isEqualTo(4100);       // nothing was taken
    }

    @Test
    void notEnoughCoinsLeavesInventoryUntouched() {
        // A full machine holds 100 x 41 cents = $41, so $50 can never be made.
        assertThatThrownBy(() -> service.dispenseChange(50)).isInstanceOf(InsufficientCoinsException.class);
        assertThat(totalCentsOnHand()).isEqualTo(4100);
    }

    @Test
    void runsOutAfterEnoughRequests() {
        // $41 in the machine: 41 one-dollar requests succeed, the 42nd fails.
        for (int i = 0; i < 41; i++) {
            service.dispenseChange(1);
        }
        assertThat(totalCentsOnHand()).isZero();
        assertThatThrownBy(() -> service.dispenseChange(1)).isInstanceOf(InsufficientCoinsException.class);
    }

    @Test
    void parallelRequestsNeverHandOutMoreCoinsThanExist() throws Exception {
        // The machine holds $41. Fire 200 simultaneous $1 requests: exactly 41 may succeed.
        // Without a lock, two threads can read the same inventory and both take the same coins.
        int requests = 200;
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch startTogether = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        AtomicInteger unexpected = new AtomicInteger();

        for (int i = 0; i < requests; i++) {
            pool.submit(() -> {
                try {
                    startTogether.await();                 // all threads wait here, then go at the same moment
                    service.dispenseChange(1);
                    succeeded.incrementAndGet();
                } catch (InsufficientCoinsException e) {
                    rejected.incrementAndGet();
                } catch (Throwable t) {
                    unexpected.incrementAndGet();          // e.g. IllegalStateException from over-deducting
                }
            });
        }
        startTogether.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(unexpected.get()).isZero();
        assertThat(succeeded.get()).isEqualTo(41);
        assertThat(rejected.get()).isEqualTo(159);
        assertThat(totalCentsOnHand()).isZero();
    }
}

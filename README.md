# Coin Change exercise

The idea of this exercise is to implement a Spring Boot Rest application able to provide change given an input dollar billType.

\* Feel free to use any Gen AI tool.

## Problem

Create a Spring Boot service that exposes a REST endpoint that allows a user to request change for a given billType. The service should assume there is a finite number of coins, and it should update that number on each request

### Requirements
*	Accepted bills are (1, 2, 5, 10, 20, 50, 100), nothing else
*	Available coins are (0.01, 0.05, 0.10, 0.25), nothing else
*	Start with 100 coins of each type
*	Change should be made by utilizing the least amount of coins
*	API should validate bad input and respond accordingly
*	Service should respond with an appropriate message if it does not have enough coins to make change
*	The service should maintain the state of the coins throughout the transactions

---

# Implementation

Java 21, Spring Boot 4.1 and Gradle. Run with `./gradlew bootRun` (http://localhost:8080) and test with `./gradlew test`. Sample requests are in `http/coin-change.http`.

## API

`POST /api/change` with body `{ "bill": 5 }`

Success (200):
```json
{
  "bill": 5,
  "totalCoins": 20,
  "change": [ { "coin": "QUARTER", "value": 0.25, "count": 20 } ]
}
```

| Case | Status |
|---|---|
| Bill not in (1, 2, 5, 10, 20, 50, 100) | 400 Invalid bill |
| `bill` missing or null | 400 Invalid request |
| Malformed JSON or wrong type (`"5"`, `5.5`, `""`) | 400 |
| Wrong HTTP method / content type | 405 / 415 |
| Not enough coins to make exact change | 409 Insufficient coins |

Errors use the standard Spring `ProblemDetail` format.

## Application flow

```
POST /api/change
   |
CoinChangeController      validates the body (@Valid, bill is required)
   |
ChangeService             1. BillType.checkValidBill -> 400 if the bill is not accepted
   |                      2. lock: take a snapshot of the inventory
   |                      3. ChangeCalculator.calculate(cents, snapshot)
   |                           empty -> InsufficientCoinsException -> 409
   |                      4. CoinInventory.deduct(coins)
   |                      5. unlock, build the response (largest coin first)
   |
GlobalExceptionHandler    turns exceptions into 400 / 409 responses
```

## Why dynamic programming

Greedy (always take the largest coin) is not enough because the coin stock is **finite**. Example: make 30 cents with 1 quarter, 3 dimes, no nickels and no pennies. Greedy takes the quarter, is left with 5 cents it cannot make, and wrongly reports failure. The correct answer is 3 dimes.

The calculator uses a bounded-coin DP table:
* `dp[row][a]` is the fewest coins that make `a` cents using only the first `row` coin types.
* For each coin type it tries `k = 0..min(stock, amount / coinValue)` coins and keeps the best result.
* The last cell, `dp[4][amount]`, is the answer. If it is impossible, the service returns 409.
* A walk back through the table recovers which coins were used.

The table is tiny (at most 4 x 10,001 cells for a $100 bill), so it runs instantly.

## Design decisions
* **Exact money:** amounts are integer cents. `BigDecimal` is only used to show coin values in the response.
* **Concurrency:** calculate and deduct run inside one `synchronized` block, so parallel requests can never spend the same coins. A test fires 200 parallel requests and checks that exactly 41 succeed.
* **Strict JSON:** Jackson is configured to reject `5.5` and `"5"` instead of silently turning them into `5` (see `application.properties`).

## Limitations
* State is in memory, so it resets on restart.
* The lock works for a single instance. Several instances would need shared state (a database transaction or distributed lock).

package com.sample.coinchange.dto;

import com.sample.coinchange.exception.InvalidBillException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BillTest {

    @Test
    void everyAcceptedNumberMapsToItsBill() {
        assertThat(BillType.checkValidBill(1)).isEqualTo(BillType.ONE);
        assertThat(BillType.checkValidBill(2)).isEqualTo(BillType.TWO);
        assertThat(BillType.checkValidBill(5)).isEqualTo(BillType.FIVE);
        assertThat(BillType.checkValidBill(10)).isEqualTo(BillType.TEN);
        assertThat(BillType.checkValidBill(20)).isEqualTo(BillType.TWENTY);
        assertThat(BillType.checkValidBill(50)).isEqualTo(BillType.FIFTY);
        assertThat(BillType.checkValidBill(100)).isEqualTo(BillType.HUNDRED);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 7, -5, 25, 101, 1000})
    void otherNumbersAreRejectedWithTheListOfAcceptedBills(int dollars) {
        assertThatThrownBy(() -> BillType.checkValidBill(dollars))
                .isInstanceOf(InvalidBillException.class)
                .hasMessage("Invalid bill: " + dollars + ". Accepted bills are 1, 2, 5, 10, 20, 50, 100.");
    }

    @Test
    void errorMessageMentionsEveryBillInTheEnum() {
        // Guards against adding a bill to the enum but forgetting the list in InvalidBillException.
        String message = new InvalidBillException(3).getMessage();

        for (BillType billType : BillType.values()) {
            assertThat(message).contains(billType.getAmount() + "");
        }
    }

    @Test
    void billIsConvertedToCents() {
        assertThat(BillType.FIVE.getCents()).isEqualTo(500);
        assertThat(BillType.HUNDRED.getCents()).isEqualTo(10_000);
    }
}

package com.sample.coinchange.dto;

import com.sample.coinchange.exception.InvalidBillException;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum BillType {
    ONE(1),
    TWO(2),
    FIVE(5),
    TEN(10),
    TWENTY(20),
    FIFTY(50),
    HUNDRED(100);

    private final int amount;

    //Converting Bill in cents
    public int getCents() {
        return amount * 100;
    }

    //Converts raw number into a Bill
    public static BillType checkValidBill(int requestBill) {
        for (BillType billType : values()) {
            if (billType.amount == requestBill) {
                return billType;
            }
        }
        throw new InvalidBillException(requestBill);
    }
}

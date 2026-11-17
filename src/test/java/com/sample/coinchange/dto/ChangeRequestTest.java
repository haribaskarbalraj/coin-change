package com.sample.coinchange.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs Bean Validation directly, without Spring or HTTP.
 * Note: @NotNull only checks "present". Whether 3 is an accepted bill is Bill's job, not this test's.
 */
class ChangeRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void nullBillIsRejected() {
        Set<ConstraintViolation<ChangeRequest>> violations = validator.validate(new ChangeRequest(null));

        assertThat(violations).hasSize(1);
        ConstraintViolation<ChangeRequest> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath()).hasToString("bill");
        assertThat(violation.getMessage()).isEqualTo("bill is required");
    }

    @Test
    void anyNumberPassesTheShapeCheck() {
        // 3 is not an accepted bill, but that is a business rule checked later.
        assertThat(validator.validate(new ChangeRequest(5))).isEmpty();
        assertThat(validator.validate(new ChangeRequest(3))).isEmpty();
    }

    @Test
    void coinCountIsBuiltFromCoinType() {
        CoinCount line = CoinCount.of(CoinType.QUARTER, 4);

        assertThat(line.getCoin()).isEqualTo("QUARTER");
        assertThat(line.getValue()).isEqualByComparingTo("0.25");
        assertThat(line.getCount()).isEqualTo(4);
    }
}

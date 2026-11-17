package com.sample.coinchange.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRequest {

    @NotNull(message = "bill is required")
    private Integer bill;
}

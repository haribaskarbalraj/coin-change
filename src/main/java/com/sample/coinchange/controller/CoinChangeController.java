package com.sample.coinchange.controller;

import com.sample.coinchange.dto.ChangeRequest;
import com.sample.coinchange.dto.ChangeResponse;
import com.sample.coinchange.dto.CoinCount;
import com.sample.coinchange.service.ChangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class CoinChangeController {

    private final ChangeService changeService;

    /** POST, Dispenses the Change and update the server state (uses up coins) */
    @PostMapping(value = "/change", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ChangeResponse calculateChange(@Valid @RequestBody ChangeRequest request) {
        log.info("POST /api/change bill={}", request.getBill());
        return changeService.dispenseChange(request.getBill());
    }

}

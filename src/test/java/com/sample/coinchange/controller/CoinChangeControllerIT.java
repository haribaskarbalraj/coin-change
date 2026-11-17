package com.sample.coinchange.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the whole HTTP contract through the real Spring context: routing, JSON parsing,
 * validation, status codes and error format. MockMvc calls the controller without a real server.
 * <p>
 * The coin inventory is a singleton, so a test that takes coins would affect later tests.
 * Tests that change it use @DirtiesContext, which throws the context away afterwards so the next
 * test starts with a full inventory. Tests that are rejected before touching coins don't need it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CoinChangeControllerIT {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions postChange(String json) throws Exception {
        return mockMvc.perform(post("/api/change").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    // ---------- success ----------

    @Test
    @DirtiesContext
    void validBillReturnsChange() throws Exception {
        postChange("{\"bill\":5}")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bill").value(5))
                .andExpect(jsonPath("$.totalCoins").value(20))
                .andExpect(jsonPath("$.change.length()").value(1))
                .andExpect(jsonPath("$.change[0].coin").value("QUARTER"))
                .andExpect(jsonPath("$.change[0].value").value(0.25))
                .andExpect(jsonPath("$.change[0].count").value(20));
    }

    @Test
    @DirtiesContext
    void usesSmallerCoinsOnceQuartersRunOut() throws Exception {
        // First $20 takes 80 quarters, leaving 20. The second $20 needs more than that, so it must add dimes and nickels.
        postChange("{\"bill\":20}").andExpect(status().isOk());

        postChange("{\"bill\":20}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.change[0].coin").value("QUARTER"))
                .andExpect(jsonPath("$.change[0].count").value(20))
                .andExpect(jsonPath("$.change[1].coin").value("DIME"));
    }

    // ---------- 400: business rule ----------

    @ParameterizedTest
    @ValueSource(strings = {"3", "0", "-5", "1000"})
    void billThatIsNotAcceptedReturns400(String bill) throws Exception {
        postChange("{\"bill\":" + bill + "}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid bill"))
                .andExpect(jsonPath("$.status").value(400));
    }

    // ---------- 400: request shape (Bean Validation) ----------

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"bill\":null}"})
    void missingOrNullBillReturns400(String json) throws Exception {
        postChange(json)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value("bill: bill is required"));
    }

    // ---------- 400: JSON / type problems (Jackson) ----------

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"bill\":\"\"}",        // empty string: must not be treated as null
            "{\"bill\":\"abc\"}",     // text
            "{\"bill\":\"5$\"}",      // special characters
            "{\"bill\":5.5}",         // decimal: must not be cut to 5
            "{\"bill\":\"5\"}",       // number as a string: must not be converted
            "{\"bill\":true}",        // wrong type
            "{\"bill\":[5]}",         // array
            "{\"bill\":99999999999}", // too big for an int
            "{\"bill\":",             // broken JSON
            "not json"
    })
    void malformedOrWrongTypeBodyReturns400(String json) throws Exception {
        postChange(json)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void missingBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/change").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ---------- other HTTP errors ----------

    @Test
    void wrongContentTypeReturns415() throws Exception {
        mockMvc.perform(post("/api/change").contentType(MediaType.TEXT_PLAIN).content("5"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void getOnChangeEndpointReturns405() throws Exception {
        mockMvc.perform(get("/api/change"))
                .andExpect(status().isMethodNotAllowed());
    }

    // ---------- 409: not enough coins ----------

    @Test
    void notEnoughCoinsReturns409() throws Exception {
        // A full machine holds $41, so $100 can never be made. Nothing is taken, so no @DirtiesContext.
        postChange("{\"bill\":100}")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Insufficient coins"))
                .andExpect(jsonPath("$.status").value(409));
    }
}

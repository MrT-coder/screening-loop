package io.github.screeningloop.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.screeningloop.dashboard.DashboardService;
import io.github.screeningloop.dashboard.DashboardSummary;
import io.github.screeningloop.security.SecurityConfiguration;
import io.github.screeningloop.security.TestUsers;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfiguration.class)
class DashboardControllerTest {

    private static final String ACTOR = TestUsers.PRACTITIONER;
    private static final int TOTAL = 3;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DashboardService service;

    @Test
    void returnsTheSummaryForTheActor() throws Exception {
        DashboardSummary summary = new DashboardSummary(
                TOTAL, 2, 1, 0, Map.of("result-issued", 2), List.of(), List.of(), List.of(), null, Map.of(), null);
        when(service.summarize(ACTOR)).thenReturn(summary);
        mvc.perform(get("/api/dashboard").with(user(TestUsers.guest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(TOTAL))
                .andExpect(jsonPath("$.byStage.result-issued").value(2))
                .andExpect(jsonPath("$.overdue").isEmpty());
    }

    @Test
    void requiresASession() throws Exception {
        mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
    }
}

package io.github.screeningloop.api;

import static io.github.screeningloop.security.TestUsers.coordinator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.screeningloop.identity.IdentityCheckFailedException;
import io.github.screeningloop.identity.IdentityClaim;
import io.github.screeningloop.identity.IdentityVerifier;
import io.github.screeningloop.identity.LivenessChallenge;
import io.github.screeningloop.identity.TooManyAttemptsException;
import io.github.screeningloop.security.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChatSessionController.class)
@Import(SecurityConfiguration.class)
class ChatSessionControllerTest {

    private static final int PATIENT_SESSION_SECONDS = 600;
    private static final String CLAIM = """
            {"documentNumber":"999-38-6612","birthDate":"1976-10-05","livenessToken":"token-1"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private IdentityVerifier verifier;

    @MockitoBean
    private LivenessChallenge liveness;

    private MockHttpSession verifiedSession() throws Exception {
        when(verifier.verify(any(IdentityClaim.class), any(String.class))).thenReturn("3328");
        return (MockHttpSession) mvc.perform(post("/api/chat/verify").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CLAIM))
                .andExpect(status().isNoContent())
                .andReturn().getRequest().getSession();
    }

    @Test
    void issuesALivenessTokenWithoutASession() throws Exception {
        when(liveness.issue()).thenReturn("token-1");
        mvc.perform(post("/api/chat/liveness").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-1"));
    }

    @Test
    void opensAShortPatientSessionWhenTheIdentityIsVerified() throws Exception {
        MockHttpSession session = verifiedSession();
        assertThat(session.getMaxInactiveInterval()).isEqualTo(PATIENT_SESSION_SECONDS);
        mvc.perform(get("/api/chat/session").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void answersUnauthorizedWithAGenericMessageWhenTheIdentityFails() throws Exception {
        when(verifier.verify(any(IdentityClaim.class), any(String.class)))
                .thenThrow(new IdentityCheckFailedException());
        mvc.perform(post("/api/chat/verify").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CLAIM))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Identity could not be verified"));
    }

    @Test
    void answersTooManyRequestsWhenTheAttemptsAreExhausted() throws Exception {
        when(verifier.verify(any(IdentityClaim.class), any(String.class)))
                .thenThrow(new TooManyAttemptsException());
        mvc.perform(post("/api/chat/verify").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(CLAIM))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void rejectsAMalformedClaim() throws Exception {
        String body = CLAIM.replace("999-38-6612", "");
        mvc.perform(post("/api/chat/verify").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refusesToVerifyWithoutACsrfToken() throws Exception {
        mvc.perform(post("/api/chat/verify").contentType(MediaType.APPLICATION_JSON).content(CLAIM))
                .andExpect(status().isForbidden());
    }

    @Test
    void keepsAPatientSessionAwayFromTheStaffEndpoints() throws Exception {
        MockHttpSession session = verifiedSession();
        mvc.perform(get("/api/dashboard").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/cases").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/cases/t1").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/patients/3328/summary").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/me").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void keepsStaffAwayFromThePatientChat() throws Exception {
        mvc.perform(get("/api/chat/session").with(user(coordinator()))).andExpect(status().isForbidden());
    }

    @Test
    void requiresAPatientSessionForTheChat() throws Exception {
        mvc.perform(get("/api/chat/session")).andExpect(status().isUnauthorized());
    }
}

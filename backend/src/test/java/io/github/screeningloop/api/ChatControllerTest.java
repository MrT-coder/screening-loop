package io.github.screeningloop.api;

import static io.github.screeningloop.security.TestUsers.coordinator;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.screeningloop.chat.ChatReply;
import io.github.screeningloop.chat.ChatService;
import io.github.screeningloop.identity.IdentityVerifier;
import io.github.screeningloop.identity.LivenessChallenge;
import io.github.screeningloop.security.PatientPrincipal;
import io.github.screeningloop.security.SecurityConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({ChatController.class, ChatSessionController.class})
@Import(SecurityConfiguration.class)
class ChatControllerTest {

    private static final int TOO_LONG = 301;
    private static final String MESSAGE = "{\"text\":\"which medications do I take?\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChatService service;

    @MockitoBean
    private IdentityVerifier verifier;

    @MockitoBean
    private LivenessChallenge liveness;

    private UsernamePasswordAuthenticationToken patient() {
        return UsernamePasswordAuthenticationToken.authenticated(
                new PatientPrincipal("3328"), null, List.of(new SimpleGrantedAuthority("ROLE_PATIENT")));
    }

    @Test
    void answersAPatientMessageUsingThePatientOfTheSession() throws Exception {
        when(service.answer("3328", "which medications do I take?"))
                .thenReturn(new ChatReply("Your active medications on record are: Aspirin.", "MEDICATIONS"));
        mvc.perform(post("/api/chat/messages").with(authentication(patient())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(MESSAGE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Your active medications on record are: Aspirin."))
                .andExpect(jsonPath("$.intent").value("MEDICATIONS"));
    }

    @Test
    void ignoresAnyPatientIdSentInTheBody() throws Exception {
        when(service.answer("3328", "hello")).thenReturn(new ChatReply("Hello.", "GREETING"));
        mvc.perform(post("/api/chat/messages").with(authentication(patient())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"hello\",\"patientId\":\"9999\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent").value("GREETING"));
    }

    @Test
    void rejectsBlankOrTooLongMessages() throws Exception {
        mvc.perform(post("/api/chat/messages").with(authentication(patient())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"  \"}"))
                .andExpect(status().isBadRequest());
        String longText = "{\"text\":\"" + "a".repeat(TOO_LONG) + "\"}";
        mvc.perform(post("/api/chat/messages").with(authentication(patient())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(longText))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresAPatientSessionAndACsrfToken() throws Exception {
        mvc.perform(post("/api/chat/messages").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(MESSAGE))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/chat/messages").with(user(coordinator())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(MESSAGE))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/chat/messages").with(authentication(patient()))
                        .contentType(MediaType.APPLICATION_JSON).content(MESSAGE))
                .andExpect(status().isForbidden());
    }
}

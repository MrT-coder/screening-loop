package io.github.screeningloop.api;

import static io.github.screeningloop.security.TestUsers.PRACTITIONER;
import static io.github.screeningloop.security.TestUsers.guest;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.screeningloop.chat.ChatReply;
import io.github.screeningloop.chat.ClinicianChatService;
import io.github.screeningloop.security.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClinicianChatController.class)
@Import(SecurityConfiguration.class)
class ClinicianChatControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ClinicianChatService service;

    @Test
    void answersAStaffQuestionAboutThePatientInThePath() throws Exception {
        when(service.answer("p1", PRACTITIONER, "medications")).thenReturn(new ChatReply("Aspirin", "MEDICATIONS"));
        mvc.perform(post("/api/patients/p1/chat").with(user(guest())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"medications\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Aspirin"))
                .andExpect(jsonPath("$.intent").value("MEDICATIONS"));
    }

    @Test
    void rejectsAnEmptyQuestion() throws Exception {
        mvc.perform(post("/api/patients/p1/chat").with(user(guest())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresASession() throws Exception {
        mvc.perform(post("/api/patients/p1/chat").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
    }
}

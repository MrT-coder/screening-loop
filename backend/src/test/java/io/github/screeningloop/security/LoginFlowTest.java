package io.github.screeningloop.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.screeningloop.api.MeController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MeController.class)
@Import(SecurityConfiguration.class)
class LoginFlowTest {

    @Autowired
    private MockMvc mvc;

    private MockHttpSession login(String username, String password) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/login").with(csrf())
                        .param("username", username).param("password", password))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession();
    }

    @Test
    void logsInWithAConfiguredUserAndExposesTheirRole() throws Exception {
        MockHttpSession session = login("guest", "guest-demo");
        mvc.perform(get("/api/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("guest"))
                .andExpect(jsonPath("$.role").value("GUEST"))
                .andExpect(jsonPath("$.practitioner").value("Practitioner/demo-guest"));
    }

    @Test
    void keepsTheCredentialsOfAUserValidAcrossRepeatedLogins() throws Exception {
        login("coordinator", "coordinator-demo");
        login("coordinator", "coordinator-demo");
    }

    @Test
    void answersUnauthorizedForAWrongPasswordOrUnknownUser() throws Exception {
        mvc.perform(post("/api/login").with(csrf()).param("username", "guest").param("password", "wrong"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/login").with(csrf()).param("username", "nobody").param("password", "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refusesToLogInWithoutACsrfToken() throws Exception {
        mvc.perform(post("/api/login").param("username", "guest").param("password", "guest-demo"))
                .andExpect(status().isForbidden());
    }

    @Test
    void endsTheSessionOnLogout() throws Exception {
        MockHttpSession session = login("gp1", "gp1-demo");
        mvc.perform(post("/api/logout").with(csrf()).session(session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void answersUnauthorizedForMeWithoutASession() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }
}

package io.github.screeningloop.api;

import io.github.screeningloop.identity.IdentityClaim;
import io.github.screeningloop.identity.IdentityVerifier;
import io.github.screeningloop.identity.LivenessChallenge;
import io.github.screeningloop.security.PatientPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatSessionController {

    private static final int PATIENT_SESSION_SECONDS = 600;
    private static final List<SimpleGrantedAuthority> PATIENT_AUTHORITIES =
            List.of(new SimpleGrantedAuthority("ROLE_PATIENT"));

    private final IdentityVerifier verifier;
    private final LivenessChallenge liveness;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public ChatSessionController(IdentityVerifier verifier, LivenessChallenge liveness) {
        this.verifier = verifier;
        this.liveness = liveness;
    }

    @PostMapping("/liveness")
    public Map<String, String> issueLivenessToken() {
        return Map.of("token", liveness.issue());
    }

    @PostMapping("/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verify(
            @Valid @RequestBody IdentityRequest body, HttpServletRequest request, HttpServletResponse response) {
        String patientId = verifier.verify(
                new IdentityClaim(body.documentNumber(), body.birthDate(), body.livenessToken()),
                request.getRemoteAddr());
        startSession(new PatientPrincipal(patientId), request, response);
    }

    @GetMapping("/session")
    public Map<String, Boolean> session() {
        return Map.of("active", true);
    }

    private void startSession(PatientPrincipal principal, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, PATIENT_AUTHORITIES);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
        request.getSession().setMaxInactiveInterval(PATIENT_SESSION_SECONDS);
    }
}

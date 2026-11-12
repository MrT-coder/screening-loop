package io.github.screeningloop.api;

import io.github.screeningloop.chat.ChatReply;
import io.github.screeningloop.chat.ChatService;
import io.github.screeningloop.security.PatientPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/messages")
public class ChatController {

    private final ChatService service;

    public ChatController(ChatService service) {
        this.service = service;
    }

    @PostMapping
    public ChatReply answer(
            @AuthenticationPrincipal PatientPrincipal principal, @Valid @RequestBody ChatMessageRequest body) {
        return service.answer(principal.patientId(), body.text());
    }
}

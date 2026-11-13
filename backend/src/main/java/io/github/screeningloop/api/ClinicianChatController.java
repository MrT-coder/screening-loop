package io.github.screeningloop.api;

import io.github.screeningloop.chat.ChatReply;
import io.github.screeningloop.chat.ClinicianChatService;
import io.github.screeningloop.chat.Language;
import io.github.screeningloop.security.AppUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients")
public class ClinicianChatController {

    private final ClinicianChatService service;

    public ClinicianChatController(ClinicianChatService service) {
        this.service = service;
    }

    @PostMapping("/{id:[A-Za-z0-9.-]{1,64}}/chat")
    public ChatReply answer(
            @PathVariable String id,
            @AuthenticationPrincipal AppUser user,
            @Valid @RequestBody ChatMessageRequest body) {
        return service.answer(id, user.practitioner(), body.text(), Language.fromCode(body.language()));
    }
}

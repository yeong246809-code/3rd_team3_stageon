package kr.co.stageon.ai.web;

import kr.co.stageon.ai.dto.AiChatHistoryItem;
import kr.co.stageon.ai.dto.AiChatRoomItem;
import kr.co.stageon.ai.service.AiChatHistoryService;
import kr.co.stageon.ai.service.AiMemberContextService;
import kr.co.stageon.member.domain.Member;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/history")
public class AiChatHistoryApiController {
    private final AiMemberContextService memberContextService;
    private final AiChatHistoryService historyService;

    public AiChatHistoryApiController(
            AiMemberContextService memberContextService,
            AiChatHistoryService historyService
    ) {
        this.memberContextService = memberContextService;
        this.historyService = historyService;
    }

    @GetMapping
    public Map<String, Object> history(Authentication authentication, CsrfToken csrfToken) {
        Member member = memberContextService.currentMember(authentication).orElse(null);
        List<AiChatRoomItem> rooms = historyService.rooms(member);
        List<AiChatHistoryItem> messages = historyService.recent(member);
        return Map.of(
                "authenticated", member != null,
                "displayName", member == null ? "" : member.getName(),
                "roomLimit", AiChatHistoryService.ROOM_LIMIT,
                "rooms", rooms,
                "messages", messages,
                "csrfHeader", csrfToken.getHeaderName(),
                "csrfToken", csrfToken.getToken()
        );
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(Authentication authentication) {
        Member member = memberContextService.currentMember(authentication).orElse(null);
        if (member == null) return ResponseEntity.status(401).build();
        historyService.clear(member);
        return ResponseEntity.noContent().build();
    }
}

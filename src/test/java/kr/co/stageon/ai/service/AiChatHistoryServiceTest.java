package kr.co.stageon.ai.service;

import kr.co.stageon.ai.domain.AiChatHistory;
import kr.co.stageon.ai.dto.AiChatRoomItem;
import kr.co.stageon.ai.repository.AiChatHistoryRepository;
import kr.co.stageon.member.domain.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatHistoryServiceTest {
    @Mock
    private AiChatHistoryRepository repository;
    @Mock
    private Member member;

    private AiChatHistoryService service;

    @BeforeEach
    void setUp() {
        service = new AiChatHistoryService(repository, new ObjectMapper());
        when(member.getId()).thenReturn(7L);
    }

    @Test
    void savesConversationMetadataWithoutChangingTheExistingTableSchema() {
        when(repository.findByMemberIdOrderByCreatedAtDesc(7L)).thenReturn(List.of());

        service.remember(member, "room-123", "이번 주말 뮤지컬 추천", "추천 답변");

        ArgumentCaptor<AiChatHistory> captor = ArgumentCaptor.forClass(AiChatHistory.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getExtractedCondition()).contains("room-123", "이번 주말 뮤지컬 추천");
    }

    @Test
    void groupsSavedMessagesByConversation() {
        AiChatHistory recent = AiChatHistory.success(
                member, "두 번째 질문", "두 번째 답변",
                "{\"conversationId\":\"room-b\",\"title\":\"주말 공연\"}");
        AiChatHistory older = AiChatHistory.success(
                member, "첫 질문", "첫 답변",
                "{\"conversationId\":\"room-a\",\"title\":\"내 취향 추천\"}");
        when(repository.findByMemberIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(recent, older));

        List<AiChatRoomItem> rooms = service.rooms(member);

        assertThat(rooms).extracting(AiChatRoomItem::conversationId).containsExactly("room-b", "room-a");
        assertThat(rooms.getFirst().messages()).hasSize(1);
        assertThat(rooms.getFirst().messages().getFirst().question()).isEqualTo("두 번째 질문");
    }
}

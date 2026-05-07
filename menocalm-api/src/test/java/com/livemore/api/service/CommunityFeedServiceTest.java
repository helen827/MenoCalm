package com.livemore.api.service;

import com.livemore.api.web.dto.CommunityFeedFileDto;
import com.livemore.api.web.dto.CommunityFeedItemDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityFeedServiceTest {

    @Mock
    private CommunityFeedStore communityFeedStore;

    @Test
    void getFeed_withoutSnapshot_returnsServiceUnavailable() {
        when(communityFeedStore.loadCurrent()).thenReturn(Optional.empty());
        CommunityFeedService service = new CommunityFeedService(communityFeedStore);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, service::getFeed);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    void getFeed_withSnapshot_mapsPayload() {
        CommunityFeedFileDto feed = new CommunityFeedFileDto();
        CommunityFeedItemDto item = new CommunityFeedItemDto();
        item.setId("p-1");
        item.setTitle("title");
        feed.setOfficial(List.of(item));
        feed.setUser(List.of());
        when(communityFeedStore.loadCurrent()).thenReturn(Optional.of(feed));
        CommunityFeedService service = new CommunityFeedService(communityFeedStore);

        var dto = service.getFeed();
        assertEquals(1, dto.getOfficial().size());
        assertEquals("p-1", dto.getOfficial().get(0).getId());
    }
}

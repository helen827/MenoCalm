package com.livemore.api.web;

import com.livemore.api.service.CommunityFeedService;
import com.livemore.api.web.dto.CommunityFeedFileDto;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/community", produces = MediaType.APPLICATION_JSON_VALUE)
public class CommunityFeedController {

    private final CommunityFeedService communityFeedService;

    public CommunityFeedController(CommunityFeedService communityFeedService) {
        this.communityFeedService = communityFeedService;
    }

    @GetMapping("/feed")
    public CommunityFeedFileDto feed() {
        return communityFeedService.getFeed();
    }
}

package com.syncfusion.intigration.controllers.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.syncfusion.dto.brahmos.request.inBound.NewForumNestedReplyRequest;
import com.syncfusion.dto.brahmos.request.inBound.NewForumPostRequest;
import com.syncfusion.dto.brahmos.request.inBound.NewForumReplyRequest;
import com.syncfusion.handlers.forumThreadHandlers.ForumThreadHandler;
import com.syncfusion.intigration.AbstractIntegrationTest;
import org.instancio.Instancio;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class ForumWebHookControllerTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ForumThreadHandler forumThreadHandler;
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void newForumPostAPIWorking() throws Exception {
        logger.info("Running test for new forum creation");
        NewForumPostRequest newForumPostRequest = Instancio.create(NewForumPostRequest.class);
        newForumPostRequest.setCreatedOn(null);
        mockMvc.perform(post("/webhook/forum/new/post")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json().build().writeValueAsString(newForumPostRequest)))
                .andExpect(status().isOk());
        verify(forumThreadHandler,times(1)).handleNewPostCreation(newForumPostRequest);
    }
    @Test
    public void newReplyCreationAPIWorking() throws Exception {
        logger.info("Running test for new reply creation...");
        NewForumReplyRequest newPostReplyReq = Instancio.create(NewForumReplyRequest.class);
        newPostReplyReq.setCreatedOn(null);
        mockMvc.perform(post("/webhook/forum/new/reply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json().build().writeValueAsString(newPostReplyReq)))
                .andExpect(status().isOk());
        verify(forumThreadHandler,times(1)).handleNewReplyCreation(newPostReplyReq);
    }

    @Test
    public void newNestedReplyCreationAPIWorking() throws Exception {
        logger.info("Running test for new nested reply creation...");
        NewForumNestedReplyRequest newForumNestedReplyRequest = Instancio.create(NewForumNestedReplyRequest.class);
        newForumNestedReplyRequest.setCreatedOn(null);
        mockMvc.perform(post("/webhook/forum/new/nestedReply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json().build().writeValueAsString(newForumNestedReplyRequest)))
                .andExpect(status().isOk());
        verify(forumThreadHandler,times(1)).handleNestedReplyCreation(newForumNestedReplyRequest);
    }
}

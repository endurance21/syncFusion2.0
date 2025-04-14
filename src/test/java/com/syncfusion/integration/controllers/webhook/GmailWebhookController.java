package com.syncfusion.intigration.controllers.webhook;

import com.syncfusion.dto.gmail.GmailDTO;
import com.syncfusion.intigration.AbstractIntegrationTest;
import com.syncfusion.processors.email.GmailInboxUpdateProcessorForum;
import org.instancio.Instancio;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class GmailWebhookController  extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GmailInboxUpdateProcessorForum gmailInboxUpdateProcessor;
    Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());

    @Test
    public void canHandleInboxUpdate() throws Exception {
        logger.info("Running test for new inbox update");
        GmailDTO.GmailPayload payload = Instancio.create( GmailDTO.GmailPayload.class);
        mockMvc.perform(post("/webhook/gmail/inbox/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json().build().writeValueAsString(payload)))
                .andExpect(status().isOk());
        verify(gmailInboxUpdateProcessor,times(1)).processHistoryId(payload.getHistoryId(), payload.getEmailAddress());
    }

}

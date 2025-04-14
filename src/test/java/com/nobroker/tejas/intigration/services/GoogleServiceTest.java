package com.syncfusion.intigration.services;

import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class GoogleServiceTest {
    Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());
    @Test
    public void canHandleNewForumCreation() {
        logger.info("Running test for new forum creation");
    }
}

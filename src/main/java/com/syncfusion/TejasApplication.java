package com.syncfusion;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestTemplate;

import java.io.FileOutputStream;
import java.io.IOException;

@SpringBootApplication(scanBasePackages = {"com.syncfusion"})
@EnableScheduling
public class TejasApplication {
   private static Logger log = org.slf4j.LoggerFactory.getLogger(TejasApplication.class);

    public static void main(String[] args) throws IOException {
        initialize();
        SpringApplication.run(TejasApplication.class, args);
        log.info("TEJAS started....");
    }

    private static void initialize() throws IOException {
        if (StringUtils.isBlank(System.getProperty("spring.profiles.active"))) {
            System.setProperty("spring.profiles.active", "dev");
        }
        //copy file from classpath to normal filepath
        FileCopyUtils.copy(new ClassPathResource("truststore.jks").getInputStream(),
                new FileOutputStream("/tmp/truststore.jks"));
    }
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

}

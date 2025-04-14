package com.syncfusion.intigration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;


public class AbstractIntegrationTest {
    public static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:3.0.2");
    public static GenericContainer<?> redisContainer = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379);
    private static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:5.7")
            .withDatabaseName("nobroker")
            .withUsername("root")
            .withPassword("admin");
   private static  KafkaContainer kafka ;


    static {
        redisContainer.start();
        mysql.start();
        kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1"));
        kafka.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        String jdbcUrl = String.format("%s?autoReconnect=true&useSSL=false&createDatabaseIfNotExist=true",mysql.getJdbcUrl());
        registry.add("spring.datasource.url", ()->jdbcUrl);
        registry.add("spring.datasource.username", () -> mysql.getUsername());
        registry.add("spring.datasource.password", () -> mysql.getPassword());

        registry.add("redis.isClusterEnabled", () -> false);
        String nodes  = String.format("%s:%s",redisContainer.getContainerIpAddress(), redisContainer.getFirstMappedPort());
        registry.add("redis.nodes", () -> nodes);

        registry.add("kafka.servers", () -> kafka.getBootstrapServers());
        registry.add("kafka.consumer.group-id", () -> "group_id");
    }



}

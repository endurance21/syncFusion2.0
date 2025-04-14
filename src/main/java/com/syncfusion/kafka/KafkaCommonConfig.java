package com.syncfusion.kafka;

import com.syncfusion.utils.UtilMethods;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaCommonConfig {

    @Value(value = "${kafka.servers}")
    private String bootstrapAddress;
    @Autowired
    private Environment environment;
    @Value(value = "${kafka.consumer.group-id}")
    private String groupId;

    @Autowired
    private UtilMethods utilMethods;


    private void addSecurityConfigs(Map<String, Object> props) {
        if(utilMethods.isTestProfile() || utilMethods.isProd())
            return;
        copyTrustStoreFromClassPathToFilePath();
        String location = environment.getProperty("kafka.ssl.truststore.location");
        props.put("security.protocol", environment.getProperty("kafka.security.protocol"));
        props.put("ssl.truststore.password", environment.getProperty("kafka.ssl.truststore.password"));
        props.put("ssl.truststore.location", location);
    }

    public void copyTrustStoreFromClassPathToFilePath() {
        // copy file using Spring FileCopyUtils
        try {
            FileCopyUtils.copy(new ClassPathResource("truststore.jks").getInputStream(),
                    new FileOutputStream("/tmp/truststore.jks"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> getProducerConfig() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapAddress);
        configProps.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );
        configProps.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);
        configProps.put("client.id", "tejas");
        addSecurityConfigs(configProps);
        return configProps;
    }

    public Map<String, Object> getConsumerConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapAddress);
        props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                groupId);
        props.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put("bootstrap.servers", environment.getProperty("kafka.servers"));
        addSecurityConfigs(props);
        return props;
    }


}

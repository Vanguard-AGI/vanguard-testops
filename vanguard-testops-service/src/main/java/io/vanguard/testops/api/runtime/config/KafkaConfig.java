package io.vanguard.testops.api.runtime.config;

import io.vanguard.testops.sdk.util.CommonBeanFactory;
import org.apache.commons.collections.MapUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    private static Map<String, Object> defaultMap;

    @Autowired
    public KafkaConfig(KafkaProperties kafkaProperties) {
        ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties(null));
        defaultMap = producerFactory.getConfigurationProperties();
    }

    public static void addDefaultConfig(Map<String, Object> producerProps) {
        if (MapUtils.isNotEmpty(defaultMap)) {
            defaultMap.forEach(producerProps::putIfAbsent);
        }
    }

    public static Map<String, Object> getKafkaConfig() {
        KafkaProperties kafkaProperties = CommonBeanFactory.getBean(KafkaProperties.class);
        Map<String, Object> producerProps = new HashMap<>();
        assert kafkaProperties != null;
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        addDefaultConfig(producerProps);
        return producerProps;
    }

    public static Map<String, Object> consumerConfig() {
        KafkaProperties kafkaProperties = CommonBeanFactory.getBean(KafkaProperties.class);
        Map<String, Object> producerProps = new HashMap<>();
        assert kafkaProperties != null;
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        addDefaultConfig(producerProps);
        producerProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        producerProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 900000);
        producerProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 5000);
        producerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        producerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        producerProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 5000);
        producerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        producerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return producerProps;
    }
}

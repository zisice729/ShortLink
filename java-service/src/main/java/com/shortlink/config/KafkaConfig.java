
package com.shortlink.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka消息队列配置类
 *
 * 功能说明：
 * - 配置Kafka生产者工厂，用于异步发送访问统计消息
 * - 使用String序列化器，支持JSON格式的消息传输
 * - 消息用途：异步统计短链接的PV/UV访问数据，解耦跳转与数据统计
 *
 * 配置项说明：
 * - bootstrapServers：Kafka集群地址，从配置中心读取
 * - KEY_SERIALIZER_CLASS_CONFIG：消息key的序列化方式
 * - VALUE_SERIALIZER_CLASS_CONFIG：消息value的序列化方式
 */
@Slf4j
@Configuration
public class KafkaConfig {

    /** Kafka集群地址，例如：192.168.1.100:9092 */
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * 创建Kafka生产者工厂
     *
     * @return ProducerFactory 配置好的生产者工厂实例
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        // 设置Kafka集群地址
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // 设置消息key的序列化器
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // 设置消息value的序列化器
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * 创建Kafka消息模板
     * 用于发送短链接访问统计消息
     *
     * @return KafkaTemplate 配置好的Kafka模板实例
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}

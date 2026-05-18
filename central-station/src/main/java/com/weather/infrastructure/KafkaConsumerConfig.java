package com.weather.infrastructure;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

public class KafkaConsumerConfig {

    public static Properties setupConsumer() throws UnknownHostException {
        Properties props = new Properties();


        String brokerUrl = System.getenv("KAFKA_BROKER") != null ?
                System.getenv("KAFKA_BROKER") : "localhost:9092";

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerUrl);


        props.put(ConsumerConfig.CLIENT_ID_CONFIG, InetAddress.getLocalHost().getHostName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "weather-monitoring-group");


        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());


        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        return props;
    }
}
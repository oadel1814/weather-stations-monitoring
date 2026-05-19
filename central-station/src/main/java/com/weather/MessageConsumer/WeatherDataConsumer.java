package com.weather.MessageConsumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.weather.model.WeatherMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class WeatherDataConsumer  implements  Runnable{

    private static final String TOPIC = "weather-readings";
    private static final Logger log = LoggerFactory.getLogger(WeatherDataConsumer.class);
    private final KafkaConsumer<String, String> consumer;
    private final ObjectMapper objectMapper;  // will bes used in the deserialization from json to object message
    private final AtomicBoolean running = new AtomicBoolean(true);

    private final RecordProcessor recordProcessor;

    public WeatherDataConsumer(Properties config, RecordProcessor recordProcessor){
        this.recordProcessor =recordProcessor;
        this.consumer= new KafkaConsumer<>(config);
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    }


    @Override
    public void run() {
        try {
            consumer.subscribe(Collections.singletonList(TOPIC));
            log.info("subscribed successfully to topic {}",TOPIC);
            while (running.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100)); //get in one rtt one patch up to 100 messages
                if (!records.isEmpty()) {
                    for (ConsumerRecord<String,String> record : records){
                        String jsonValue = record.value();
                        WeatherMessage weather = objectMapper.readValue(jsonValue, WeatherMessage.class);
                        recordProcessor.process(weather);
                    }
                    // it is non-blocking commit commits and go up to take the next patch
                    consumer.commitAsync();
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error", e);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                consumer.close();
            }
        }
    }

    public void shutdown() {
        running.set(false);
    }
}

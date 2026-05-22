package com.weather.publishers;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.weather.model.WeatherMessage;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class InvalidMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(InvalidMessagePublisher.class);
    private static final String INVALID_TOPIC = "weather-readings-invalid";


    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;

    public InvalidMessagePublisher(Properties producerProps) {
        this.producer = new KafkaProducer<>(producerProps);

        // We need the ObjectMapper to turn the object back into a JSON string
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public  void publish(WeatherMessage message, String reason) {
        try {
            //  Convert the Java object back to a JSON string
            String jsonPayload = objectMapper.writeValueAsString(message);

            // Create the record using the Station ID as the routing key
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    INVALID_TOPIC,
                    String.valueOf(message.station_id),
                    jsonPayload
            );

            //  Attach the business rule => to know the reason of failure (sensor baz)
            record.headers().add("invalid_reason", reason.getBytes());

            // 4. Send asynchronously => to not block the main thread
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("CRITICAL: Failed to write to Invalid Message Channel for station {}!",
                            message.station_id, exception);
                } else {
                    log.info("Successfully routed invalid data to '{}'. Station: {}, Reason: {}",
                            INVALID_TOPIC, message.station_id, reason);
                }
            });

        } catch (Exception e) {
            log.error("Failed to serialize or send invalid message for station {}", message.station_id, e);
        }
    }

    public void close() {
        if (producer != null) {
            producer.close();
            log.info("InvalidMessagePublisher closed.");
        }
    }
}

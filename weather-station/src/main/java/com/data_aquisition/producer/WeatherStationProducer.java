
package com.data_aquisition.producer;

import com.data_aquisition.infrastructure.KafkaProducerConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.data_aquisition.model.Weather;
import com.data_aquisition.model.WeatherMessage;
import org.apache.kafka.clients.producer.*;

import java.time.Instant;
import java.util.Random;

public class WeatherStationProducer {

    private static final String TOPIC = "weather-readings";

    public static void main(String[] args) throws Exception {

        String envId = System.getenv("STATION_ID");
        long stationId = 1;

        if (envId != null && !envId.trim().isEmpty()) {
            try {
                if (envId.contains("-")) {
                    String[] parts = envId.split("-");
                    stationId = Long.parseLong(parts[parts.length - 1]) + 1;
                } else {
                    stationId = Long.parseLong(envId);
                }
            } catch (NumberFormatException e) {
                System.err.println("Could not parse STATION_ID: " + envId);
            }
        }

        KafkaProducerConfig config = new KafkaProducerConfig();
        KafkaProducer<String, String> producer = new KafkaProducer<>(config.build());


        ObjectMapper mapper = new ObjectMapper();
        Random random = new Random();

        long sequence = 1;
        WeatherMessage lastMessage = null;

        while (true) {
            int scenario = random.nextInt(100);
            WeatherMessage message;
            boolean isNewMessage = true;

            if (scenario < 10) {
                sequence++;
                Thread.sleep(1000);
                continue;
            }

            if (scenario >= 10 && scenario < 20) {
                message = new WeatherMessage(
                        stationId,
                        sequence,
                        generateBattery(random),
                        Instant.now().getEpochSecond(),
                        new Weather(150, 8000, 500),
                        "SYSTEM_PRODUCER"
                );
                lastMessage = message; // Cache it
            }
            else if (scenario >= 20 && scenario < 30 && lastMessage != null) {
                message = lastMessage;
                isNewMessage = false;
            }
            else {
                message = new WeatherMessage(
                        stationId,
                        sequence,
                        generateBattery(random),
                        Instant.now().getEpochSecond(),
                        new Weather(
                                random.nextInt(101),
                                -20 + random.nextInt(70),
                                random.nextInt(120)
                        ),
                        "SYSTEM_PRODUCER"
                );
                lastMessage = message; // Cache it
            }

            // Serialize and Send
            String json = mapper.writeValueAsString(message);
            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, String.valueOf(stationId), json);

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    exception.printStackTrace();
                    return;
                }
                System.out.println("Sent -> Partition: " + metadata.partition() + " Offset: " + metadata.offset() + " Data: " + json);
            });

            //  Only increment if we actually generated a brand new message
            if (isNewMessage) {
                sequence++;
            }

            Thread.sleep(1000);
        }
    }

    private static String generateBattery(Random random) {

        int x = random.nextInt(100);

        if (x < 30)
            return "low";

        if (x < 70)
            return "medium";

        return "high";
    }
}

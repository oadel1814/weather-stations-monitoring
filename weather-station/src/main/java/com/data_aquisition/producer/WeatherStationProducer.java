
package com.data_aquisition.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.data_aquisition.model.Weather;
import com.data_aquisition.model.WeatherMessage;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Instant;
import java.util.Properties;
import java.util.Random;

public class WeatherStationProducer {

    private static final String TOPIC = "weather-readings";

    public static void main(String[] args) throws Exception {

        String stationEnv = System.getenv("STATION_ID");
        long stationId;

        if (stationEnv != null && stationEnv.contains("-")) {
           String[] parts = stationEnv.split("-");
           stationId = Long.parseLong(parts[parts.length - 1]) + 1;
        } else if (stationEnv != null) {
          // If it's already just a number, use it directly
          stationId = Long.parseLong(stationEnv);
        } else {
         // Fallback if environment variable is missing
         stationId = 1;
        }

        Properties props = new Properties();

        String brokerUrl = System.getenv("KAFKA_BROKER") != null ?
                System.getenv("KAFKA_BROKER") : "localhost:9092";

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerUrl);

        props.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName()
        );

        props.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName()
        );

        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        KafkaProducer<String, String> producer =
                new KafkaProducer<>(props);

        ObjectMapper mapper = new ObjectMapper();
        Random random = new Random();

        long sequence = 1;

        while (true) {

            int dropChance = random.nextInt(100);

            System.out.println("new message created! ");

            WeatherMessage message = new WeatherMessage(
                    stationId,
                    sequence,
                    generateBattery(random),
                    Instant.now().getEpochSecond(),
                    new Weather(
                            random.nextInt(101),
                            50 + random.nextInt(60),
                            random.nextInt(120)
                    )
            );

            sequence++;

            if (dropChance < 10) {
                System.out.println("Dropped Message: " + message.s_no);
                Thread.sleep(1000);
                continue;
            }

            String json = mapper.writeValueAsString(message);

            ProducerRecord<String, String> record =
                    new ProducerRecord<>(
                            TOPIC,
                            String.valueOf(stationId),
                            json
                    );

            producer.send(record, (metadata, exception) -> {

                if (exception != null) {
                    exception.printStackTrace();
                    return;
                }

                System.out.println(
                        "Sent -> Partition: " + metadata.partition()
                                + " Offset: " + metadata.offset()
                                + " Data: " + json
                );
            });

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


package com.data_aquisition.producer;

import com.data_aquisition.infrastructure.KafkaProducerConfig;
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
        long stationId = stationEnv == null ? 1 : Long.parseLong(stationEnv);

        KafkaProducerConfig config = new KafkaProducerConfig();
        KafkaProducer<String, String> producer = new KafkaProducer<>(config.build());


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

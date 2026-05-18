
package com.data_aquisition.streams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.KStream;

import java.util.Properties;

public class RainDetectorStream {

    public static void main(String[] args) {

        Properties props = new Properties();

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "rain-detector-app");

        props.put(
                StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092"
        );

        props.put(
                StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
                Serdes.String().getClass()
        );

        props.put(
                StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
                Serdes.String().getClass()
        );

        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> stream =
                builder.stream("weather-readings");

        ObjectMapper mapper = new ObjectMapper();

        KStream<String, String> rainAlerts = stream.filter((key, value) -> {

            try {

                JsonNode node = mapper.readTree(value);

                int humidity =
                        node.get("weather").get("humidity").asInt();

                return humidity > 70;

            } catch (Exception e) {

                e.printStackTrace();
                return false;
            }
        });

        rainAlerts.to("rain-alerts");

        KafkaStreams streams =
                new KafkaStreams(builder.build(), props);

        streams.start();

        System.out.println("Rain Detector Started");

        Runtime.getRuntime().addShutdownHook(
                new Thread(streams::close)
        );
    }
}

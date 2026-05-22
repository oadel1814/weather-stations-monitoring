package com.data_aquisition.producer;

import com.data_aquisition.model.Weather;
import com.data_aquisition.model.WeatherMessage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import org.apache.kafka.common.serialization.StringSerializer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.Instant;
import java.util.Properties;

public class OpenMeteoChannelAdaptor {

    private static final String TOPIC = "weather-readings";

    // Alexandria coordinates
    private static final double LAT = 31.2001;
    private static final double LON = 29.9187;

    private static final String API_URL =
            "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=%s&longitude=%s" +
                    "&current=temperature_2m,relative_humidity_2m,wind_speed_10m" +
                    "&temperature_unit=fahrenheit" +
                    "&wind_speed_unit=kmh";

    public static void main(String[] args) throws Exception {

        Properties props = new Properties();

        String brokerUrl = System.getenv("KAFKA_BROKER") != null ?
                System.getenv("KAFKA_BROKER") : "localhost:9092";

        props.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                brokerUrl
        );

        props.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName()
        );

        props.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName()
        );

        props.put(
                ProducerConfig.ACKS_CONFIG,
                "all"
        );

        KafkaProducer<String, String> producer =
                new KafkaProducer<>(props);

        HttpClient client =
                HttpClient.newHttpClient();

        ObjectMapper mapper =
                new ObjectMapper();

        long sequence = 1;

        while (true) {

            try {

                String url =
                        String.format(API_URL, LAT, LON);

                HttpRequest request =
                        HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .GET()
                                .build();

                HttpResponse<String> response =
                        client.send(
                                request,
                                HttpResponse.BodyHandlers.ofString()
                        );

                JsonNode root =
                        mapper.readTree(response.body());

                JsonNode current =
                        root.get("current");

                int temperature =
                        (int) Math.round(
                                current.get("temperature_2m")
                                        .asDouble()
                        );

                int humidity =
                        current.get("relative_humidity_2m")
                                .asInt();

                int windSpeed =
                        (int) Math.round(
                                current.get("wind_speed_10m")
                                        .asDouble()
                        );

                WeatherMessage message =
                        new WeatherMessage(
                                100,
                                sequence++,
                                "high",
                                Instant.now().getEpochSecond(),

                                new Weather(
                                        humidity,
                                        temperature,
                                        windSpeed
                                )
                        );

                String json =
                        mapper.writeValueAsString(message);

                ProducerRecord<String, String> record =
                        new ProducerRecord<>(
                                TOPIC,
                                "100",
                                json
                        );

                producer.send(record);

                System.out.println(
                        "REAL WEATHER -> " + json
                );

                // wait 60 seconds
                Thread.sleep(60000);

            } catch (Exception e) {

                e.printStackTrace();

                // retry after 10 sec
                Thread.sleep(10000);
            }
        }
    }
}
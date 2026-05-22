package com.data_aquisition.services;

import com.data_aquisition.infrastructure.KafkaProducerConfig;
import com.data_aquisition.model.Weather;
import com.data_aquisition.model.WeatherMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

public class OpenMeteoKafkaService {
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

    private final HttpClient client;
    private final ObjectMapper mapper;
    private final KafkaProducer<String, String> producer;

    public OpenMeteoKafkaService() {
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();

        KafkaProducerConfig config = new KafkaProducerConfig();
        this.producer = new KafkaProducer<>(config.build());
    }

    public void fetchAndPublish(long stationId, long sequence) throws Exception {

        // 1. Fetch data from API
        String url = String.format(API_URL, LAT, LON);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        JsonNode root = mapper.readTree(response.body());

        JsonNode current = root.get("current");

        int temperature = (int) Math.round(current.get("temperature_2m").asDouble());
        int humidity = current.get("relative_humidity_2m").asInt();
        int windSpeed = (int) Math.round(current.get("wind_speed_10m").asDouble());

        Weather weatherData = new Weather(humidity, temperature, windSpeed);

        // 2. Wrap the payload in the Envelope
        WeatherMessage message =
                new WeatherMessage(
                        stationId,
                        sequence,
                        "N/A",
                        Instant.now().getEpochSecond(),

                        new Weather(
                                humidity,
                                temperature,
                                windSpeed
                        )
                        ,
                        "API_STATION"
                );

        // 3. Serialize and Send to Kafka
        String json = mapper.writeValueAsString(message);
        ProducerRecord<String, String> record = new ProducerRecord<>(
                TOPIC,
                String.valueOf(stationId),
                json
        );

        producer.send(record);
        System.out.println("REAL WEATHER -> " + json);
    }

    public void close() {
        if (producer != null) {
            producer.close();
        }
    }
}

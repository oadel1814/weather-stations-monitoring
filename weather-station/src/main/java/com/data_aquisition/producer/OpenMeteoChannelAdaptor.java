package com.data_aquisition.producer;

import com.data_aquisition.services.OpenMeteoKafkaService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OpenMeteoChannelAdaptor {

    public static void main(String[] args) {

        System.out.println("Starting Open-Meteo Polling Adapter...");

        // Instantiate your combined service
        OpenMeteoKafkaService meteoService = new OpenMeteoKafkaService();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        long liveStationId = 1000;
        final long[] sequence = {1};

        // Define the polling task
        Runnable fetchWeatherDataTask = () -> {
            try {
                meteoService.fetchAndPublish(liveStationId, sequence[0]++);
            } catch (Exception e) {
                System.err.println("Error during polling cycle. Will retry. Reason: " + e.getMessage());
            }
        };

        // Schedule to run every 1 minute
        scheduler.scheduleAtFixedRate(fetchWeatherDataTask, 0, 1, TimeUnit.MINUTES);  // in real the reading changes in 15 to 60 min
        //so in production it may be every 60 min

        // Graceful Shutdown Hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down Polling Adapter gracefully...");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
            meteoService.close(); // Close the Kafka producer safely
            System.out.println("Adapter shut down complete.");
        }));
    }
}
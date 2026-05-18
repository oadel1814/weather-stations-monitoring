package com.weather;
import com.weather.MessageConsumer.BitcaskService;
import com.weather.MessageConsumer.ParquetService;
import com.weather.MessageConsumer.RecordProcessor;
import com.weather.MessageConsumer.WeatherDataConsumer;
import com.weather.infrastructure.KafkaConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class App
{
    private static final Logger log = LoggerFactory.getLogger(App.class);
    public static void main( String[] args )
    {

        log.info("Central Station Server starting...");

        try {
            // 1. Initialize your processor and its services
            RecordProcessor recordProcessor = new RecordProcessor(new BitcaskService(), new ParquetService());

            // 2. Get the configuration (Make sure your config class returns Properties!)
            Properties kafkaProps = KafkaConsumerConfig.setupConsumer();

            // 3. Create the Runnable Worker
            WeatherDataConsumer worker = new WeatherDataConsumer(kafkaProps, recordProcessor);

            //  Put the worker in a Thread and START it
            Thread consumerThread = new Thread(worker);
            consumerThread.start();
            log.info("Weather Data Consumer thread started successfully.");

            // 5. Register the Graceful Shutdown Hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutdown signal received (Ctrl+C). Stopping consumer gracefully...");


                worker.shutdown();

                try {
                    // Tell the Main thread to wait until the Consumer thread finishes its final commit
                    consumerThread.join();
                } catch (InterruptedException e) {
                    log.error("Thread interrupted during shutdown", e);
                }

                log.info("Central Station Server stopped safely.");
            }));

        } catch (Exception ex) {
            log.error("Fatal error during startup", ex);
            ex.printStackTrace();
        }
    }
}


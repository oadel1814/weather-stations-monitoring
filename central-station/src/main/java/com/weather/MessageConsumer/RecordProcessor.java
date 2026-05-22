package com.weather.MessageConsumer;

import com.weather.MessageConsumer.interfaces.IBitcask;
import com.weather.MessageConsumer.interfaces.IParquet;
import com.weather.model.WeatherMessage;
import com.weather.publishers.InvalidMessagePublisher;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RecordProcessor {

    private static final Logger log = LoggerFactory.getLogger(RecordProcessor.class);
    private final IBitcask bitmaskService;
    private final IParquet parquetService;
    private final InvalidMessagePublisher invalidMessagePublisher;

    private final Map<String, Boolean> seenMessages = Collections.synchronizedMap(
            new LinkedHashMap<>(10_000, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > 10_000;
                }
            }
    );


    public RecordProcessor(IBitcask bitcaskService ,IParquet parquetService,InvalidMessagePublisher invalidMessagePublisher){
        this.bitmaskService =bitcaskService;
        this.parquetService = parquetService;
        this.invalidMessagePublisher = invalidMessagePublisher;
    }

    public void process(WeatherMessage weatherMessage){

        String messageKey = weatherMessage.station_id + "-" + weatherMessage.s_no;
        if (seenMessages.putIfAbsent(messageKey, true) != null) {
            log.warn("DUPLICATE DETECTED: station={} s_no={} — skipping.",
                    weatherMessage.station_id, weatherMessage.s_no);
            return;
        }

        if (weatherMessage.station_id<1000 && !isValid(weatherMessage))
        {
            log.warn("INVALID DATA: Station {} reported impossible metrics. Routing to Invalid Channel.", weatherMessage.station_id);
            // send to invalid queue
            invalidMessagePublisher.publish(weatherMessage, "Violated physical weather bounds");
            return;
        }
        log.info("process() called");
        log.info(" Station Type: {} | Processing Station: {} | Seq: {} | Temp: {}C | Humidity: {}%",
                weatherMessage.station_type,
                weatherMessage.station_id,
                weatherMessage.s_no,
                weatherMessage.weather.temperature,
                weatherMessage.weather.humidity);
          bitmaskService.process(weatherMessage);
          parquetService.process(weatherMessage);
    }

    private boolean isValid(WeatherMessage msg) {
        if (msg.weather == null) return false;

        if (msg.weather.temperature < -90 || msg.weather.temperature > 60) return false;
        if (msg.weather.humidity < 0 || msg.weather.humidity > 100) return false;
        if (msg.weather.wind_speed < 0 || msg.weather.wind_speed > 400) return false;

        return true;
    }





}

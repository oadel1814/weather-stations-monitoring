package com.weather.MessageConsumer;
import com.ddia.bitcask.Impl.BitcaskFactory;
import com.ddia.bitcask.interfaces.Bitcask;
import com.weather.MessageConsumer.interfaces.IBitcask;
import com.weather.model.WeatherMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BitcaskService implements IBitcask {

    private static final Logger log = LoggerFactory.getLogger(BitcaskService.class);
    String bitcaskDir = System.getenv().getOrDefault("BITCASK_DIR", "/data/bitcask");
    private final Bitcask bitcask;

    public BitcaskService() {
        bitcask = BitcaskFactory.getInstance(bitcaskDir);
        log.info("BitcaskService initialized at {}", bitcaskDir);
    }

    @Override
    public void process(WeatherMessage msg) {
        try {
            String key   = String.valueOf(msg.station_id);
            String value = toJson(msg);
            bitcask.put(key, value);
            log.info("Bitcask add key : {} with value : {}", key, value );
        } catch (Exception e) {
            log.error("Failed to write to Bitcask for station {}: {}",
                    msg.station_id, e.getMessage());
        }

    }

    private String toJson(WeatherMessage msg) {
        return String.format(
                "{\"station_id\":%d,\"s_no\":%d,\"battery_status\":\"%s\"," +
                        "\"status_timestamp\":%d,\"humidity\":%d,\"temperature\":%d,\"wind_speed\":%d}",
                msg.station_id, msg.s_no, msg.battery_status, msg.status_timestamp,
                msg.weather.humidity, msg.weather.temperature, msg.weather.wind_speed
        );
    }
}

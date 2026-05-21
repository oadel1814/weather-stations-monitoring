package com.weather.MessageConsumer;

import com.weather.MessageConsumer.interfaces.IBitcask;
import com.weather.MessageConsumer.interfaces.IParquet;
import com.weather.model.WeatherMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordProcessor {

    private static final Logger log = LoggerFactory.getLogger(RecordProcessor.class);
    private final IBitcask bitmaskService;
    private final IParquet parquetService;

    public RecordProcessor(IBitcask bitcaskService ,IParquet parquetService){
        this.bitmaskService =bitcaskService;
        this.parquetService = parquetService;
    }

    public void process(WeatherMessage weatherMessage){
        log.info("process() called");
        log.info("Processing Station: {} | Seq: {} | Temp: {}C | Humidity: {}%",
                weatherMessage.station_id,
                weatherMessage.s_no,
                weatherMessage.weather.temperature,
                weatherMessage.weather.humidity);
          bitmaskService.process(weatherMessage);
          parquetService.process(weatherMessage);
    }



}

package com.weather.MessageConsumer.interfaces;


import com.weather.model.WeatherMessage;

public interface IBitcask {
    void process(WeatherMessage msg);
    // to be continued
}

package com.weather.MessageConsumer.interfaces;

import com.weather.model.WeatherMessage;

public interface IParquet {
    void process(WeatherMessage msg);
}

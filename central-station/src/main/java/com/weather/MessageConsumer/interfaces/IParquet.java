package com.weather.MessageConsumer.interfaces;

import com.weather.model.Weather;

public interface IParquet {
    void process(Weather weather);
}

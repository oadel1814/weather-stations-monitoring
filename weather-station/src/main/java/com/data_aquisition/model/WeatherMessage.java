
package com.data_aquisition.model;

//enveloper
public class WeatherMessage {

    public long station_id;
    public long s_no;
    public String station_type;
    public String battery_status;
    public long status_timestamp;
    public Weather weather;

    public WeatherMessage() {}

    public WeatherMessage(long station_id,
                          long s_no,
                          String battery_status,
                          long status_timestamp,
                          Weather weather,
                          String station_type
                             ) {

        this.station_id = station_id;
        this.s_no = s_no;
        this.battery_status = battery_status;
        this.status_timestamp = status_timestamp;
        this.weather = weather;
        this.station_type=station_type;
    }
}

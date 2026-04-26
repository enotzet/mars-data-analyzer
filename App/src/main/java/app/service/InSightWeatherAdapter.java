package app.service;

import java.util.List;

public interface InSightWeatherAdapter {

    List<WeatherRecord> fetchWeather();

    record WeatherRecord(
            String sol,
            String season,
            String firstUtc,
            String lastUtc,
            Double avgTempC,
            Double minTempC,
            Double maxTempC,
            Double avgWindMs,
            Double maxWindMs,
            Double avgPressurePa,
            Double minPressurePa,
            Double maxPressurePa,
            String prevailingWindDir
    ) {}
}

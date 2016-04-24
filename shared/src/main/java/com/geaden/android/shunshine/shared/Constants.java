package com.geaden.android.shunshine.shared;

/**
 * Some shared constants between wearable and app.
 *
 * @author Gennady Denisov
 */
public final class Constants {
    private Constants() {

    }

    // Data layer keys
    public static final String EXTRA_HIGH_TEMP = "extra_high_temp";
    public static final String EXTRA_LOW_TEMP = "extra_low_temp";
    public static final String EXTRA_ART = "extra_art";
    public static final String EXTRA_TIME = "extra_time";
    public static final String EXTRA_UNITS_METRIC = "extra_units";

    // Wear Data API path
    public static final String WEATHER_PATH = "/weather";
    public static final String WEATHER_REQUEST_PATH = "/weather-request";

    public static final String GOOGLE_API_CLIENT_ERROR_MSG = "Failed to connect to GoogleApiClient.";

    // 30 seconds
    public static final long GOOGLE_API_CLIENT_TIMEOUT_S = 30;
    public static final long GET_CAPABILITY_TIMEOUT_S = 30;
}

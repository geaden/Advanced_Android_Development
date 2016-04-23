package com.example.android.sunshine.app.wear;

import android.content.Intent;
import android.util.Log;

import com.geaden.android.shunshine.shared.Constants;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Listener for weather update requests from wearable.
 *
 * @author Gennady Denisov
 */
public class WeatherRequestListenerService extends WearableListenerService {
    private static final String TAG = "WeatherRequestListener";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        if (messageEvent.getPath().equals(Constants.WEATHER_REQUEST_PATH)) {
            Log.d(TAG, "Message to update weather and send it to the wearable received!");
            Intent intent = new Intent(this, SendWeatherDataService.class);
            intent.setAction(SendWeatherDataService.ACTION_SEND_WEATHER_DATA);
            startService(intent);
        }
    }
}

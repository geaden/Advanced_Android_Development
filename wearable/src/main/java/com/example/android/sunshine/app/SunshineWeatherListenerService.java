package com.example.android.sunshine.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.geaden.android.shunshine.shared.AbstractGoogleApiClientWrapper;
import com.geaden.android.shunshine.shared.Constants;
import com.geaden.android.shunshine.shared.Utils;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;


/**
 * A Wear listener service, used to receive inbound messages from
 * other devices.
 *
 * @author Gennady Denisov
 */
public class SunshineWeatherListenerService extends WearableListenerService {

    private static final String TAG = "SunshineWeatherService";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        Log.d(TAG, "Message received " + Arrays.toString(messageEvent.getData()));
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);
        for (final DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED &&
                    event.getDataItem().getUri().getPath().equals(Constants.WEATHER_PATH)) {
                Log.d(TAG, "Weather data received!");

                new AbstractGoogleApiClientWrapper(this) {
                    @Override
                    public void executeAction(GoogleApiClient googleApiClient) {
                        DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                        Asset artAsset = dataMapItem.getDataMap().getAsset(Constants.EXTRA_ART);
                        double hi = dataMapItem.getDataMap().getDouble(Constants.EXTRA_HIGH_TEMP);
                        double lo = dataMapItem.getDataMap().getDouble(Constants.EXTRA_LOW_TEMP);
                        boolean isMetric = dataMapItem.getDataMap().getBoolean(Constants.EXTRA_UNITS_METRIC);
                        Bitmap bitmap = Utils.loadBitmapFromAsset(googleApiClient, artAsset);
                        updateWatchFaceWeatherInfo(hi, lo, isMetric, bitmap);
                    }
                }.wrap();
            }
        }
    }

    /**
     * Updates weather info on the watch face.
     *
     * @param hi       max temperature.
     * @param lo       min temperature.
     * @param isMetric indicates whether units in Celsius of in Fahrenheits.
     * @param bitmap   art based on weather.
     */
    private void updateWatchFaceWeatherInfo(double hi, double lo, boolean isMetric, Bitmap bitmap) {
        Log.d(TAG, "Updating wearable weather info");
        Intent intent = new Intent(SunshineWatchFaceService.ACTION_WEATHER_RECEIVED);

        // Temperature is in Celsius by default. If unit changed, do transformation.
        if (!isMetric) {
            hi = Utils.convertToImperial(hi);
            lo = Utils.convertToImperial(lo);
        }

        intent.putExtra(SunshineWatchFaceService.EXTRA_HI, hi);
        intent.putExtra(SunshineWatchFaceService.EXTRA_LO, lo);
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        if (null != bitmap) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 50, bs);
        }
        intent.putExtra(SunshineWatchFaceService.EXTRA_ART, bs.toByteArray());
        Log.d(TAG, "Sending broadcast");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}

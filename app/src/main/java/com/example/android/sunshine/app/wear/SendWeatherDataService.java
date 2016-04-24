package com.example.android.sunshine.app.wear;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.example.android.sunshine.app.BuildConfig;
import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;
import com.geaden.android.shunshine.shared.AbstractGoogleApiClientWrapper;
import com.geaden.android.shunshine.shared.Constants;
import com.geaden.android.shunshine.shared.Utils;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Intent service to send weather data to nearby wearable.
 *
 * @author Gennady Denisov
 */
public class SendWeatherDataService extends IntentService {
    public static final String ACTION_SEND_WEATHER_DATA =
            "com.example.android.sunshine.app.ACTION_SEND_WEATHER_DATA";

    private static final String TAG = "SendWeatherDataService";

    public SendWeatherDataService() {
        super("SendWeatherDataService");
    }

    /**
     * Helper method that allows to initiate sending weather data to the wearables.
     *
     * @param context the context.
     */
    public static void launchService(Context context) {
        Intent intent = new Intent(context, SendWeatherDataService.class);
        intent.setAction(ACTION_SEND_WEATHER_DATA);
        context.startService(intent);
    }

    // The projection with required for wearables data.
    private static final String[] WEARABLE_WEATHER_PROJECTION = new String[]{
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
    };

    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction().equals(ACTION_SEND_WEATHER_DATA)) {
            Log.d(TAG, "Handling send weather data intent");
            new AbstractGoogleApiClientWrapper(this) {
                @Override
                public void executeAction(GoogleApiClient googleApiClient) {
                    Context context = SendWeatherDataService.this;
                    String locationQuery = Utility.getPreferredLocation(context);
                    Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                            locationQuery, System.currentTimeMillis());

                    // As always query the content provider.
                    Cursor cursor = context.getContentResolver().query(weatherUri,
                            WEARABLE_WEATHER_PROJECTION, null, null, null);

                    if (null != cursor && cursor.moveToFirst()) {
                        int weatherId = cursor.getInt(INDEX_WEATHER_ID);
                        double high = cursor.getDouble(INDEX_MAX_TEMP);
                        double low = cursor.getDouble(INDEX_MIN_TEMP);
                        int resourceId = Utility.getIconResourceForWeatherCondition(weatherId);
                        Bitmap art = BitmapFactory.decodeResource(context.getResources(), resourceId);

                        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(Constants.WEATHER_PATH);
                        putDataMapReq.setUrgent();
                        putDataMapReq.getDataMap().putDouble(Constants.EXTRA_HIGH_TEMP, high);
                        putDataMapReq.getDataMap().putDouble(Constants.EXTRA_LOW_TEMP, low);
                        putDataMapReq.getDataMap().putBoolean(Constants.EXTRA_UNITS_METRIC,
                                Utility.isMetric(SendWeatherDataService.this));
                        putDataMapReq.getDataMap().putAsset(Constants.EXTRA_ART,
                                Utils.createAssetFromBitmap(art));

                        if (BuildConfig.DEBUG) {
                            // For debug purposes only.
                            putDataMapReq.getDataMap().putLong(Constants.EXTRA_TIME, System.currentTimeMillis());
                        }

                        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();

                        PendingResult<DataApi.DataItemResult> pendingResult =
                                Wearable.DataApi.putDataItem(googleApiClient, putDataReq);
                        DataApi.DataItemResult result = pendingResult.await();

                        if (result.getStatus().isSuccess()) {
                            Log.d(TAG, "Data item successfully set: " + result.getDataItem().getUri());
                        }
                    }
                    if (null != cursor) cursor.close();
                }
            }.wrap();
        }
    }
}

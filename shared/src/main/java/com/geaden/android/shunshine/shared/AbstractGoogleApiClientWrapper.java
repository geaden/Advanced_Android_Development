package com.geaden.android.shunshine.shared;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

/**
 * Helper class to wrap {@link GoogleApiClient} initialization.
 *
 * @author Gennady Denisov
 */
public abstract class AbstractGoogleApiClientWrapper {
    private static final String TAG = "GoogleApiClientHandler";
    private final Context mContext;

    public AbstractGoogleApiClientWrapper(Context context) {
        mContext = context;
    }

    /**
     * Wraps initialization, connection and disconnection of {@link GoogleApiClient} to call #executeAction method.
     * Should never be called from Main/UI thread.
     */
    public void wrap() {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(Wearable.API)
                .build();

        ConnectionResult connectionResult = googleApiClient.blockingConnect(
                Constants.GOOGLE_API_CLIENT_TIMEOUT_S, TimeUnit.SECONDS);

        if (!connectionResult.isSuccess() || !googleApiClient.isConnected()) {
            Log.e(TAG, String.format(Constants.GOOGLE_API_CLIENT_ERROR_MSG,
                    connectionResult.getErrorCode()));
            return;
        }

        executeAction(googleApiClient);

        googleApiClient.disconnect();
    }

    /**
     * The action to be executed with the initialized Google API client.
     *
     * @param googleApiClient the {@link GoogleApiClient}
     */
    public abstract void executeAction(GoogleApiClient googleApiClient);
}

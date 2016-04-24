package com.example.android.sunshine.app;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.geaden.android.shunshine.shared.AbstractGoogleApiClientWrapper;
import com.geaden.android.shunshine.shared.Constants;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Intent service to trigger request for weather data from the connected handheld device
 * with Sunshine Weather Update capabilities.
 *
 * @author Gennady Denisov
 */
public class WeatherRequestService extends IntentService {
    private static final String TAG = WeatherRequestService.class.getSimpleName();

    public WeatherRequestService() {
        super("WeatherUpdateService");
    }

    Set<String> weatherNodeIds;

    /**
     * Sets up nodes with weather update capabilities.
     *
     * @param googleApiClient the {@link GoogleApiClient}
     */
    private void setupWeatherUpdateNode(GoogleApiClient googleApiClient) {
        CapabilityApi.GetCapabilityResult result =
                Wearable.CapabilityApi.getCapability(
                        googleApiClient,
                        getString(R.string.sunshine_weather_capability),
                        CapabilityApi.FILTER_REACHABLE).await(Constants.GET_CAPABILITY_TIMEOUT_S,
                        TimeUnit.SECONDS);

        if (result.getStatus().isSuccess()) {
            updateNodes(result.getCapability());
        } else {
            Log.d(TAG, "Failed to get capabilities.");
        }
    }

    /**
     * Updates information about available nodes.
     *
     * @param capabilityInfo information to get nodes with required capabilities.
     */
    private void updateNodes(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        weatherNodeIds = getNodes(connectedNodes);
    }

    /**
     * Picks nearest node with the requested capabilities.
     *
     * @param nodes set of nodes.
     * @return nearest node.
     */
    private Set<String> getNodes(Set<Node> nodes) {
        weatherNodeIds = new HashSet<>();
        // Find a nearby node or pick one arbitrarily
        for (Node node : nodes) {
            weatherNodeIds.add(node.getId());
        }
        return weatherNodeIds;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction().equals(SunshineWatchFaceService.ACTION_REQUEST_WEATHER)) {
            Log.d(TAG, "Requesting weather info.");
            new AbstractGoogleApiClientWrapper(this) {
                @Override
                public void executeAction(GoogleApiClient googleApiClient) {
                    setupWeatherUpdateNode(googleApiClient);

                    Random random = new Random();
                    for (String nodeId : weatherNodeIds) {
                        Log.d(TAG, "Sending message to " + nodeId);
                        byte[] data = new byte[10];
                        random.nextBytes(data);
                        Wearable.MessageApi.sendMessage(googleApiClient, nodeId,
                                Constants.WEATHER_REQUEST_PATH, data).setResultCallback(
                                new ResultCallback<MessageApi.SendMessageResult>() {
                                    @Override
                                    public void onResult(@NonNull MessageApi.SendMessageResult
                                                                 result) {
                                        if (!result.getStatus().isSuccess()) {
                                            // Failed to send message
                                            Log.d(TAG, "Failed to deliver a message.");
                                        } else {
                                            Log.d(TAG, "Message is delivered");
                                        }
                                    }
                                }
                        );
                    }
                }
            }.wrap();
        }
    }
}

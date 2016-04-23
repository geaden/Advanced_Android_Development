package com.geaden.android.shunshine.shared;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Some shared utils between wearable and app.
 *
 * @author Gennady Denisov
 */
public final class Utils {
    private static final String TAG = "Utils";

    private Utils() {

    }

    /**
     * Convert an asset into a bitmap object synchronously. Only call this
     * method from a background thread (it should never be called from the
     * main/UI thread as it blocks).
     */
    public static Bitmap loadBitmapFromAsset(GoogleApiClient googleApiClient, Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }
        // convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                googleApiClient, asset).await().getInputStream();

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.");
            return null;
        }
        // decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
    }

    /**
     * Create a wearable asset from a bitmap.
     */
    public static Asset createAssetFromBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            return Asset.createFromBytes(byteStream.toByteArray());
        }
        return null;
    }
}

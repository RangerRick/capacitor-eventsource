package com.raccoonfink.plugins.eventsource;

import android.util.Log;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.MessageEvent;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;

enum ReadyState {
    CONNECTING,
    OPEN,
    CLOSED
}

@CapacitorPlugin
public class EventSource extends Plugin implements EventHandler {

    private static final String TAG = "EventSource";

    private com.launchdarkly.eventsource.EventSource sse;
    private URL url;
    private boolean opened = false;

    @PluginMethod
    public void configure(final PluginCall call) {
        final String urlString = call.getString("url");

        if (urlString == null) {
            call.reject("you must provide a URL");
            return;
        }

        Log.i(TAG, "configuring event source with URL " + urlString);

        try {
            this.url = new URL(urlString);
        } catch (final MalformedURLException e) {
            Log.w(TAG, "invalid URL", e);
            call.reject("invalid URL");
        }

        if (this.sse != null) {
            Log.v(TAG, "configure() called, but there is an existing event source; making sure it's closed");
            this.opened = false;
            this.sse.close();
            this.sse = null;
        }

        final int reconnectTime = call.getInt("reconnectTime", 1000);
        final int maxReconnectTime = call.getInt("maxReconnectTime", 60000);
        final int backoffResetThreshold = call.getInt("backoffResetThreshold", 5000);
        final int idleTimeout = call.getInt("idleTimeout", 30000);

        final Request request = new Request.Builder().url(this.url).build();
        final OkHttpClient client = new OkHttpClient.Builder().readTimeout(idleTimeout, TimeUnit.MILLISECONDS).build();
        try {
            this.sse =
                new com.launchdarkly.eventsource.EventSource.Builder(this, this.url.toURI())
                    .reconnectTimeMs(reconnectTime)
                    .maxReconnectTimeMs(maxReconnectTime)
                    .backoffResetThresholdMs(backoffResetThreshold)
                    .readTimeoutMs(idleTimeout)
                    .build();
        } catch (final URISyntaxException e) {
            e.printStackTrace();
        }

        call.resolve();
    }

    private void safeNotifyListeners(final String eventName, final JSObject ret) {
        if (eventName == null || ret == null) {
            Log.w(TAG, "Failed to notify listeners, incomplete update");
            return;
        }
        try {
            this.notifyListeners(eventName, ret, false);
        } catch (final Exception e) {
            Log.e(TAG, "Failed to notify listeners, update dropped: " + ret.toString(), e);
        }
    }

    @PluginMethod
    public void open(final PluginCall call) {
        Log.i(TAG, "opening connection");

        final JSObject ret = new JSObject();
        ret.put("state", ReadyState.CONNECTING);
        this.safeNotifyListeners("readyStateChanged", ret);

        this.sse.start();
        this.opened = true;

        call.resolve();
    }

    @PluginMethod
    public void close(final PluginCall call) {
        Log.i(TAG, "closing connection");

        if (this.sse != null) {
            this.opened = false;
            this.sse.close();
            this.sse = null;
        }

        call.resolve();
    }

    public void onOpen() {
        if (!this.opened) {
            Log.v(TAG, "onOpen skipped (this.opened=false)");
            return;
        }

        Log.v(TAG, "onOpen");

        final JSObject ret = new JSObject();
        ret.put("value", null);
        this.safeNotifyListeners("open", ret);

        final JSObject rsRet = new JSObject();
        rsRet.put("state", ReadyState.OPEN);
        this.safeNotifyListeners("readyStateChanged", rsRet);
    }

    public void onMessage(final String event, final MessageEvent messageEvent) {
        if (!this.opened) {
            Log.v(TAG, "onMessage skipped (this.opened=false)");
            return;
        }

        Log.v(TAG, "onMessage: " + messageEvent.getData());

        final JSObject ret = new JSObject();
        ret.put("message", messageEvent.getData());
        this.safeNotifyListeners("message", ret);
    }

    public void onComment(final String comment) {
        Log.v(TAG, "onComment: " + comment);
    }

    public void onError(final Throwable throwable) {
        if (!this.opened) {
            Log.v(TAG, "onError skipped (this.opened=false)");
            return;
        }

        Log.e(TAG, "onError: " + throwable.getMessage(), throwable);

        final JSObject ret = new JSObject();
        ret.put("error", throwable.getMessage());
        this.safeNotifyListeners("error", ret);
    }

    public void onClosed() {
        if (!this.opened) {
            Log.v(TAG, "onClosed skipped (this.opened=false)");
            return;
        }

        Log.v(TAG, "onClosed");

        final JSObject ret = new JSObject();
        ret.put("state", ReadyState.CLOSED);
        this.safeNotifyListeners("readyStateChanged", ret);
    }
}

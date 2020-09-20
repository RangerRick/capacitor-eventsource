package com.raccoonfink.plugins.eventsource;

import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

enum ReadyState {
    CONNECTING,
    OPEN,
    CLOSED
}

@NativePlugin
public class EventSource extends Plugin {
    private static final String TAG = "EventSource";

    private okhttp3.sse.EventSource sse;
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
            this.sse.cancel();
            this.sse = null;
        }

        call.resolve();
    }

    @PluginMethod
    public void open(final PluginCall call) {
        Log.i(TAG, "opening connection");

        final JSObject ret = new JSObject();
        ret.put("state", ReadyState.CONNECTING);
        this.notifyListeners("readyStateChanged", ret, true);

        final int idleTimeout = call.getInt("idleTimeout", 30000);

        final Request request = new Request.Builder().url(this.url).build();
        final OkHttpClient client = new OkHttpClient.Builder().build();
        okhttp3.sse.EventSource.Factory factory = EventSources.createFactory(client);

        final EventSource me = this;

        this.sse =
            factory.newEventSource(
                request,
                new EventSourceListener() {

                    @Override
                    public void onOpen(@NotNull okhttp3.sse.EventSource eventSource, @NotNull Response response) {
                        me.onOpen(eventSource, response);
                    }

                    @Override
                    public void onEvent(
                        @NotNull okhttp3.sse.EventSource eventSource,
                        @Nullable String id,
                        @Nullable String type,
                        @NotNull String data
                    ) {
                        if (type == "message") {
                            me.onMessage(eventSource, id, type, data);
                        } else if (type == "comment") {
                            me.onComment(eventSource, data);
                        } else {
                            Log.w(TAG, "unhandled event id=" + id + ", type=" + type + ", data=" + data);
                        }
                    }

                    @Override
                    public void onFailure(
                        @NotNull okhttp3.sse.EventSource eventSource,
                        @Nullable Throwable t,
                        @Nullable Response response
                    ) {
                        me.onError(eventSource, t, response);
                    }

                    @Override
                    public void onClosed(@NotNull okhttp3.sse.EventSource eventSource) {
                        me.onClosed(eventSource);
                    }
                }
            );

        this.opened = true;

        call.resolve();
    }

    @PluginMethod
    public void close(final PluginCall call) {
        Log.i(TAG, "closing connection");

        if (this.sse != null) {
            this.opened = false;
            this.sse.cancel();
            this.sse = null;
        }

        call.resolve();
    }

    public void onOpen(final okhttp3.sse.EventSource sse, final Response response) {
        if (!this.opened) {
            Log.v(TAG, "onOpen skipped (this.opened=false)");
            return;
        }

        Log.v(TAG, "onOpen");

        final JSObject ret = new JSObject();
        ret.put("value", null);
        this.notifyListeners("open", ret, true);

        final JSObject rsRet = new JSObject();
        rsRet.put("state", ReadyState.OPEN);
        this.notifyListeners("readyStateChanged", rsRet, true);
    }

    public void onMessage(final okhttp3.sse.EventSource sse, final String id, final String event, final String message) {
        if (!this.opened) {
            Log.v(TAG, "onMessage skipped (this.opened=false)");
            return;
        }

        Log.v(TAG, "onMessage: " + message);

        final JSObject ret = new JSObject();
        ret.put("message", message);
        this.notifyListeners("message", ret, true);
    }

    public void onComment(final okhttp3.sse.EventSource sse, final String comment) {
        Log.v(TAG, "onComment: " + comment);
    }

    public boolean onError(final okhttp3.sse.EventSource sse, final Throwable throwable, final Response response) {
        if (!this.opened) {
            Log.v(TAG, "onError skipped (this.opened=false)");
            return true;
        }

        Log.w(TAG, "onError: " + throwable.getMessage());

        final JSObject ret = new JSObject();
        ret.put("error", throwable.getMessage());
        this.notifyListeners("error", ret, true);

        return true; // True to retry, false otherwise
    }

    public void onClosed(final okhttp3.sse.EventSource sse) {
        if (!this.opened) {
            Log.v(TAG, "onClosed skipped (this.opened=false)");
            return;
        }

        Log.v(TAG, "onClosed");

        final JSObject ret = new JSObject();
        ret.put("state", ReadyState.CLOSED);
        this.notifyListeners("readyStateChanged", ret, true);
    }
}

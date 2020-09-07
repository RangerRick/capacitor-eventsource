package com.raccoonfink.plugins.eventsource;

import android.util.Log;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.here.oksse.OkSse;
import com.here.oksse.ServerSentEvent;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

enum ReadyState {
    CONNECTING,
    OPEN,
    CLOSED
}

@NativePlugin
public class EventSource extends Plugin implements ServerSentEvent.Listener {
    private static final String TAG = "EventSource";

    private ServerSentEvent sse;
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
            this.sse.close();
            this.opened = false;
            this.see = null;
        }

        call.resolve();
    }

    @PluginMethod
    public void open(final PluginCall call) {
        Log.i(TAG, "opening connection");

        final JSObject ret = new JSObject();
        ret.put("state", ReadyState.CONNECTING);
        this.notifyListeners("readyStateChanged", ret, true);

        final Request request = new Request.Builder().url(this.url).build();
        final OkSse okSse = new OkSse();
        this.sse = okSse.newServerSentEvent(request, this);

        this.opened = true;

        call.resolve();
    }

    @PluginMethod
    public void close(final PluginCall call) {
        Log.i(TAG, "closing connection");

        if (this.sse != null) {
            this.sse.close();
            this.opened = false;
            this.sse = null;
        }

        call.resolve();
    }

    @Override
    public void onOpen(final ServerSentEvent sse, final Response response) {
        Log.v(TAG, "onOpen");

        final JSObject ret = new JSObject();
        ret.put("value", null);
        this.notifyListeners("open", ret, true);

        final JSObject rsRet = new JSObject();
        rsRet.put("state", ReadyState.OPEN);
        this.notifyListeners("readyStateChanged", rsRet, true);
    }

    @Override
    public void onMessage(final ServerSentEvent sse, final String id, final String event, final String message) {
        Log.v(TAG, "onMessage: " + message);

        final JSObject ret = new JSObject();
        ret.put("message", message);
        this.notifyListeners("message", ret, true);
    }

    @Override
    public void onComment(final ServerSentEvent sse, final String comment) {
        Log.v(TAG, "onComment: " + comment);
    }

    @Override
    public Request onPreRetry(final ServerSentEvent sse, final Request originalRequest) {
        Log.v(TAG, "onPreRetry");
        return originalRequest;
    }

    @Override
    public boolean onRetryTime(final ServerSentEvent sse, final long milliseconds) {
        Log.v(TAG, "onRetryTime: " + milliseconds);
        return true; // True to use the new retry time received by SSE
    }

    @Override
    public boolean onRetryError(final ServerSentEvent sse, final Throwable throwable, final Response response) {
        Log.w(TAG, "onRetryError: " + throwable.getMessage());

        final JSObject ret = new JSObject();
        ret.put("error", throwable.getMessage());
        this.notifyListeners("error", ret, true);

        return true; // True to retry, false otherwise
    }

    @Override
    public void onClosed(final ServerSentEvent sse) {
        Log.v(TAG, "onClosed");

        final JSObject ret = new JSObject();
        ret.put("state", ReadyState.CLOSED);
        this.notifyListeners("readyStateChanged", ret, true);
    }
}

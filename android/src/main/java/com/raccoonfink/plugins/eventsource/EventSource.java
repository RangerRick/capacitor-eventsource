package com.raccoonfink.plugins.eventsource;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.tylerjroach.eventsource.EventSourceHandler;
import com.tylerjroach.eventsource.MessageEvent;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import org.json.JSONObject;

enum ReadyState {
    CONNECTING,
    OPEN,
    CLOSED
}

@NativePlugin
public class EventSource extends Plugin implements EventSourceHandler {
    private com.tylerjroach.eventsource.EventSource eventSource;
    private URL url;
    private boolean opened = false;

    @PluginMethod
    public void configure(final PluginCall call) {
        final String urlString = call.getString("url");

        if (urlString == null) {
            call.reject("you must provide a URL");
            return;
        }

        System.err.println("configuring event source with URL " + urlString);

        try {
            this.url = new URL(urlString);
        } catch (final MalformedURLException e) {
            call.reject("invalid URL");
        }

        if (this.eventSource != null) {
            this.eventSource.close();
            this.opened = false;
        }

        try {
            this.eventSource = new com.tylerjroach.eventsource.EventSource.Builder(this.url.toURI()).eventHandler(this).build();
        } catch (final URISyntaxException e) {
            call.reject("invalid URL");
        }

        call.resolve();
    }

    public void open(final PluginCall call) {
        this.opened = true;
        final JSObject ret = new JSObject();
        ret.put("state", ReadyState.CONNECTING);
        this.notifyListeners("onReadyStateChanged", ret);
        this.eventSource.connect();
    }

    public void close(final PluginCall call) {
        if (this.eventSource != null && this.opened) {
            this.eventSource.close();
            this.opened = false;
        }
    }

    @Override
    public void onConnect() throws Exception {
        final JSObject ret = new JSObject();
        ret.put("value", null);
        this.notifyListeners("onOpen", ret);

        final JSObject rsRet = new JSObject();
        rsRet.put("state", ReadyState.OPEN);
        this.notifyListeners("onReadyStateChanged", rsRet);
    }

    @Override
    public void onMessage(final String event, final MessageEvent message) throws Exception {
        final JSObject ret = new JSObject();
        ret.put("message", message.data);
        this.notifyListeners("onMessage", ret);
    }

    @Override
    public void onComment(final String comment) throws Exception {
        System.err.println("unhandled event: onComment: " + comment);
    }

    @Override
    public void onError(final Throwable t) {
        final JSObject ret = new JSObject();
        ret.put("error", t.getMessage());
        this.notifyListeners("onError", ret);
    }

    @Override
    public void onClosed(final boolean willReconnect) {
        final JSObject ret = new JSObject();
        ret.put("state", ReadyState.CLOSED);
        this.notifyListeners("onReadyStateChanged", ret);
    }
}

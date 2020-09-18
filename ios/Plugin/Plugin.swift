import Foundation
import Capacitor
import LDSwiftEventSource

#if !os(Linux)
import os.log
#endif

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(EventSource)
public class EventSource: CAPPlugin, LDSwiftEventSource.EventHandler {
    #if !os(Linux)
    private let logger: OSLog = OSLog(subsystem: "com.raccoonfink.capacitor.eventsource", category: "EventSource")
    #endif

    var eventSource: LDSwiftEventSource.EventSource?
    var url: String?
    var opened = false

    @objc func configure(_ call: CAPPluginCall) {
        guard let url = call.getString("url") else {
            call.reject("you must provide a URL")
            return
        }

        #if !os(Linux)
        os_log("configuring event source with URL %s", log: logger, type: .info, url)
        #endif

        guard let serverURL = URL.init(string: url) else {
            call.reject("invalid URL")
            return
        }
        self.url = url
        if self.eventSource != nil && opened {
            #if !os(Linux)
            os_log("configure() called: closing existing event source", log: logger, type: .error)
            #endif
            self.eventSource?.stop()
            self.opened = false
        }
        var config = LDSwiftEventSource.EventSource.Config(handler: self, url: serverURL)

        config.reconnectTime         = (call.getDouble("reconnectTime")         ?? 1000.0)  / 1000.0
        config.maxReconnectTime      = (call.getDouble("maxReconnectTime")      ?? 60000.0) / 1000.0
        config.backoffResetThreshold = (call.getDouble("backoffResetThreshold") ?? 5000.0)  / 1000.0
        config.idleTimeout           = (call.getDouble("idleTimeout")           ?? 30000.0) / 1000.0

        self.eventSource = LDSwiftEventSource.EventSource.init(config: config)
        call.resolve()
    }

    @objc func open(_ call: CAPPluginCall) {
        if self.eventSource == nil {
            call.reject("you must configure the event source first")
            return
        }

        #if !os(Linux)
        os_log("opening event source to %s", log: logger, type: .info, self.url ?? "unknown")
        #endif

        self.notifyListeners("readyStateChanged", data: [
            "state": ReadyState.connecting as Any
        ], retainUntilConsumed: true)

        self.opened = true
        self.eventSource?.start()
        call.resolve()
    }

    @objc func close(_ call: CAPPluginCall) {
        #if !os(Linux)
        os_log("closing event source connection to %s", log: logger, type: .info, self.url ?? "unknown")
        #endif

        if self.eventSource != nil {
            self.opened = false
            self.eventSource?.stop()
            self.eventSource = nil
        }
        call.resolve()
    }

    public func onOpened() {
        if !self.opened {
            #if !os(Linux)
            os_log("onOpened skipped (self.opened=false)", log: logger, type: .debug)
            #endif
            return
        }

        self.notifyListeners("readyStateChanged", data: [
            "state": ReadyState.open as Any
        ], retainUntilConsumed: true)
        self.notifyListeners("open", data: [String: Any](), retainUntilConsumed: true)
    }

    public func onClosed() {
        if !self.opened {
            #if !os(Linux)
            os_log("onClosed skipped (self.opened=false)", log: logger, type: .debug)
            #endif
            return
        }

        self.notifyListeners("readyStateChanged", data: [
            "state": ReadyState.closed as Any
        ], retainUntilConsumed: true)
    }

    public func onMessage(eventType: String, messageEvent: MessageEvent) {
        if !self.opened {
            #if !os(Linux)
            os_log("onMessage skipped (self.opened=false)", log: logger, type: .debug)
            #endif
            return
        }

        self.notifyListeners("message", data: [
            "type": eventType,
            "message": messageEvent.data as Any
        ], retainUntilConsumed: true)
    }

    public func onComment(comment: String) {
        #if !os(Linux)
        os_log("comment ignored: %s", log: logger, type: .debug, comment)
        #endif
    }

    public func onError(error: Error) {
        if !self.opened {
            #if !os(Linux)
            os_log("onError skipped (self.opened=false, error=%s)", log: logger, type: .debug, error.localizedDescription)
            #endif
            return
        }

        self.notifyListeners("error", data: [
            "error": error.localizedDescription as Any
        ], retainUntilConsumed: true)
    }

}

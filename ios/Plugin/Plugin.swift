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
        config.reconnectTime = 1.0
        config.maxReconnectTime = 60.0
        config.backoffResetThreshold = 10.0
        config.idleTimeout = 120.0

        self.eventSource = LDSwiftEventSource.EventSource.init(config: config)

        self.opened = true
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

        self.eventSource?.start()
        call.resolve()
    }

    @objc func close(_ call: CAPPluginCall) {
        #if !os(Linux)
        os_log("closing event source connection to %s", log: logger, type: .info, self.url ?? "unknown")
        #endif

        if self.eventSource != nil {
            self.eventSource?.stop()
            self.opened = false
            self.eventSource = nil
        }
        call.resolve()
    }

    public func onOpened() {
        self.notifyListeners("readyStateChanged", data: [
            "state": ReadyState.open as Any
        ], retainUntilConsumed: true)
        self.notifyListeners("open", data: [String: Any](), retainUntilConsumed: true)
    }

    public func onClosed() {
        self.notifyListeners("readyStateChanged", data: [
            "state": ReadyState.closed as Any
        ], retainUntilConsumed: true)
    }

    public func onMessage(eventType: String, messageEvent: MessageEvent) {
        self.notifyListeners("message", data: [
            "type": eventType,
            "message": messageEvent.data as Any
        ], retainUntilConsumed: true)
    }

    public func onComment(comment: String) {
        #if !os(Linux)
        os_log("comment received: %s", log: logger, type: .info, comment)
        #endif
    }

    public func onError(error: Error) {
        self.notifyListeners("error", data: [
            "error": error.localizedDescription as Any
        ], retainUntilConsumed: true)
    }

}

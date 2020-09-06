import Foundation
import Capacitor
import IKEventSource

#if !os(Linux)
import os.log
#endif

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(EventSource)
public class EventSource: CAPPlugin {
    #if !os(Linux)
    private let logger: OSLog = OSLog(subsystem: "com.raccoonfink.capacitor.eventsource", category: "EventSource")
    #endif

    var eventSource: IKEventSource.EventSource?
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
            self.eventSource?.disconnect()
            self.opened = false
        }
        self.eventSource = IKEventSource.EventSource.init(url: serverURL);

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
            "state": EventSourceState.connecting as Any
        ], retainUntilConsumed: true)

        self.eventSource?.onOpen({ [weak self] in
            self?.notifyListeners("open", data: [
                "value": true as Any
            ], retainUntilConsumed: true)
            self?.notifyListeners("readyStateChanged", data: [
                "state": EventSourceState.open as Any
            ], retainUntilConsumed: true)
        })
        self.eventSource?.onMessage({ [weak self] id, event, data in
            self?.notifyListeners("message", data: [
                "event": event as Any,
                "message": data as Any
            ], retainUntilConsumed: true)
        })
        self.eventSource?.onComplete({ [weak self] statusCode, reconnect, error in
            self?.notifyListeners("error", data: [
                "error": error?.localizedDescription as Any
            ], retainUntilConsumed: true)
            self?.notifyListeners("readyStateChanged", data: [
                "state": EventSourceState.closed as Any
            ], retainUntilConsumed: true)
        })

        self.eventSource?.connect()
        call.resolve()
    }

    @objc func close(_ call: CAPPluginCall) {
        #if !os(Linux)
        os_log("closing event source connection to %s", log: logger, type: .info, self.url ?? "unknown")
        #endif

        if self.eventSource != nil && self.opened {
            self.eventSource?.disconnect()
            self.opened = false
        }
        call.resolve()
    }
}

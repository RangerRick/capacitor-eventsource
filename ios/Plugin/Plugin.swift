import Foundation
import Capacitor
import DarklyEventSource

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

    var eventSource: LDEventSource?
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
            self.eventSource?.close()
            self.opened = false
        }
        self.eventSource = LDEventSource.init(url: serverURL, httpHeaders: [String: String]())

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

        self.eventSource?.onOpen({ (event: LDEvent?) in
            if event != nil {
                self.notifyListeners("open", data: [
                    "value": event?.data as Any
                ], retainUntilConsumed: true)
            }
        })
        self.eventSource?.onMessage({ (event: LDEvent?) in
            if event != nil {
                self.notifyListeners("message", data: [
                    "message": event?.data as Any
                ], retainUntilConsumed: true)
            }
        })
        self.eventSource?.onError({ (event: LDEvent?) in
            if event != nil {
                self.notifyListeners("error", data: [
                    "error": event?.data as Any
                ], retainUntilConsumed: true)
            }
        })
        self.eventSource?.onReadyStateChanged({ (event: LDEvent?) in
            if event != nil {
                self.notifyListeners("readyStateChanged", data: [
                    "state": event?.readyState as Any
                ], retainUntilConsumed: true)
            }
        })

        self.eventSource?.open()
        call.resolve()
    }

    @objc func close(_ call: CAPPluginCall) {
        #if !os(Linux)
        os_log("closing event source connection to %s", log: logger, type: .info, self.url ?? "unknown")
        #endif

        if self.eventSource != nil {
            self.eventSource?.close()
            self.opened = false
            self.eventSource = nil
        }
        call.resolve()
    }
}

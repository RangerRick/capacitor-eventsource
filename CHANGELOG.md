Changes
=======

0.1.6
-----
* Sigh, the launchdarkly one crashes.  :(

0.1.5
-----
* More fixes for potential race conditions.
* Fix a compilation/import thing on Android.

0.1.4
-----

* Dependency updates.
* Properly export the new configuration parameters

0.1.3
-----

* Update Android implementation to use launchdarkly (haha I know)
* Pass parameters for configuring retries/timeout/etc

0.1.2
-----

* Update Android implementation to use okhttp-sse
* Skip updates that come through after shutting down, to avoid
  a potential crash in notifying listeners

0.1.1
-----

* Dependency updates.

0.1.0
-----

* More iOS switching! Now using LDSwiftEventSource ;)

0.0.9
-----

* Fix typo in Android build.

0.0.7
-----

* Add typescript definition for `removeAllListeners()`

0.0.6
-----

* Always close, just in case.

0.0.5
-----

* Put it back.  :)

0.0.3/0.0.4
-----------

* Switch to IKEventSource on iOS.

0.0.2
-----

* Fix worker threads not communicating properly on Android.

0.0.1
-----

* Initial implementation.

# Native SSE (EventSource) Plugin for Capacitor

Because of the way Capacitor plugins are designed, it does not exactly follow the format/signature of the "real" browser `EventSource` API, but it works similarly.

## Installation

```shell
npm install capacitor-eventsource
npx cap sync
```

On iOS or for pure-web use, no further steps are needed.

### Android Installation

On Android, a few steps are involved:

First, set up your Gradle build to find dependencies from Jitpack.
Edit your `android/build.gradle` file and add the following to the `repositories:` section inside `allProjects` (under `jcenter()`):

```gradle
maven { url 'https://jitpack.io' }
```

Next, update your main activity to pull in the `EventSource` plugin:

```java
import com.raccoonfink.plugins.eventsource.EventSource;

public class MainActivity extends BridgeActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Initializes the Bridge
    this.init(savedInstanceState, new ArrayList<Class<? extends Plugin>>() {{
      // Additional plugins you've installed go here
      // Ex: add(TotallyAwesomePlugin.class);
      add(EventSource.class);
    }});
  }
}
```

## Usage

```javascript
EventSource.configure({ url: 'https://my-sse-url/' });
EventSource.addListener('open', (openResult) => {
  console.debug('Connection opened.');
});
EventSource.addListener('message', (messageResult) => {
  console.info('I got a message: ' + messageResult.message);
});
EventSource.addListener('error', (errorResult) => {
  console.error('Uh-oh: ' + errorResult.error);
});
EventSource.addListener('readyStateChanged', (readyStateChangedResult) => {
  console.debug('Ready state changed: ' + readyStateChangedResult.state);
});
```

# Network Connection Class

Network Connection Class is an Android library that allows you to figure out 
the quality of the current user's internet connection.  The connection gets 
classified into several "Connection Classes" that make it easy to develop 
against.  The library does this by listening to the existing internet traffic 
done by your app and notifying you when the user's connection quality changes.  
Developers can then use this Connection Class information and adjust the application's 
behaviour (request lower quality images or video, throttle type-ahead, etc).

## Integration

### Download
Download [the latest JARs](https://github.com/facebook/network-connection-class/releases/latest) or grab via Gradle:
```groovy
compile 'com.facebook.network.connectionclass:connectionclass:1.0.0'
```
or Maven:
```xml
<dependency>
  <groupId>com.facebook.network.connectionclass</groupId>
  <artifactId>connectionclass</artifactId>
  <version>1.0.0</version>
</dependency>
```

### Calculate Connection Class
Connection Class provides an interface for classes to add themselves as
listeners for when the network's connection quality changes. In the subscriber
class, implement `ConnectionClassStateChangeListener`:

```java
public interface ConnectionClassStateChangeListener {
  public void onBandwidthStateChange(ConnectionQuality bandwidthState);
}
```

and subscribe with the listener:

```java
ConnectionClassManager.getInstance().register(mListener);
```

Alternatively, you can manually query for the connection quality bucket with
`getCurrentBandwidthQuality()`.

```java
ConnectionQuality cq = ConnectionClassManager.getInstance().getCurrentBandwidthQuality();
```

The main way to provide the ConnectionClassManager data is to use the DownloadBandwidthSampler.
The DownloadBandwidthSampler samples the device's underlying network stats when you tell it
you're performing some sort of network activity (downloading photos, playing a video, etc).  

```java
// Override ConnectionClassStateChangeListener
ConnectionClassManager.getInstance().register(mListener);
DownloadBandwidthSampler.getInstance().startSampling();
// Do some downloading tasks
DownloadBandwidthSampler.getInstance().stopSampling();
```

If the application is aware of the bandwidth downloaded in a certain time frame,
data can be added to the moving average using:

```java
ConnectionClassManager.addBandwidth(bandwidth, time);
```

See the `connectionclass-sample` project for more details.

## Improve Connection Class!
See the CONTRIBUTING.md file for how to help out.

## License
Connection Class is BSD-licensed. We also provide an additional patent grant.

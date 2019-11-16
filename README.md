<p align="center">
<img width="200" src="https://corp.map.ir/wp-content/uploads/2019/06/map-site-logo-1.png" alt="Map.ir Logo">
</p>

<p align="center">
 <a href="https://bintray.com/shivehmapco/Tracker/ir.map.tracker/_latestVersion">
      <img src="https://api.bintray.com/packages/shivehmapco/Tracker/ir.map.tracker/images/download.svg" alt="Download">
   </a>
 <a href="#">
      <img src="https://img.shields.io/badge/API-14%2B-brightgreen.svg?style=flat" alt="Download">
 </a>
</p>


# MapirLiveTracker

<p align="center">

</p>

## Features

- Map.ir Live Tracker uses MQTT protocol which has low data usage.
- Easy configuration.
- Complete and expressive documentation.
- You can use both Java and Kotlin languages. 

## Example

The example applications are the best way to see `MapirLiveTracker` in action. Simply clone and open app in Android Studio.

## Prerequisites

Create an account in map.ir then create a project and get your API_KEY :
https://corp.map.ir/registration/

## Installation

Add this line in dependencies scope in your project's app.gradle :

```bash
implementation 'ir.map.tracker:tracker:$latest_version'
```

## Usage

### Publisher
#### 1.Put Following permissions in Manifest file :

```
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" /> // For receive location updates
    <uses-permission android:name="android.permission.READ_PHONE_STATE" /> // For read device imei
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" /> // For making LiveService run in forground if you want use SDK's service
```
#### 2.Declare LiveService in Manifest (if you want use SDK's LiveService) in application scope :
```
<service
            android:name="ir.map.tracker.PublisherService"
            android:enabled="true" />
```
#### 3.Create publisher object and use static method getLiveTracker to initialize object to work with it :
```
Publisher publisher = Publisher.getLiveTracker(context, API_KEY, track_id, true, new TrackerEvent.PublishListener() {
                @Override
                public void publishedLocation(Location location) {
                    // Each published location
                }
                
                @Override
                public void onFailure(PublisherError error) {
                    switch (code) {
                        case LOCATION_PERMISSION:
                        
                            // Missing Location permission
                            break;
                        case TELEPHONY_PERMISSION:
                        
                            // Missing Telephony permission
                            break;
                        case INITIALIZE_ERROR:
                        
                            // Something went wrong during sdk initilization
                            break;
                        case MISSING_API_KEY:
                        
                            // Provided access token is missing or wrong
                            break;
                        case MISSING_TRACK_ID:
                        
                            // Missing track_id
                            break;    
                        case MISSING_CONTEXT:
                        
                            // Missing context
                            break;     
                    }
                }

                @Override
                public void onLiveTrackerDisconnected() {
                    // Tracker disconnected
                }
            });
            
```
#### 3.Activity/Fragment Life Cycle methods
##### OnResume
```
    @Override
    protected void onResume() {
        if (publisher != null)
            publisher.onResume();
        super.onResume();
    }
```
##### OnPause
```
    @Override
    protected void onPause() {
        if (publisher != null)
            publisher.onPause();
        super.onPause();
    }

```

#### 4.Start sending location : 
```
publisher.start(interval); // interval is in miliseconds and should be at least 1000
```
#### 5.Stop sending location :
```
publisher.stop();
```
### Subscriber
#### 1.Put Following permissions in Manifest file :

```
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.READ_PHONE_STATE" /> // For read device imei
```
#### 2.Create subscriber object and use static method getLiveTracker to initialize object to work with it :

```
Subscriber subscriber = Subscriber.getLiveTracker(context, API_KEY, track_id, new TrackerEvent.SubscribeListener() {
                @Override
                public void onLocationReceived(Location location) {
                    // Recieved location
                }
                
                @Override
                public void onFailure(SubscriberError error) {
                    switch (code) {
                        case TELEPHONY_PERMISSION:
                        
                            // Missing Telephony permission
                            break;
                        case INITIALIZE_ERROR:
                        
                            // Something went wrong during sdk initilization
                            break;
                        case MISSING_API_KEY:
                        
                            // Provided access token is missing or wrong
                            break;
                        case MISSING_TRACK_ID:
                        
                            // Missing track_id
                            break;    
                        case MISSING_CONTEXT:
                        
                            // Missing context
                            break;  
                    }
                }

                @Override
                public void onLiveTrackerDisconnected() {
                    Toast.makeText(MainActivity.this, "onLiveTrackerDisconnected", Toast.LENGTH_SHORT).show();
                }
            });
            
```

#### 3.Start receiving location :
```
subscriber.start();
```
#### 4.Stop receiving location :
```
subscriber.stop();
```
## Example
Clone project and run subscriber_sample or puvlisher_sample modules seperatly
Put your API_KEY in project whenever it needs
## Contributing
Contributions are very welcome 🙌

## License
License is available in LICENSE file.

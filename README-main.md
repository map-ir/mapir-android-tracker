<p align="center">
<img width="200" src="https://corp.map.ir/wp-content/uploads/2019/06/map-site-logo-1.png" alt="Map.ir Logo">
</p>

 [ ![Download](https://api.bintray.com/packages/shivehmapco/Tracker/ir.map.tracker/images/download.svg) ](https://bintray.com/shivehmapco/Tracker/ir.map.tracker/_latestVersion)  [![API](https://img.shields.io/badge/API-14%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=14)



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

## Installation

Add this line in dependencies scope in your project's app.gradle :

```bash
implementation 'ir.map.tracker:tracker:$latest_version'
```

## Usage

### Publisher
1.Create publisher object and use static method getLiveTracker to initialize object to work with it :

```
Publisher publisher = Publisher.getLiveTracker(context, API_KEY, track_id, true, new TrackerEvent.PublishListener() {
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
                        case ACCESS_TOKEN_NOT_AVAILABLE:
                        
                            // Provided access token is missing or wrong
                            break;
                    }
                }

                @Override
                public void onLiveTrackerDisconnected() {
                    // Tracker disconnected
                }

                @Override
                public void publishedLocation(Location location) {
                    // Each published location
                }
            });
            
 publisher.start(INTERVAL); // INTERVAL is in miliseconds and should be at least 1000
```

### Subscriber
1.Create subscriber object and use static method getLiveTracker to initialize object to work with it :

```
Publisher publisher = Publisher.getLiveTracker(context, API_KEY, track_id, true, new TrackerEvent.PublishListener() {
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
                        case ACCESS_TOKEN_NOT_AVAILABLE:
                        
                            // Provided access token is missing or wrong
                            break;
                    }
                }

                @Override
                public void onLiveTrackerDisconnected() {
                    // Tracker disconnected
                }

                @Override
                public void publishedLocation(Location location) {
                    // Each published location
                }
            });
            
 publisher.start(INTERVAL); // INTERVAL is in miliseconds and should be at least 1000
```
## Contributing
Contributions are very welcome 🙌

## License
License is available in LICENSE file.

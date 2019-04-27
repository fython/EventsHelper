EventsHelper
----

# Import

![image](https://api.bintray.com/packages/fython/EventsHelper/events-library-core/images/download.svg)

```groovy
allprojects {
    repositories {
        // ...
        maven { url 'https://dl.bintray.com/fython/EventsHelper' }
    }
}

dependencies {
    implementation 'moe.feng.common.eventshelper:event-library-core:latest-version'
    annotationProcessor 'moe.feng.common.eventshelper:event-compiler:latest-version'
    
    // For Kotlin developers, please use 'kapt' instead of 'annotationProcessor'
    // kapt 'moe.feng.common.eventshelper:event-compiler:latest-version'
}
```

# How to use?

See `app` module.
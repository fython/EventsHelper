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
    implementation 'moe.feng.common.eventshelper:events-library-core:1.0.0-alpha3'
    annotationProcessor 'moe.feng.common.eventshelper:events-compiler:1.0.0-alpha3'
    
    // For Kotlin developers, please use 'kapt' instead of 'annotationProcessor'
    // kapt 'moe.feng.common.eventshelper:events-compiler:1.0.0-alpha3'
    // implementation 'moe.feng.common.eventshelper:events-library-ktx:1.0.0-alpha3'
}
```

# How to use?

See `app` module.
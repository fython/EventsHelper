EventsHelper
====

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
    implementation 'moe.feng.common.eventshelper:events-library-core:1.0.2'
    annotationProcessor 'moe.feng.common.eventshelper:events-compiler:1.0.2'
    
    // For Kotlin developers, please use 'kapt' instead of 'annotationProcessor'
    // kapt 'moe.feng.common.eventshelper:events-compiler:1.0.2'
    // implementation 'moe.feng.common.eventshelper:events-library-ktx:1.0.2'
}
```

# How to use?

## Create a listener and implement it

Create an interface class annotated with `@EventsListener`:

```java
@EventsListener
public interface MyListener {
    
    @EventsOnThread(EventsOnThread.MAIN_THREAD) // Schedule method calls on main thread
    void onStatusUpdate(int status);
    
}
```

Implement this interface according to your own needs:

```java
public class MyActivity extends Activity implements MyListener {
    
    ...
    
    @Override
    public void onStatusUpdate(int status) {
        Log.d(TAG, "onStatusUpdate: status = " + status);
    }
    
}
```

## Register/Unregister listener

Register/Unregister listener at a proper time of components lifecycle:

```java
public class MyActivity extends Activity implements MyListener {
    
    ...
    
    @Override
    protected void onStart() {
        super.onStart();
        EventsHelper.getInstance(this /* extends Context */)
                .registerListener(this /* implements MyListener */);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        EventsHelper.getInstance(this /* extends Context */)
                .unregisterListener(this /* implements MyListener */);
    }
    
}
```

## Call listeners by EventsHelper

`EventsHelper` can help you call methods on all specific listener type cross over components.

You can use this way to get a listener wrapper and call it:

```java
MyListener listener = EventsHelper.getInstance(context).of(MyListener.class);
listener.onStatusUpdate(1000);
```

## Advanced usage

### Schedule method calls on threads

Annotating methods with `@EventsOnThread(int type)` can tell `EventsHelper` to schedule calls 
on specific threads.

Supported threads type:

- EventsOnThread.CURRENT_THREAD
- EventsOnThread.MAIN_THREAD
- EventsOnThread.NEW_THREAD

## License

```
MIT License

Copyright (c) 2019-2020 Siubeng Fung (fython)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

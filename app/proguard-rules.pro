# -keepnames class moe.feng.common.eventshelper.*

-keep @moe.feng.common.eventshelper.EventsListener class *
-keepclassmembers class * {
    @moe.feng.common.eventshelper.EventsListener *;
}

-keep class moe.feng.common.eventshelper.** { *; }
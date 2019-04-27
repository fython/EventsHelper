package moe.feng.common.eventshelper.sample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import moe.feng.common.eventshelper.EventsHelper;

public class SampleActivity extends Activity {

    private static final String TAG = SampleActivity.class.getSimpleName();

    private TestListener mListenerA = (time) -> Log.d(TAG, "ListenerA: " + time + " thread: " + Thread.currentThread());
    private TestAsyncListener mListenerB = (time) -> Log.d(TAG, "ListenerB: " + time + " thread: " + Thread.currentThread());
    private TestAsyncListener mListenerC = (time) -> Log.d(TAG, "ListenerC: " + time + " thread: " + Thread.currentThread());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        EventsHelper.init(this);

        findViewById(R.id.send_event_button).setOnClickListener(v -> {
            Log.d(TAG, "Now calling A TestListener#onButtonClick");
            EventsHelper.of(TestListener.class, "A").onButtonClick(System.currentTimeMillis());
            Log.d(TAG, "Now calling B TestListener#onButtonClick");
            EventsHelper.of(TestAsyncListener.class, "B").onButtonClick(System.currentTimeMillis());
            Log.d(TAG, "Now calling C TestListener#onButtonClick");
            EventsHelper.of(TestAsyncListener.class, "C").onButtonClick(System.currentTimeMillis());
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventsHelper.registerListener(mListenerA, "A");
        EventsHelper.registerListener(mListenerB, "B");
        EventsHelper.registerListener(mListenerC, "C");
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventsHelper.unregisterListeners(mListenerA, mListenerB, mListenerC);
    }

}

package moe.feng.common.eventshelper;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

public class SampleActivity extends Activity {

    private static final String TAG = SampleActivity.class.getSimpleName();

    private TestListener mListenerA = (time) -> Log.d(TAG, "ListenerA: " + time);
    private TestListener mListenerB = (time) -> Log.d(TAG, "ListenerB: " + time);
    private TestListener mListenerC = (time) -> Log.d(TAG, "ListenerC: " + time);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        findViewById(R.id.send_event_button).setOnClickListener(v -> {
            Log.d(TAG, "Now calling all TestListener#onButtonClick");
            EventsHelper.of(TestListener.class).onButtonClick(System.currentTimeMillis());
            Log.d(TAG, "Now calling A TestListener#onButtonClick");
            EventsHelper.of(TestListener.class, "A").onButtonClick(System.currentTimeMillis());
            Log.d(TAG, "Now calling B TestListener#onButtonClick");
            EventsHelper.of(TestListener.class, "B").onButtonClick(System.currentTimeMillis());
            Log.d(TAG, "Now calling C TestListener#onButtonClick");
            EventsHelper.of(TestListener.class, "C").onButtonClick(System.currentTimeMillis());
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
        EventsHelper.unregisterListener(mListenerA);
        EventsHelper.unregisterListener(mListenerB);
        EventsHelper.unregisterListener(mListenerC);
    }

}

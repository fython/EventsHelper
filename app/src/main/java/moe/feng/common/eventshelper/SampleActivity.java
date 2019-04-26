package moe.feng.common.eventshelper;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class SampleActivity extends Activity implements TestListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        findViewById(R.id.send_event_button).setOnClickListener(v -> {
            EventsHelper.of(TestListener.class).onButtonClick(System.currentTimeMillis());
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventsHelper.registerListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventsHelper.unregisterListener(this);
    }

    @Override
    public void onButtonClick(long time) {
        Toast.makeText(this, "Time: " + time, Toast.LENGTH_SHORT).show();
    }

}

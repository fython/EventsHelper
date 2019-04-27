package moe.feng.common.eventshelper.sample;

import moe.feng.common.eventshelper.EventsListener;
import moe.feng.common.eventshelper.EventsOnThread;

@EventsListener
public interface TestAsyncListener {

    @EventsOnThread(EventsOnThread.NEW_THREAD)
    void onButtonClick(long time);

}

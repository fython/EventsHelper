package moe.feng.common.eventshelper.sample;

import moe.feng.common.eventshelper.EventsListener;
import moe.feng.common.eventshelper.EventsOnThread;
import moe.feng.common.eventshelper.Ignore;

@EventsListener
public interface TestListener {

    @EventsOnThread(EventsOnThread.MAIN_THREAD)
    void onButtonClick(long time);

    @Ignore
    void ignoredMethod();

}

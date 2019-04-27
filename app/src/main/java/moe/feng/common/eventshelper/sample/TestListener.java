package moe.feng.common.eventshelper.sample;

import moe.feng.common.eventshelper.EventsListener;

@EventsListener
public interface TestListener {

    void onButtonClick(long time);

}

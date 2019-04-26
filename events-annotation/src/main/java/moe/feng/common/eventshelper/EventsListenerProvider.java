package moe.feng.common.eventshelper;

import java.util.List;

public interface EventsListenerProvider {

    <T> List<T> getListenersByClass(Class<T> listenerClass);

}

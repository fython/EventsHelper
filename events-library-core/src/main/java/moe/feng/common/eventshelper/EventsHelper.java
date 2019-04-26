package moe.feng.common.eventshelper;

import androidx.annotation.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EventsHelper {

    private static final List<Object> mListeners = new ArrayList<>();

    private static final Map<String, Object> mHelperCache = new HashMap<>();

    private static final EventsListenerProvider sListenerProvider = EventsHelper::getListenersByClass;

    private static final boolean sUseProxyInterface = false;

    private static <T> List<T> getListenersByClass(Class<T> listenerClass) {
        List<T> list = new ArrayList<>();
        for (Object listener : mListeners) {
            if (listenerClass.isInstance(listener)) {
                list.add((T) listener);
            }
        }
        return list;
    }

    public static EventsListenerProvider getEventsListenerProvider() {
        return sListenerProvider;
    }

    public static void registerListener(@NonNull Object listener) {
        mListeners.add(listener);
    }

    public static void unregisterListener(@NonNull Object listener) {
        mListeners.remove(listener);
    }

    public static <T> T of(final Class<T> listenerClass) {
        validateListenerInterface(listenerClass);

        if (sUseProxyInterface) {
            return proxyOf(listenerClass);
        } else {
            String helperClassName = listenerClass.getCanonicalName() + "$$Helper";
            if (mHelperCache.containsKey(helperClassName)) {
                return (T) mHelperCache.get(helperClassName);
            } else {
                try {
                    Class helperClass = Class.forName(helperClassName);
                    Constructor<T> constructor = helperClass.getDeclaredConstructor(
                            EventsListenerProvider.class);
                    T instance = constructor.newInstance(getEventsListenerProvider());
                    mHelperCache.put(helperClassName, instance);
                    return instance;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static <T> T proxyOf(final Class<T> listenerClass) {
        return (T) Proxy.newProxyInstance(listenerClass.getClassLoader(),
                new Class<?>[]{ listenerClass },
                new ListenerInvocationHandler(listenerClass));
    }

    private static <T> void validateListenerInterface(Class<T> listenerClass) {
        if (!listenerClass.isInterface()) {
            throw new IllegalArgumentException("API declarations must be interfaces.");
        }
    }

    private static class ListenerInvocationHandler implements InvocationHandler {

        private Class<?> listenerClass;

        ListenerInvocationHandler(Class<?> listenerClass) {
            this.listenerClass = listenerClass;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            for (Object listener : getListenersByClass(listenerClass)) {
                method.invoke(listener, args);
            }
            return null;
        }

    }

}

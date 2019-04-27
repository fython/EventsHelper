package moe.feng.common.eventshelper;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class EventsHelper {

    private static final HashMap<Object, String> mListeners = new HashMap<>();

    private static final Map<Pair<String, String>, Object> mHelperCache = new HashMap<>();

    private static final EventsListenerProvider sListenerProvider = new EventsListenerProvider() {
        @Override
        public <T> List<T> getListenersByClass(Class<T> listenerClass) {
            return EventsHelper.getListenersByClass(listenerClass);
        }

        @Override
        public <T> List<T> getListenersByClass(Class<T> listenerClass, String tag) {
            return EventsHelper.getListenersByClass(listenerClass, tag);
        }
    };

    private static final boolean sUseProxyInterface = false;

    private static <T> List<T> getListenersByClass(Class<T> listenerClass) {
        List<T> list = new ArrayList<>();
        for (Object listener : mListeners.keySet()) {
            if (listenerClass.isInstance(listener)) {
                list.add((T) listener);
            }
        }
        return list;
    }

    private static <T> List<T> getListenersByClass(Class<T> listenerClass, String tag) {
        List<T> list = new ArrayList<>();
        for (Map.Entry<Object, String> entry : mListeners.entrySet()) {
            if (listenerClass.isInstance(entry.getKey())) {
                if (tag != null && !Objects.equals(tag, entry.getValue())) {
                    continue;
                }
                list.add((T) entry.getKey());
            }
        }
        return list;
    }

    public static EventsListenerProvider getEventsListenerProvider() {
        return sListenerProvider;
    }

    public static void registerListener(@NonNull Object listener) {
        mListeners.put(listener, null);
    }

    public static void registerListener(@NonNull Object listener, @Nullable String tag) {
        mListeners.put(listener, tag);
    }

    public static void unregisterListener(@NonNull Object listener) {
        mListeners.remove(listener);
    }

    public static <T> T of(@NonNull Class<T> listenerClass) {
        return of(listenerClass, null);
    }

    public static <T> T of(@NonNull Class<T> listenerClass, @Nullable String tag) {
        validateListenerInterface(listenerClass);

        if (sUseProxyInterface) {
            return proxyOf(listenerClass, tag);
        } else {
            String helperClassName = listenerClass.getCanonicalName() + "$$Helper";
            Pair<String, String> key = Pair.create(helperClassName, tag);
            if (mHelperCache.containsKey(key)) {
                return (T) mHelperCache.get(helperClassName);
            } else {
                try {
                    Class helperClass = Class.forName(helperClassName);
                    Constructor<T> constructor = helperClass.getDeclaredConstructor(
                            EventsListenerProvider.class, String.class);
                    T instance = constructor.newInstance(getEventsListenerProvider(), tag);
                    mHelperCache.put(key, instance);
                    return instance;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static <T> T proxyOf(Class<T> listenerClass, String tag) {
        return (T) Proxy.newProxyInstance(listenerClass.getClassLoader(),
                new Class<?>[]{ listenerClass },
                new ListenerInvocationHandler(listenerClass, tag));
    }

    private static <T> void validateListenerInterface(Class<T> listenerClass) {
        if (!listenerClass.isInterface()) {
            throw new IllegalArgumentException("API declarations must be interfaces.");
        }
    }

    private static class ListenerInvocationHandler implements InvocationHandler {

        private Class<?> listenerClass;
        private String tag;

        ListenerInvocationHandler(Class<?> listenerClass, String tag) {
            this.listenerClass = listenerClass;
            this.tag = tag;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            for (Object listener : getListenersByClass(listenerClass, tag)) {
                method.invoke(listener, args);
            }
            return null;
        }

    }

}

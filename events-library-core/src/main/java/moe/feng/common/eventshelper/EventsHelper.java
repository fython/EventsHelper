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

import static java.util.Objects.requireNonNull;

public final class EventsHelper {

    private static final String PACKAGE_NAME = requireNonNull(EventsHelper.class.getPackage()).getName();

    private static final HashMap<Object, String> mListeners = new HashMap<>();

    private static final Map<Pair<String, String>, Object> mHelperCache = new HashMap<>();

    private static final boolean sUseProxyInterface = false;

    static <T> List<T> getListenersByClass(Class<T> listenerClass) {
        List<T> list = new ArrayList<>();
        for (Object listener : mListeners.keySet()) {
            if (listenerClass.isInstance(listener)) {
                list.add((T) listener);
            }
        }
        return list;
    }

    static <T> List<T> getListenersByClass(Class<T> listenerClass, String tag) {
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

    public static void registerListener(@NonNull Object listener) {
        requireNonNull(listener, "Listener argument cannot be null.");
        mListeners.put(listener, null);
    }

    public static void registerListeners(@NonNull Object... listeners) {
        for (Object listener : listeners) {
            registerListener(listener);
        }
    }

    public static void registerListener(@NonNull Object listener, @Nullable String tag) {
        requireNonNull(listener, "Listener argument cannot be null.");
        mListeners.put(listener, tag);
    }

    public static void unregisterListener(@NonNull Object listener) {
        requireNonNull(listener, "Listener argument cannot be null.");
        mListeners.remove(listener);
    }

    public static void unregisterListeners(@NonNull Object... listeners) {
        for (Object listener : listeners) {
            mListeners.remove(listener);
        }
    }

    public static void clearAllListeners() {
        mListeners.clear();
    }

    public static <T> T of(@NonNull Class<T> listenerClass) {
        return of(listenerClass, null);
    }

    public static <T> T of(@NonNull Class<T> listenerClass, @Nullable String tag) {
        validateListenerInterface(listenerClass);

        if (sUseProxyInterface) {
            return proxyOf(listenerClass, tag);
        } else {
            String listenerClassName = listenerClass.getCanonicalName();
            Pair<String, String> key = Pair.create(listenerClassName, tag);
            if (mHelperCache.containsKey(key)) {
                return (T) mHelperCache.get(key);
            } else {
                try {
                    String helperClassName = PACKAGE_NAME + ".Helper$$"
                            + listenerClassName.replace(".", "_");
                    Class helperClass = Class.forName(helperClassName);
                    Constructor<T> constructor = helperClass.getDeclaredConstructor(String.class);
                    T instance = constructor.newInstance(tag);
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

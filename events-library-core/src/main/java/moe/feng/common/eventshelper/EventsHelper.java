package moe.feng.common.eventshelper;

import android.content.Context;
import android.os.Handler;
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

    private static final String TAG = EventsHelper.class.getSimpleName();
    private static final String PACKAGE_NAME = requireNonNull(EventsHelper.class.getPackage()).getName();

    private static final HashMap<Object, String> sListeners = new HashMap<>();

    private static final Map<Pair<String, String>, Object> sHelperCache = new HashMap<>();

    private static Handler sMainHandler;

    private static final boolean sUseProxyInterface = false;

    public static void init(@NonNull Context context) {
        requireNonNull(context);
        if (sMainHandler == null) {
            sMainHandler = new Handler(context.getMainLooper());
        }
    }

    public static void registerListener(@NonNull Object listener) {
        registerListener(listener, null);
    }

    public static void registerListeners(@NonNull Object... listeners) {
        requireNonNull(listeners, "Listeners argument cannot be null.");
        for (Object listener : listeners) {
            registerListener(listener);
        }
    }

    public static void registerListener(@NonNull Object listener, @Nullable String tag) {
        requireNonNull(listener, "Listener argument cannot be null.");
        validateListenerInstance(listener);
        sListeners.put(listener, tag);
    }

    public static void unregisterListener(@NonNull Object listener) {
        requireNonNull(listener, "Listener argument cannot be null.");
        sListeners.remove(listener);
    }

    public static void unregisterListeners(@NonNull Object... listeners) {
        requireNonNull(listeners, "Listeners argument cannot be null.");
        for (Object listener : listeners) {
            sListeners.remove(listener);
        }
    }

    public static void clearAllListeners() {
        sListeners.clear();
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
            if (sHelperCache.containsKey(key)) {
                return (T) sHelperCache.get(key);
            } else {
                try {
                    String helperClassName = PACKAGE_NAME + ".Helper$$"
                            + listenerClassName.replace(".", "_");
                    Class helperClass = Class.forName(helperClassName);
                    Constructor<T> constructor = helperClass.getDeclaredConstructor(String.class);
                    T instance = constructor.newInstance(tag);
                    sHelperCache.put(key, instance);
                    return instance;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    static <T> List<T> getListenersByClass(Class<T> listenerClass) {
        return getListenersByClass(listenerClass, null);
    }

    static <T> List<T> getListenersByClass(Class<T> listenerClass, String tag) {
        List<T> list = new ArrayList<>();
        for (Map.Entry<Object, String> entry : sListeners.entrySet()) {
            if (listenerClass.isInstance(entry.getKey())) {
                if (tag != null && !Objects.equals(tag, entry.getValue())) {
                    continue;
                }
                list.add((T) entry.getKey());
            }
        }
        return list;
    }

    static void scheduleRunnable(Runnable runnable, @EventsOnThread.ThreadType int threadType) {
        if (threadType == EventsOnThread.CURRENT_THREAD) {
            runnable.run();
        } else if (threadType == EventsOnThread.NEW_THREAD) {
            new Thread(runnable).start();
        } else if (threadType == EventsOnThread.MAIN_THREAD) {
            requireNonNull(sMainHandler, "You should call EventsHelper#init(Context) " +
                    "before calling methods on main thread");
            sMainHandler.post(runnable);
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

        if (listenerClass.getAnnotation(EventsListener.class) == null) {
            throw new IllegalArgumentException("Objects registering should be " +
                    "annotated with @EventsListener");
        }
    }

    private static <T> void validateListenerInstance(T listener) {
        Class<?>[] interfaces = listener.getClass().getInterfaces();
        boolean implementedEventsListener = false;
        for (Class<?> interfaceClass : interfaces) {
            implementedEventsListener = interfaceClass.getAnnotation(EventsListener.class) != null;
            if (implementedEventsListener) {
                break;
            }
        }
        if (!implementedEventsListener) {
            throw new IllegalArgumentException(
                    "This listener isn't annotated with EventsListener.");
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
                EventsOnThread annotation = method.getAnnotation(EventsOnThread.class);
                int threadType = EventsOnThread.CURRENT_THREAD;
                if (annotation != null) {
                    threadType = annotation.value();
                }
                EventsHelper.scheduleRunnable(() -> {
                    try {
                        method.invoke(listener, args);
                    } catch (Exception e) {
                        throw new RuntimeException();
                    }
                }, threadType);
            }
            return null;
        }

    }

}

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

/**
 * @author Fung Gwo (fythonx@gmail.com)
 */
public final class EventsHelper {

    private static final String TAG = EventsHelper.class.getSimpleName();
    private static final String PACKAGE_NAME = requireNonNull(EventsHelper.class.getPackage()).getName();

    private static final boolean sUseProxyInterface = false;

    @Nullable
    private static EventsHelper sInstance = null;
    private static final Object sLock = new Object();

    private final HashMap<Object, String> mListeners = new HashMap<>();

    private final Map<Pair<String, String>, Object> mHelperCache = new HashMap<>();

    private final Handler mMainHandler;

    /**
     * Get an instance of EventsHelper by context
     *
     * @param context Context
     * @return an instance of EventsHelper
     */
    @NonNull
    public static EventsHelper getInstance(@NonNull Context context) {
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new EventsHelper(context);
            }
            return sInstance;
        }
    }

    /**
     * Get an existing instance of EventsHelper.
     * If you didn't call {@link EventsHelper#getInstance(Context)} before, it will throw a NPE.
     *
     * @return an instance of EventsHelper
     */
    @NonNull
    public static EventsHelper getInstance() {
        synchronized (sLock) {
            if (sInstance == null) {
                throw new NullPointerException("EventsHelper instance hasn't been initialized.");
            }
            return sInstance;
        }
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

    private EventsHelper(@NonNull Context context) {
        requireNonNull(context);
        mMainHandler = new Handler(context.getMainLooper());
    }

    /**
     * Register events listener
     *
     * @param listener Object implemented a interface annotated with {@link EventsListener}
     */
    public void registerListener(@NonNull Object listener) {
        registerListener(listener, null);
    }


    /**
     * Register list of events listeners
     *
     * @param listeners Objects implemented a interface annotated with {@link EventsListener}
     */
    public void registerListeners(@NonNull Object... listeners) {
        requireNonNull(listeners, "Listeners argument cannot be null.");
        for (Object listener : listeners) {
            registerListener(listener);
        }
    }


    /**
     * Register events listener with a tag
     *
     * @param listener Object implemented a interface annotated with {@link EventsListener}
     * @param tag A tag name of object
     */
    public void registerListener(@NonNull Object listener, @Nullable String tag) {
        requireNonNull(listener, "Listener argument cannot be null.");
        validateListenerInstance(listener);
        mListeners.put(listener, tag);
    }

    /**
     * Unregister events listener
     *
     * @param listener Object to unregister
     */
    public void unregisterListener(@NonNull Object listener) {
        requireNonNull(listener, "Listener argument cannot be null.");
        mListeners.remove(listener);
    }

    /**
     * Unregister list of events listeners
     *
     * @param listeners Objects to unregister
     */
    public void unregisterListeners(@NonNull Object... listeners) {
        requireNonNull(listeners, "Listeners argument cannot be null.");
        for (Object listener : listeners) {
            mListeners.remove(listener);
        }
    }

    /**
     * Clear all events listeners
     */
    public void clearAllListeners() {
        mListeners.clear();
    }

    /**
     * Get events helper of specific listener type. Calling methods in helper will schedule
     * calls to all specific listeners.
     *
     * @param listenerClass The class of specific listener type
     * @param <T> Listener type
     * @return Events helper
     */
    public <T> T of(@NonNull Class<T> listenerClass) {
        return of(listenerClass, null);
    }


    /**
     * Get events helper of specific listener type and tag.
     *
     * @param listenerClass The class of specific listener type
     * @param tag Specific tag name
     * @param <T> Listener type
     * @return Events helper
     * @see EventsHelper#of(Class)
     */
    public <T> T of(@NonNull Class<T> listenerClass, @Nullable String tag) {
        validateListenerInterface(listenerClass);

        if (sUseProxyInterface) {
            return proxyOf(listenerClass, tag);
        } else {
            String listenerClassName = requireNonNull(listenerClass.getCanonicalName());
            Pair<String, String> key = Pair.create(listenerClassName, tag);
            if (mHelperCache.containsKey(key)) {
                return (T) mHelperCache.get(key);
            } else {
                try {
                    String helperClassName = PACKAGE_NAME + ".Helper$$"
                            + listenerClassName.replace(".", "_");
                    Class helperClass = Class.forName(helperClassName);
                    Constructor<T> constructor = helperClass.getDeclaredConstructor(
                            String.class, EventsHelper.class);
                    T instance = constructor.newInstance(tag, this);
                    mHelperCache.put(key, instance);
                    return instance;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    <T> List<T> getListenersByClass(Class<T> listenerClass) {
        return getListenersByClass(listenerClass, null);
    }

    <T> List<T> getListenersByClass(Class<T> listenerClass, String tag) {
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

    void scheduleRunnable(Runnable runnable, @EventsOnThread.ThreadType int threadType) {
        if (threadType == EventsOnThread.CURRENT_THREAD) {
            runnable.run();
        } else if (threadType == EventsOnThread.NEW_THREAD) {
            new Thread(runnable).start();
        } else if (threadType == EventsOnThread.MAIN_THREAD) {
            requireNonNull(mMainHandler, "You should call EventsHelper#init(Context) " +
                    "before calling methods on main thread");
            mMainHandler.post(runnable);
        }
    }

    private <T> T proxyOf(Class<T> listenerClass, String tag) {
        return (T) Proxy.newProxyInstance(listenerClass.getClassLoader(),
                new Class<?>[]{ listenerClass },
                new ListenerInvocationHandler(listenerClass, tag));
    }

    private class ListenerInvocationHandler implements InvocationHandler {

        private Class<?> listenerClass;
        private String tag;

        ListenerInvocationHandler(Class<?> listenerClass, String tag) {
            this.listenerClass = listenerClass;
            this.tag = tag;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Ignore ignoreAnnotation = method.getAnnotation(Ignore.class);
            if (ignoreAnnotation != null) {
                throw new UnsupportedOperationException("Method " + method + " is ignored. "
                        + "If you want to call this method by helpers, "
                        + "please remove @Ignore annotation from interface.");
            }

            if (method.getReturnType() != Void.class) {
                throw new UnsupportedOperationException("Method " + method + " in "
                        + listenerClass
                        + " class doesn't return void type.");
            }

            EventsOnThread annotation = method.getAnnotation(EventsOnThread.class);
            int threadType = EventsOnThread.CURRENT_THREAD;
            if (annotation != null) {
                threadType = annotation.value();
            }

            for (Object listener : getListenersByClass(listenerClass, tag)) {
                scheduleRunnable(() -> {
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

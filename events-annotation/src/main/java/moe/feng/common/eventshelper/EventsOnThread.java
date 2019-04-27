package moe.feng.common.eventshelper;

import androidx.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventsOnThread {

    int MAIN_THREAD = 0;
    int NEW_THREAD = 1;
    int CURRENT_THREAD = 2;

    @ThreadType
    int value() default CURRENT_THREAD;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({MAIN_THREAD, NEW_THREAD, CURRENT_THREAD})
    @interface ThreadType {}

}

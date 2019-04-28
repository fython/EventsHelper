package moe.feng.common.eventshelper.sample

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View

import moe.feng.common.eventshelper.EventsHelper
import moe.feng.common.eventshelper.listeners
import moe.feng.common.eventshelper.of

class SampleActivity : Activity() {

    companion object {

        private val TAG = SampleActivity::class.java.simpleName

    }

    private val mListenerA = object : TestListener {
        override fun onButtonClick(time: Long) {
            Log.d(TAG, "ListenerA: " + time + " thread: " + Thread.currentThread())
        }

        override fun ignoredMethod() {
            throw RuntimeException("This method shouldn't be called by EventsHelper")
        }
    }
    private val mListenerB = TestAsyncListener { time ->
        Log.d(TAG, "ListenerB: " + time + " thread: " + Thread.currentThread())
    }
    private val mListenerC = TestAsyncListener { time ->
        Log.d(TAG, "ListenerC: " + time + " thread: " + Thread.currentThread())
    }

    private lateinit var mEventsHelper: EventsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)

        mEventsHelper = EventsHelper.getInstance(this)

        findViewById<View>(R.id.send_event_button).setOnClickListener {
            Log.d(TAG, "Now calling TestListener#onButtonClick")
            mEventsHelper.of<TestListener>().onButtonClick(System.currentTimeMillis())
            Log.d(TAG, "Now calling B TestListener#onButtonClick")
            mEventsHelper.of<TestAsyncListener>("B").onButtonClick(System.currentTimeMillis())
            Log.d(TAG, "Now calling C TestListener#onButtonClick")
            mEventsHelper.of<TestAsyncListener>("C").onButtonClick(System.currentTimeMillis())

            Log.d(TAG, "Now calling TestListener#ignoredMethod with try-catch")
            try {
                mEventsHelper.of<TestListener>().ignoredMethod()
            } catch (e: Exception) {
                Log.e(TAG, "ignoredMethod of helper throws a exception", e)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mEventsHelper.listeners += mListenerA to "A"
        mEventsHelper.listeners += listOf(
                mListenerB to "B",
                mListenerC to "C"
        )
        // Java style
        // mEventsHelper.registerListener(mListenerA, "A")
        // mEventsHelper.registerListener(mListenerB, "B")
        // mEventsHelper.registerListener(mListenerC, "C")
    }

    override fun onStop() {
        super.onStop()
        mEventsHelper.listeners -= listOf(mListenerA, mListenerB, mListenerC)
        // Java style
        // mEventsHelper.unregisterListeners(mListenerA, mListenerB, mListenerC)
    }

}

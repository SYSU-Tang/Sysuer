package com.sysu.edu

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity

class Application : Application() {
	var currentActivity: FragmentActivity? = null
		private set

	override fun onCreate() {
		super.onCreate()
		registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
			override fun onActivityStarted(activity: Activity) {
				currentActivity = activity as? FragmentActivity
			}

			override fun onActivityResumed(activity: Activity) {
				currentActivity = activity as? FragmentActivity
			}

			override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
			override fun onActivityPaused(activity: Activity) {}
			override fun onActivityStopped(activity: Activity) {}
			override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
			override fun onActivityDestroyed(activity: Activity) {
				if (currentActivity == activity) currentActivity = null
			}
		})
	}
}
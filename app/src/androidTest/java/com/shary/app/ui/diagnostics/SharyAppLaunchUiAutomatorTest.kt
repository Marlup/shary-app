package com.shary.app.ui.diagnostics

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharyAppLaunchUiAutomatorTest {

    @Test
    fun launchesAppFromLauncher() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val packageName = context.packageName
        val device = UiDevice.getInstance(instrumentation)

        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: throw AssertionError("Launch intent not found for $packageName")

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        device.pressHome()
        context.startActivity(launchIntent)

        assertTrue(
            device.wait(Until.hasObject(By.pkg(packageName).depth(0)), 5_000)
        )
    }
}

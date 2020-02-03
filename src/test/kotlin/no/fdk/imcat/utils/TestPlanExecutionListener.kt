package no.fdk.imcat.utils

import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan

class TestPlanExecutionListener : TestExecutionListener {

    override fun testPlanExecutionFinished(testPlan: TestPlan?) {

        val testType = System.getProperty("test.type")

        if (testType != null && testType.contains("contract")) {
            ApiTestContainer.stopGracefully()
        }
    }
}

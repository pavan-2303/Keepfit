package com.keepfit.app.health

import android.os.ParcelFileDescriptor
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.keepfit.feature.steps.data.HealthConnectStepsRepository
import com.keepfit.feature.steps.data.StepsSnapshot
import java.io.FileInputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthConnectStepsRepositoryTest {
    @Test
    fun repositoryReadsGrantedTodayStepsFromHealthConnect() = runBlocking<Unit> {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val targetContext = instrumentation.targetContext
        assumeTrue(HealthConnectClient.getSdkStatus(targetContext) == HealthConnectClient.SDK_AVAILABLE)

        grantPermission(targetContext.packageName, HealthPermission.getReadPermission(StepsRecord::class))

        val client = HealthConnectClient.getOrCreate(targetContext)
        val zoneId = ZoneId.systemDefault()
        val todayStart = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant()
        val sevenDayStart = LocalDate.now(zoneId).minusDays(6).atStartOfDay(zoneId).toInstant()
        val beforeRead = Instant.now()
        val todayBefore = client.aggregateSteps(todayStart, beforeRead)
        val sevenDayBefore = client.aggregateSteps(sevenDayStart, beforeRead)

        val summary = requireNotNull(
            HealthConnectStepsRepository(targetContext).connectedSummaryOrNull(),
        ) {
            "Expected a connected steps summary after granting permission."
        }

        val afterRead = Instant.now()
        val todayAfter = client.aggregateSteps(todayStart, afterRead)
        val sevenDayAfter = client.aggregateSteps(sevenDayStart, afterRead)

        assertTrue(
            "Expected repository today steps ${summary.todaySteps} within the direct Health Connect range $todayBefore..$todayAfter.",
            summary.todaySteps in todayBefore..todayAfter,
        )
        assertTrue(
            "Expected repository seven-day steps ${summary.sevenDayTotal} within the direct Health Connect range $sevenDayBefore..$sevenDayAfter.",
            summary.sevenDayTotal in sevenDayBefore..sevenDayAfter,
        )
    }

    private suspend fun HealthConnectStepsRepository.connectedSummaryOrNull() =
        (loadSnapshot() as? StepsSnapshot.Connected)?.summary

    private suspend fun HealthConnectClient.aggregateSteps(start: Instant, end: Instant): Long =
        aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end),
            ),
        )[StepsRecord.COUNT_TOTAL] ?: 0L

    private fun grantPermission(packageName: String, permission: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val descriptor = instrumentation.uiAutomation.executeShellCommand(
            "pm grant $packageName $permission",
        )
        descriptor.useAndDrain()
    }

    private fun ParcelFileDescriptor.useAndDrain() {
        FileInputStream(fileDescriptor).use { input ->
            while (input.read() != -1) {
                // Drain the command output so the shell process can exit cleanly.
            }
        }
        close()
    }
}

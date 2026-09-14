package com.keepfit.app.health

import android.os.ParcelFileDescriptor
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
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
        grantPermission(targetContext.packageName, HealthPermission.getWritePermission(StepsRecord::class))

        val writer = HealthConnectClient.getOrCreate(targetContext)
        val zoneId = ZoneId.systemDefault()
        val (startTime, endTime) = requireNotNull(findEmptyMinuteSlot(writer, zoneId)) {
            "Expected at least one empty minute slot earlier today for Health Connect verification."
        }
        val zoneOffset = zoneId.rules.getOffset(endTime)
        val aggregateStart = startTime.minusSeconds(60)
        val aggregateEnd = endTime.plusSeconds(60)

        writer.deleteRecords(
            StepsRecord::class,
            TimeRangeFilter.between(aggregateStart, aggregateEnd),
        )
        Thread.sleep(500L)

        val repository = HealthConnectStepsRepository(targetContext)
        val before = repository.connectedSummaryOrNull()
        val insertedSteps = 1_234L

        try {
            writer.insertRecords(
                listOf(
                    StepsRecord(
                        startTime,
                        zoneOffset,
                        endTime,
                        zoneOffset,
                        insertedSteps,
                        Metadata.manualEntry(),
                    ),
                ),
            )

            Thread.sleep(1_500L)

            val aggregateEnd = Instant.now()
            val directAggregate = writer.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(aggregateStart, aggregateEnd),
                ),
            )[StepsRecord.COUNT_TOTAL] ?: 0L

            assertTrue(
                "Expected direct Health Connect aggregate to include at least $insertedSteps steps, but was $directAggregate.",
                directAggregate >= insertedSteps,
            )

            val after = requireNotNull(repository.connectedSummaryOrNull()) {
                "Expected a connected steps summary after granting permission."
            }

            val beforeToday = before?.todaySteps ?: 0L
            val beforeSevenDay = before?.sevenDayTotal ?: 0L

            assertTrue(
                "Expected today's total to increase by at least $insertedSteps, but was ${after.todaySteps - beforeToday}.",
                after.todaySteps >= beforeToday + insertedSteps,
            )
            assertTrue(
                "Expected seven-day total to increase by at least $insertedSteps, but was ${after.sevenDayTotal - beforeSevenDay}.",
                after.sevenDayTotal >= beforeSevenDay + insertedSteps,
            )
        } finally {
            writer.deleteRecords(
                StepsRecord::class,
                TimeRangeFilter.between(
                    aggregateStart,
                    aggregateEnd,
                ),
            )
        }
    }

    private suspend fun HealthConnectStepsRepository.connectedSummaryOrNull() =
        (loadSnapshot() as? StepsSnapshot.Connected)?.summary

    private suspend fun findEmptyMinuteSlot(
        client: HealthConnectClient,
        zoneId: ZoneId,
    ): Pair<Instant, Instant>? {
        val dayStart = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant()
        for (minuteOffset in 1..180) {
            val start = dayStart.plusSeconds(minuteOffset * 60L)
            val end = start.plusSeconds(60)
            val aggregate = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                ),
            )[StepsRecord.COUNT_TOTAL] ?: 0L
            if (aggregate == 0L) {
                return start to end
            }
        }
        return null
    }

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

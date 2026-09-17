package com.keepfit.feature.steps.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class HealthConnectStepsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : StepsRepository {
    override val requiredPermissions: Set<String> =
        setOf(HealthPermission.getReadPermission(StepsRecord::class))

    override suspend fun loadSnapshot(): StepsSnapshot {
        return when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_UNAVAILABLE -> StepsSnapshot.Unavailable
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> StepsSnapshot.UpdateRequired
            HealthConnectClient.SDK_AVAILABLE -> {
                val client = HealthConnectClient.getOrCreate(context)
                val grantedPermissions = client.permissionController.getGrantedPermissions()
                if (!grantedPermissions.containsAll(requiredPermissions)) {
                    StepsSnapshot.PermissionRequired
                } else {
                    StepsSnapshot.Connected(
                        summary = StepsSummary(
                            todaySteps = aggregateSteps(
                                client = client,
                                startTime = LocalDate.now().atStartOfDay(zoneId).toInstant(),
                                endTime = Instant.now(),
                            ),
                            sevenDayTotal = aggregateSteps(
                                client = client,
                                startTime = LocalDate.now().minusDays(6).atStartOfDay(zoneId).toInstant(),
                                endTime = Instant.now(),
                            ),
                        ),
                    )
                }
            }
            else -> StepsSnapshot.Unavailable
        }
    }

    override suspend fun loadTotal(startDate: LocalDate, endDate: LocalDate): Long? {
        if (endDate.isBefore(startDate)) return null
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return null
        val client = HealthConnectClient.getOrCreate(context)
        if (!client.permissionController.getGrantedPermissions().containsAll(requiredPermissions)) return null
        return aggregateSteps(
            client = client,
            startTime = startDate.atStartOfDay(zoneId).toInstant(),
            endTime = endDate.plusDays(1).atStartOfDay(zoneId).toInstant(),
        )
    }

    private suspend fun aggregateSteps(
        client: HealthConnectClient,
        startTime: Instant,
        endTime: Instant,
    ): Long {
        val response = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
            ),
        )
        return response[StepsRecord.COUNT_TOTAL] ?: 0L
    }

    private companion object {
        val zoneId: ZoneId = ZoneId.systemDefault()
    }
}

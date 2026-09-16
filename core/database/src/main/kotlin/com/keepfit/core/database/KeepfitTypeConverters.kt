package com.keepfit.core.database

import androidx.room.TypeConverter
import com.keepfit.core.database.nutrition.MealType
import com.keepfit.core.database.nutrition.MealQuality
import java.time.DayOfWeek
import java.time.LocalDate

class KeepfitTypeConverters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromDayOfWeek(value: DayOfWeek?): String? = value?.name

    @TypeConverter
    fun toDayOfWeek(value: String?): DayOfWeek? = value?.let(DayOfWeek::valueOf)

    @TypeConverter
    fun fromMealType(value: MealType?): String? = value?.name

    @TypeConverter
    fun toMealType(value: String?): MealType? = value?.let(MealType::valueOf)

    @TypeConverter
    fun fromMealQuality(value: MealQuality?): String? = value?.name

    @TypeConverter
    fun toMealQuality(value: String?): MealQuality? = value?.let(MealQuality::valueOf)

}

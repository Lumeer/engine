package io.lumeer.core.util

import io.lumeer.api.model.rule.CronRule
import java.time.DayOfWeek
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.math.min

class CronTaskChecker {

   companion object {

      @JvmStatic
      fun now(): ZonedDateTime {
         val now = ZonedDateTime.now()
         val date = Date(now.toInstant().toEpochMilli())
         return ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC)
      }
   }

   fun shouldExecute(rule: CronRule, on: ZonedDateTime = now()): Boolean {
      if (!checkInterval(rule, on)) return false

      return when (rule.unit) {
         ChronoUnit.DAYS -> checkDailyCron(rule, on)
         ChronoUnit.WEEKS -> checkWeeklyCron(rule, on)
         ChronoUnit.MONTHS -> checkMonthlyCron(rule, on)
         else -> false
      }
   }

   private fun checkInterval(rule: CronRule, date: ZonedDateTime): Boolean {
      if (rule.startsOn == null) {
         return false
      }
      if (rule.startsOn.isAfter(date)) {
         return false
      }

      if (rule.executionsLeft != null) {
         return rule.executionsLeft > 0
      }
      if (rule.endsOn != null) {
         return rule.endsOn.isAfter(date) || rule.endsOn.isEqual(date)
      }

      return true
   }

   private fun checkDailyCron(rule: CronRule, date: ZonedDateTime): Boolean {
      if (rule.lastRun == null) {
         return shouldRunOnDay(rule, date)
      }

      val runOn = rule.lastRun
            .plusDays(rule.interval.toLong())
            .withHour(rule.hour)
            .truncatedTo(ChronoUnit.HOURS)

      return shouldRunByHour(runOn, rule, date)
   }

   private fun checkMonthlyCron(rule: CronRule, date: ZonedDateTime): Boolean {
      val lastDayOfMonth = date.with(TemporalAdjusters.lastDayOfMonth()).dayOfMonth
      val dayOfMonth = min(lastDayOfMonth, rule.occurrence)

      if (dayOfMonth != date.dayOfMonth) {
         return false
      }

      if (rule.lastRun == null) {
         return shouldRunOnDay(rule, date)
      }

      val runOn = rule.lastRun
            .plusMonths(rule.interval.toLong())
            .withDayOfMonth(dayOfMonth)
            .withHour(rule.hour)
            .truncatedTo(ChronoUnit.HOURS)

      return shouldRunByHour(runOn, rule, date)
   }

   private fun checkWeeklyCron(rule: CronRule, date: ZonedDateTime): Boolean {
      val dayOfWeeks = getDayOfWeeks(rule)

      if (!dayOfWeeks.contains(date.dayOfWeek)) {
         return false
      }

      if (rule.lastRun == null) {
         return shouldRunOnDay(rule, date)
      }

      if (areSameWeeks(rule.lastRun, date)) {
         val runOn = rule.lastRun
               .with(ChronoField.DAY_OF_WEEK, date.dayOfWeek.value.toLong())
               .withHour(rule.hour)
               .truncatedTo(ChronoUnit.HOURS)

         return rule.lastRun.dayOfWeek != date.dayOfWeek && shouldRunByHour(runOn, rule, date)
      }

      val runOn = rule.lastRun
            .plusWeeks(rule.interval.toLong())
            .with(ChronoField.DAY_OF_WEEK, date.dayOfWeek.value.toLong())
            .withHour(rule.hour)
            .truncatedTo(ChronoUnit.HOURS)

      return shouldRunByHour(runOn, rule, date)
   }

   private fun shouldRunByHour(runOn: ZonedDateTime, rule: CronRule, date: ZonedDateTime): Boolean {
      return (runOn.isBefore(date) || runOn.isEqual(date)) && rule.hour <= date.hour
   }

   private fun areSameWeeks(d1: ZonedDateTime, d2: ZonedDateTime): Boolean {
      val d1Monday = d1.with(ChronoField.DAY_OF_WEEK, DayOfWeek.MONDAY.value.toLong())
      val d2Monday = d2.with(ChronoField.DAY_OF_WEEK, DayOfWeek.MONDAY.value.toLong())
      return d1Monday.year == d2Monday.year &&
         d1Monday.get(ChronoField.ALIGNED_WEEK_OF_YEAR) == d2Monday.get(ChronoField.ALIGNED_WEEK_OF_YEAR)
   }

   private fun shouldRunOnDay(rule: CronRule, date: ZonedDateTime): Boolean {
      val createdAt = rule.rule.createdAt ?: date
      val shouldBeExecutedAt = date.withHour(rule.hour)
            .truncatedTo(ChronoUnit.HOURS)
      return createdAt.isBefore(shouldBeExecutedAt) && rule.hour <= date.hour
   }

   private fun getDayOfWeeks(rule: CronRule): List<DayOfWeek> {
      return (1..7).filter { isNthBitSet(rule.daysOfWeek, it - 1) }.map { DayOfWeek.of(it) }
   }

   private fun isNthBitSet(num: Int, n: Int) = (num and (1 shl n)) > 0
}

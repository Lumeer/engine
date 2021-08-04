package io.lumeer.core.util

import io.lumeer.api.model.rule.CronRule
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.min

class CronTaskChecker {

   fun shouldExecute(rule: CronRule): Boolean {
      if (!checkInterval(rule)) return false

      return when (rule.unit) {
         ChronoUnit.DAYS -> checkDailyCron(rule)
         ChronoUnit.WEEKS -> checkWeeklyCron(rule)
         ChronoUnit.MONTHS -> checkMonthlyCron(rule)
         else -> false
      }
   }

   private fun checkInterval(rule: CronRule): Boolean {
      if (rule.startsOn == null) {
         return false
      }
      if (rule.startsOn.isAfter(ZonedDateTime.now())) {
         return false
      }

      if (rule.executionsLeft != null) {
         return rule.executionsLeft > 0
      }
      if (rule.endsOn != null) {
         return rule.endsOn.isBefore(ZonedDateTime.now())
      }

      return true
   }

   private fun checkDailyCron(rule: CronRule): Boolean {
      if (rule.lastRun == null) {
         return shouldRunToday(rule)
      }

      val now = ZonedDateTime.now()
      val runOn = rule.lastRun
            .plusDays(rule.interval.toLong())
            .truncatedTo(ChronoUnit.HOURS)

      return shouldRunByHour(runOn, rule)
   }

   private fun checkMonthlyCron(rule: CronRule): Boolean {
      val now = ZonedDateTime.now()
      val lastDayOfMonth = now.with(TemporalAdjusters.lastDayOfMonth()).dayOfMonth
      val dayOfMonth = min(lastDayOfMonth, rule.occurrence)

      if (dayOfMonth != now.dayOfMonth) {
         return false
      }

      if (rule.lastRun == null) {
         return shouldRunToday(rule)
      }

      val runOn = rule.lastRun
            .plusMonths(rule.interval.toLong())
            .withDayOfMonth(dayOfMonth)
            .truncatedTo(ChronoUnit.HOURS)

      return shouldRunByHour(runOn, rule)
   }

   private fun checkWeeklyCron(rule: CronRule): Boolean {
      val now = ZonedDateTime.now()
      val dayOfWeeks = getDayOfWeeks(rule)

      if (!dayOfWeeks.contains(now.dayOfWeek)) {
         return false
      }

      if (rule.lastRun == null) {
         return shouldRunToday(rule)
      }

      if (areSameWeeks(rule.lastRun, now)) {
         val runOn = rule.lastRun
               .with(ChronoField.DAY_OF_WEEK, now.dayOfWeek.value.toLong())
               .truncatedTo(ChronoUnit.HOURS)

         return rule.lastRun.dayOfWeek != now.dayOfWeek && shouldRunByHour(runOn, rule)
      }

      val runOn = rule.lastRun
            .plusWeeks(rule.interval.toLong())
            .with(ChronoField.DAY_OF_WEEK, now.dayOfWeek.value.toLong())
            .truncatedTo(ChronoUnit.HOURS)

      return shouldRunByHour(runOn, rule)
   }

   private fun shouldRunByHour(runOn: ZonedDateTime, rule: CronRule): Boolean {
      val now = ZonedDateTime.now()
      return (runOn.isBefore(now) || runOn.isEqual(now)) && rule.hour <= now.hour
   }

   private fun areSameWeeks(d1: ZonedDateTime, d2: ZonedDateTime): Boolean = d1.year == d2.year && d1.get(ChronoField.ALIGNED_WEEK_OF_YEAR) == d2.get(ChronoField.ALIGNED_WEEK_OF_YEAR)

   private fun shouldRunToday(rule: CronRule): Boolean {
      val now = ZonedDateTime.now()
      val createdAt = rule.rule.createdAt ?: ZonedDateTime.now()
      val shouldBeExecutedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS).withHour(rule.hour)
      return createdAt.isBefore(shouldBeExecutedAt) && rule.hour <= now.hour
   }

   private fun getDayOfWeeks(rule: CronRule): List<DayOfWeek> {
      return (1..7).filter { isNthBitSet(rule.daysOfWeek, it - 1) }.map { DayOfWeek.of(it) }

   }

   private fun isNthBitSet(num: Int, n: Int) = (num and (1 shl n)) > 0
}

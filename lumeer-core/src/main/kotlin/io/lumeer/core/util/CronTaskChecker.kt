package io.lumeer.core.util

import io.lumeer.api.model.rule.CronRule
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.min

class CronTaskChecker {

   fun shouldExecute(rule: CronRule, on: ZonedDateTime): Boolean {
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
         return rule.endsOn.isBefore(date)
      }

      return true
   }

   private fun checkDailyCron(rule: CronRule, date: ZonedDateTime): Boolean {
      if (rule.lastRun == null) {
         return shouldRunOnDay(rule, date)
      }

      val runOn = rule.lastRun
            .plusDays(rule.interval.toLong())
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
               .truncatedTo(ChronoUnit.HOURS)

         return rule.lastRun.dayOfWeek != date.dayOfWeek && shouldRunByHour(runOn, rule, date)
      }

      val runOn = rule.lastRun
            .plusWeeks(rule.interval.toLong())
            .with(ChronoField.DAY_OF_WEEK, date.dayOfWeek.value.toLong())
            .truncatedTo(ChronoUnit.HOURS)

      return shouldRunByHour(runOn, rule, date)
   }

   private fun shouldRunByHour(runOn: ZonedDateTime, rule: CronRule, date: ZonedDateTime): Boolean {
      return (runOn.isBefore(date) || runOn.isEqual(date)) && rule.hour <= date.hour
   }

   private fun areSameWeeks(d1: ZonedDateTime, d2: ZonedDateTime): Boolean = d1.year == d2.year && d1.get(ChronoField.ALIGNED_WEEK_OF_YEAR) == d2.get(ChronoField.ALIGNED_WEEK_OF_YEAR)

   private fun shouldRunOnDay(rule: CronRule, date: ZonedDateTime): Boolean {
      val createdAt = rule.rule.createdAt ?: date
      val shouldBeExecutedAt = date.truncatedTo(ChronoUnit.HOURS).withHour(rule.hour)
      return createdAt.isBefore(shouldBeExecutedAt) && rule.hour <= date.hour
   }

   private fun getDayOfWeeks(rule: CronRule): List<DayOfWeek> {
      return (1..7).filter { isNthBitSet(rule.daysOfWeek, it - 1) }.map { DayOfWeek.of(it) }
   }

   private fun isNthBitSet(num: Int, n: Int) = (num and (1 shl n)) > 0
}

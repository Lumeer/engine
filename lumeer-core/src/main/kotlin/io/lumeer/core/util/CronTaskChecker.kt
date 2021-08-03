package io.lumeer.core.util

import io.lumeer.api.model.rule.CronRule
import java.time.ZonedDateTime
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
      val now = ZonedDateTime.now()
      if (rule.lastRun == null) {
         val createdAt = rule.rule.createdAt ?: ZonedDateTime.now()
         val shouldBeExecutedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS).withHour(rule.hour)
         return createdAt.isBefore(shouldBeExecutedAt) && rule.hour <= now.hour
      }

      var runOn = rule.lastRun
      runOn = runOn.plusDays(rule.interval.toLong())

      return runOn.isBefore(now) && rule.hour <= now.hour
   }

   private fun checkMonthlyCron(rule: CronRule): Boolean {
      val now = ZonedDateTime.now()
      val lastDayOfMonth = now.with(TemporalAdjusters.lastDayOfMonth()).dayOfMonth
      val dayOfMonth = min(lastDayOfMonth, rule.occurrence)

      if (rule.lastRun == null) {
         val createdAt = rule.rule.createdAt ?: ZonedDateTime.now()
         val shouldBeExecutedAt = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS).withHour(rule.hour)
         return createdAt.isBefore(shouldBeExecutedAt) && dayOfMonth == now.dayOfMonth && rule.hour <= now.hour
      }

      var runOn = rule.lastRun
      runOn = runOn.plusMonths(rule.interval.toLong())
      runOn = runOn.withDayOfMonth(dayOfMonth)

      return runOn.isBefore(now) && rule.hour <= now.hour
   }

   private fun checkWeeklyCron(rule: CronRule): Boolean {
      val now = ZonedDateTime.now()
      var runOn = rule.lastRun

      if (runOn == null) {

      }

      return false
   }
}

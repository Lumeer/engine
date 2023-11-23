package io.lumeer.core.util

import io.lumeer.api.model.Rule
import io.lumeer.api.model.rule.CronRule
import io.lumeer.engine.api.data.DataDocument
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import kotlin.math.pow

class CronTaskCheckerTest {

   private val checker = CronTaskChecker()

   @Test
   fun checkRunningRule() {
      val (rule, now) = createRunningRuleData()

      Assertions.assertThat(checker.shouldExecute(rule, now)).isTrue
   }

   @Test
   fun checkInvalidCronUnit() {
      val (rule, now) = createRunningRuleData()
      rule.unit = ChronoUnit.DECADES

      Assertions.assertThat(checker.shouldExecute(rule, now)).isFalse
   }

   @Test
   fun checkCronBeforeStartsOn() {
      val (rule, now) = createRunningRuleData()
      rule.startsOn = now.plusDays(2)

      Assertions.assertThat(checker.shouldExecute(rule, now)).isFalse
   }

   @Test
   fun checkCronAfterEndsOn() {
      val (rule, now) = createRunningRuleData()
      rule.endsOn = now.minusDays(2)

      Assertions.assertThat(checker.shouldExecute(rule, now)).isFalse
   }

   @Test
   fun checkCronExecutionsLeft() {
      val (rule, now) = createRunningRuleData()
      rule.executionsLeft = 2

      Assertions.assertThat(checker.shouldExecute(rule, now)).isTrue

      rule.executionsLeft = 0
      Assertions.assertThat(checker.shouldExecute(rule, now)).isFalse
   }

   @Test
   fun checkDailyCronCreatedAfterAndBeforeHour() {
      val createdAt = CronTaskChecker.now().withHour(15)
      val rule = createRule(createdAt)
      rule.unit = ChronoUnit.DAYS
      rule.hour = 14
      rule.interval = 2
      rule.startsOn = CronTaskChecker.now().minusDays(10)

      // created at after execution
      val now = CronTaskChecker.now().withHour(14).truncatedTo(ChronoUnit.HOURS)
      Assertions.assertThat(checker.shouldExecute(rule, now)).isFalse

      // created at before execution
      rule.rule.createdAt = createdAt.withHour(13)
      Assertions.assertThat(checker.shouldExecute(rule, now)).isTrue
   }

   @Test
   fun checkDailyCronAfterSomeExecution() {
      val now = CronTaskChecker.now().withHour(18).withMinute(20)

      val rule = createRule()
      rule.unit = ChronoUnit.DAYS
      rule.hour = 18
      rule.interval = 2
      rule.startsOn = now.minusDays(10)
      rule.lastRun = now.minusDays(1)

      Assertions.assertThat(checker.shouldExecute(rule, now)).isFalse

      rule.lastRun = now.minusDays(2)
      Assertions.assertThat(checker.shouldExecute(rule, now)).isTrue

      // check with delay

      rule.lastRun = now.minusDays(2).plusSeconds(23)
      Assertions.assertThat(checker.shouldExecute(rule, now)).isTrue

      rule.lastRun = now.minusDays(2).plusMinutes(30)
      Assertions.assertThat(checker.shouldExecute(rule, now)).isTrue

      rule.lastRun = now.minusDays(2).plusHours(1)
      Assertions.assertThat(checker.shouldExecute(rule, now)).isTrue
   }

   @Test
   fun checkMonthlyCronCreatedAfterAndBeforeHour() {
      val createdAt = CronTaskChecker.now().withDayOfMonth(12).withHour(10)
      val rule = createRule(createdAt)
      rule.unit = ChronoUnit.MONTHS
      rule.hour = 9
      rule.occurrence = 12
      rule.interval = 3
      rule.startsOn = CronTaskChecker.now().withDayOfMonth(12).minusDays(10)

      // created at after execution
      val now = CronTaskChecker.now().withDayOfMonth(12).withHour(9).truncatedTo(ChronoUnit.HOURS)
      Assertions.assertThat(checker.shouldExecute(rule, now)).isFalse

      // created at before execution
      rule.rule.createdAt = createdAt.withHour(8)
      Assertions.assertThat(checker.shouldExecute(rule, now)).isTrue
   }

   @Test
   fun checkMonthlyCronOtherDay() {
      val createdAt = CronTaskChecker.now().withDayOfMonth(1).withHour(5)
      val rule = createRule(createdAt)
      rule.unit = ChronoUnit.MONTHS
      rule.hour = 9
      rule.occurrence = 1
      rule.interval = 3
      rule.startsOn = CronTaskChecker.now().withDayOfMonth(1).minusDays(10)

      // created at after execution
      val now = CronTaskChecker.now().withDayOfMonth(3).withHour(9).truncatedTo(ChronoUnit.HOURS)
      Assertions.assertThat(checker.shouldExecute(rule, now)).isFalse
   }

   @Test
   fun checkMonthlyCronAfterExecution() {
      val now = CronTaskChecker.now().withDayOfMonth(1).withHour(11).withMinute(44)

      val rule = createRule(now.minusMonths(10))
      rule.unit = ChronoUnit.MONTHS
      rule.hour = 11
      rule.occurrence = 1
      rule.interval = 3
      rule.startsOn = now.minusMonths(10)
      rule.lastRun = now.minusMonths(1)

      Assertions.assertThat(checker.shouldExecute(rule, now)).isFalse

      rule.lastRun = now.minusMonths(2)
      Assertions.assertThat(checker.shouldExecute(rule, now)).isFalse

      rule.lastRun = now.minusMonths(3)
      Assertions.assertThat(checker.shouldExecute(rule, now)).isTrue

      rule.occurrence = 3
      Assertions.assertThat(checker.shouldExecute(rule, now)).isFalse
   }

   @Test
   fun checkWeeklyCronCreatedAfterAndBeforeHour() {
      val createdAt = CronTaskChecker.now().withHour(10)
      val rule = createRule(createdAt)
      rule.unit = ChronoUnit.WEEKS
      rule.hour = 9
      rule.daysOfWeek = 127 // every day in week
      rule.interval = 3
      rule.startsOn = createdAt.minusDays(1)

      // created at after execution
      val now = createdAt.withHour(9).truncatedTo(ChronoUnit.HOURS)
      Assertions.assertThat(checker.shouldExecute(rule, now)).isFalse

      // created at before execution
      rule.rule.createdAt = createdAt.withHour(8)
      Assertions.assertThat(checker.shouldExecute(rule, now)).isTrue
   }

   @Test
   fun checkWeeklyCronOtherDay() {
      val createdAt = CronTaskChecker.now().minusDays(10)
      val daysOfWeek = 2f.pow(CronTaskChecker.now().dayOfWeek.value - 1).toInt()
      val rule = createRule(createdAt)
      rule.unit = ChronoUnit.WEEKS
      rule.hour = 9
      rule.daysOfWeek = daysOfWeek - 1
      rule.interval = 3
      rule.startsOn = createdAt

      // check execution other day
      val now = CronTaskChecker.now().withHour(9).truncatedTo(ChronoUnit.HOURS)
      Assertions.assertThat(checker.shouldExecute(rule, now)).isFalse

      // check execution this day
      rule.daysOfWeek = daysOfWeek
      Assertions.assertThat(checker.shouldExecute(rule, now)).isTrue
   }

   @Test
   fun checkWeeklyAfterExecutedThisWeek() {
      val createdAt = CronTaskChecker.now().minusYears(1)

      val monday = DayOfWeek.MONDAY.value.toLong()
      val wednesday = DayOfWeek.WEDNESDAY.value.toLong()
      val saturday = DayOfWeek.SATURDAY.value.toLong()

      val rule = createRule(createdAt)
      rule.unit = ChronoUnit.WEEKS
      rule.hour = 6
      rule.daysOfWeek = 1 + 4 + 32 // MON, WED, SAT
      rule.interval = 3
      rule.startsOn = createdAt
      rule.lastRun = CronTaskChecker.now()
            .with(ChronoField.DAY_OF_WEEK, monday)

      // ran in monday and check if it should run in wednesday
      var now = CronTaskChecker.now()
            .with(ChronoField.DAY_OF_WEEK, wednesday)
            .withHour(6)
            .truncatedTo(ChronoUnit.HOURS)
      Assertions.assertThat(checker.shouldExecute(rule, now)).isTrue

      // ran in monday and check if it should run in saturday
      now = now.with(ChronoField.DAY_OF_WEEK, saturday)
      Assertions.assertThat(checker.shouldExecute(rule, now)).isTrue

      // ran in monday and check if it should run in monday
      now = now.with(ChronoField.DAY_OF_WEEK, monday)
      Assertions.assertThat(checker.shouldExecute(rule, now)).isFalse


   }


   @Test
   fun checkWeeklyAfterExecutedOtherWeek() {
      val createdAt = CronTaskChecker.now().minusYears(1)
      val rule = createRule(createdAt)
      rule.unit = ChronoUnit.WEEKS
      rule.hour = 4
      rule.daysOfWeek = 2f.pow(CronTaskChecker.now().dayOfWeek.value - 1).toInt()
      rule.interval = 5
      rule.startsOn = createdAt
      rule.lastRun = CronTaskChecker.now().minusWeeks(1)

      val now = CronTaskChecker.now().withHour(4).truncatedTo(ChronoUnit.HOURS)
      Assertions.assertThat(checker.shouldExecute(rule, now)).isFalse

      rule.lastRun = CronTaskChecker.now().minusWeeks(2)
      Assertions.assertThat(checker.shouldExecute(rule, now)).isFalse

      rule.lastRun = CronTaskChecker.now().minusWeeks(3)
      Assertions.assertThat(checker.shouldExecute(rule, now)).isFalse

      rule.lastRun = CronTaskChecker.now().minusWeeks(4)
      Assertions.assertThat(checker.shouldExecute(rule, now)).isFalse

      rule.lastRun = CronTaskChecker.now().minusWeeks(5)
      Assertions.assertThat(checker.shouldExecute(rule, now)).isTrue
   }

   private fun createRule(createdAt: ZonedDateTime? = null): CronRule {
      val rule = Rule("r1", Rule.RuleType.CRON, null, DataDocument())
      rule.createdAt = createdAt
      return CronRule(rule)
   }

   private fun createRunningRuleData(): Pair<CronRule, ZonedDateTime> {
      val createdAt = CronTaskChecker.now().withHour(13)
      val rule = createRule(createdAt).apply {
         unit = ChronoUnit.DAYS
         hour = 14
         interval = 2
         startsOn = CronTaskChecker.now().minusDays(10)
      }

      val now = CronTaskChecker.now().withHour(14).truncatedTo(ChronoUnit.HOURS)
      return Pair(rule, now)
   }
}

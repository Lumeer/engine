package io.lumeer.core.util

import io.lumeer.api.model.Rule
import io.lumeer.api.model.rule.CronRule
import io.lumeer.engine.api.data.DataDocument
import org.assertj.core.api.Assertions
import org.junit.Test
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class CronTaskCheckerTest {

   @Test
   fun checkRunningRule() {
      val (rule, now) = createRunningRuleData()

      Assertions.assertThat(CronTaskChecker().shouldExecute(rule, now)).isTrue
   }

   @Test
   fun checkInvalidCronUnit() {
      val (rule, now) = createRunningRuleData()
      rule.unit = ChronoUnit.DECADES

      Assertions.assertThat(CronTaskChecker().shouldExecute(rule, now)).isFalse
   }

   @Test
   fun checkCronBeforeStartsOn() {
      val (rule, now) = createRunningRuleData()
      rule.startsOn = now.plusDays(2)

      Assertions.assertThat(CronTaskChecker().shouldExecute(rule, now)).isFalse
   }

   @Test
   fun checkCronAfterEndsOn() {
      val (rule, now) = createRunningRuleData()
      rule.endsOn = now.minusDays(2)

      Assertions.assertThat(CronTaskChecker().shouldExecute(rule, now)).isFalse
   }

   @Test
   fun checkCronExecutionsLeft() {
      val (rule, now) = createRunningRuleData()
      rule.executionsLeft = 2

      Assertions.assertThat(CronTaskChecker().shouldExecute(rule, now)).isTrue

      rule.executionsLeft = 0
      Assertions.assertThat(CronTaskChecker().shouldExecute(rule, now)).isFalse
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
      Assertions.assertThat(CronTaskChecker().shouldExecute(rule, now)).isFalse

      // created at before execution
      rule.rule.createdAt = createdAt.withHour(13)
      Assertions.assertThat(CronTaskChecker().shouldExecute(rule, now)).isTrue
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

      Assertions.assertThat(CronTaskChecker().shouldExecute(rule, now)).isFalse

      rule.lastRun = now.minusDays(2)
      Assertions.assertThat(CronTaskChecker().shouldExecute(rule, now)).isTrue

      // check with delay

      rule.lastRun = now.minusDays(2).plusSeconds(23)
      Assertions.assertThat(CronTaskChecker().shouldExecute(rule, now)).isTrue

      rule.lastRun = now.minusDays(2).plusMinutes(30)
      Assertions.assertThat(CronTaskChecker().shouldExecute(rule, now)).isTrue

      rule.lastRun = now.minusDays(2).plusHours(1)
      Assertions.assertThat(CronTaskChecker().shouldExecute(rule, now)).isTrue
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

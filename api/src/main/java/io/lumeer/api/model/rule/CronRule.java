/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.lumeer.api.model.rule;

import io.lumeer.api.model.Language;
import io.lumeer.api.model.Query;
import io.lumeer.api.model.Rule;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class CronRule extends BlocklyRule {

   public static final String CRON_SINCE = "since";
   public static final String CRON_UNTIL = "until";
   public static final String CRON_WHEN = "when";
   public static final String CRON_INTERVAL = "interval";
   public static final String CRON_LAST_RUN = "lastRun";
   public static final String CRON_UNIT = "unit";
   public static final String CRON_EXECUTING = "executing";
   public static final String CRON_QUERY = "query";
   public static final String CRON_LANGUAGE = "language";
   public static final String CRON_DOW = "dow"; // days of week - stored as binary number starting with Monday as the least significant bit
   public static final String CRON_OCCURRENCE = "occurrence";

   public CronRule(final Rule rule) {
      super(rule, Rule.RuleType.CRON);
   }

   public ZonedDateTime getSince() {
      final Date since = rule.getConfiguration().getDate(CRON_SINCE);
      return since == null ? null : ZonedDateTime.ofInstant(since.toInstant(), ZoneOffset.UTC);
   }

   public void setSince(final ZonedDateTime since) {
      rule.getConfiguration().put(CRON_SINCE, new Date(since.toInstant().toEpochMilli()));
   }

   public ZonedDateTime getUntil() {
      final Date until = rule.getConfiguration().getDate(CRON_UNTIL);
      return until == null ? null : ZonedDateTime.ofInstant(until.toInstant(), ZoneOffset.UTC);
   }

   public void setUntil(final ZonedDateTime until) {
      rule.getConfiguration().put(CRON_UNTIL, new Date(until.toInstant().toEpochMilli()));
   }

   public int getWhen() {
      return rule.getConfiguration().getInteger(CRON_WHEN);
   }

   public void setWhen(final int when) {
      rule.getConfiguration().put(CRON_WHEN, when);
   }

   public int getInterval() {
      return rule.getConfiguration().getInteger(CRON_INTERVAL);
   }

   public void setInterval(final int interval) {
      rule.getConfiguration().put(CRON_INTERVAL, interval);
   }

   public ZonedDateTime getLastRun() {
      final Date lastRun = rule.getConfiguration().getDate(CRON_LAST_RUN);
      return lastRun == null ? null : ZonedDateTime.ofInstant(lastRun.toInstant(), ZoneOffset.UTC);
   }

   public void setLastRun(final ZonedDateTime lastRun) {
      rule.getConfiguration().put(CRON_LAST_RUN, new Date(lastRun.toInstant().toEpochMilli()));
   }

   public ChronoUnit getUnit() {
      final String unit = rule.getConfiguration().getString(CRON_UNIT);
      return unit != null ? ChronoUnit.valueOf(unit) : null;
   }

   public void setUnit(final ChronoUnit unit) {
      rule.getConfiguration().put(CRON_UNIT, unit != null ? unit.toString() : null);
   }

   public String getExecuting() {
      return rule.getConfiguration().getString(CRON_EXECUTING);
   }

   public void setExecuting(final String executing) {
      rule.getConfiguration().put(CRON_EXECUTING, executing);
   }

   public Query getQuery() {
      return (Query) rule.getConfiguration().get(CRON_QUERY);
   }

   public void setQuery(final Query query) {
      rule.getConfiguration().put(CRON_QUERY, query);
   }

   public Language getLanguage() {
      return Language.fromString(rule.getConfiguration().getString(CRON_LANGUAGE));
   }

   public void setLanguage(final Language language) {
      rule.getConfiguration().put(CRON_LANGUAGE, language.toString());
   }

   public int getDow() {
      return rule.getConfiguration().getInteger(CRON_DOW);
   }

   public void setDow(final int dow) {
      rule.getConfiguration().put(CRON_DOW, dow);
   }

   public int getOccurrence() {
      return rule.getConfiguration().getInteger(CRON_OCCURRENCE);
   }

   public void setOccurrence(final int occurrence) {
      rule.getConfiguration().put(CRON_OCCURRENCE, occurrence);
   }
}

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
import io.lumeer.api.model.Rule;
import io.lumeer.engine.api.data.DataDocument;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;

public class CronRule extends BlocklyRule {

   public static final String CRON_STARTS_ON = "startsOn";
   public static final String CRON_ENDS_ON = "endsOn";
   public static final String CRON_EXECUTIONS_LEFT = "executionsLeft";
   public static final String CRON_HOUR = "hour";
   public static final String CRON_INTERVAL = "interval";
   public static final String CRON_LAST_RUN = "lastRun";
   public static final String CRON_UNIT = "unit";
   public static final String CRON_EXECUTING = "executing";
   public static final String CRON_VIEW_ID = "viewId";
   public static final String CRON_LANGUAGE = "language";
   public static final String CRON_DAYS_OF_WEEK = "daysOfWeek"; // stored as binary number starting with Monday as the least significant bit
   public static final String CRON_OCCURRENCE = "occurrence";

   public CronRule(final Rule rule) {
      super(rule, Rule.RuleType.CRON);
   }

   public static DataDocument parseConfiguration(DataDocument configuration) {
      DataDocument parsedConfiguration = new DataDocument(configuration);

      parseDate(parsedConfiguration, CRON_STARTS_ON);
      parseDate(parsedConfiguration, CRON_ENDS_ON);
      parseNumber(parsedConfiguration, CRON_EXECUTIONS_LEFT);
      parseNumber(parsedConfiguration, CRON_INTERVAL);
      parseNumber(parsedConfiguration, CRON_HOUR);
      parseNumber(parsedConfiguration, CRON_DAYS_OF_WEEK);
      parseNumber(parsedConfiguration, CRON_OCCURRENCE);

      return parsedConfiguration;
   }

   private static void parseDate(DataDocument parsedConfiguration, String param) {
      if (!parsedConfiguration.containsKey(param)) {
         return;
      }
      try {
         String string = parsedConfiguration.getString(param, "").trim();
         Date date = Date.from(Instant.parse(string));
         parsedConfiguration.put(param, date);
      } catch (DateTimeParseException ignored) {
      }
   }

   private static void parseNumber(DataDocument parsedConfiguration, String param) {
      if (!parsedConfiguration.containsKey(param)) {
         return;
      }
      try {
         Object object = parsedConfiguration.get(param);
         if (object instanceof String s) {
            Integer integer = Integer.parseInt(s);
            parsedConfiguration.put(param, integer);
         }
      } catch (NumberFormatException ignored) {
      }
   }

   public static Set<String> internalConfigurationKeys() {
      return Set.of(CRON_EXECUTING, CRON_LAST_RUN);
   }

   public boolean shouldResetCreatedAt(CronRule originalRule) {
      if (getLastRun() == null) {
         if (getHour() != originalRule.getHour() || !getStartsOn().isEqual(originalRule.getStartsOn()) || !getUnit().equals(originalRule.getUnit())) {
            return true;
         }

         switch (getUnit()) {
            case WEEKS:
               return getDaysOfWeek() != originalRule.getDaysOfWeek();
            case MONTHS:
               return getOccurrence() != originalRule.getOccurrence();
            default:
               return false;
         }
      }

      return false;
   }

   public ZonedDateTime getStartsOn() {
      final Date since = rule.getConfiguration().getDate(CRON_STARTS_ON);
      return since == null ? null : ZonedDateTime.ofInstant(since.toInstant(), ZoneOffset.UTC);
   }

   public void setStartsOn(final ZonedDateTime startsOn) {
      rule.getConfiguration().put(CRON_STARTS_ON, new Date(startsOn.toInstant().toEpochMilli()));
   }

   public ZonedDateTime getEndsOn() {
      final Date until = rule.getConfiguration().getDate(CRON_ENDS_ON);
      return until == null ? null : ZonedDateTime.ofInstant(until.toInstant(), ZoneOffset.UTC);
   }

   public void setEndsOn(final ZonedDateTime endsOn) {
      if (endsOn != null) {
         rule.getConfiguration().put(CRON_ENDS_ON, new Date(endsOn.toInstant().toEpochMilli()));
      } else {
         rule.getConfiguration().remove(CRON_ENDS_ON);
      }
   }

   public int getHour() {
      return rule.getConfiguration().getInteger(CRON_HOUR);
   }

   public void setHour(final int hour) {
      rule.getConfiguration().put(CRON_HOUR, hour);
   }

   public int getInterval() {
      return rule.getConfiguration().getInteger(CRON_INTERVAL);
   }

   public void setInterval(final int interval) {
      rule.getConfiguration().put(CRON_INTERVAL, interval);
   }

   public ZonedDateTime getLastRun() {
      final var raw = rule.getConfiguration().getObject(CRON_LAST_RUN);
      if (raw instanceof Date d) {
         return ZonedDateTime.ofInstant(d.toInstant(), ZoneOffset.UTC);
      } else if (raw instanceof Long l) {
         return ZonedDateTime.ofInstant(Instant.ofEpochMilli(l), ZoneOffset.UTC);
      }

      return null;
   }

   public void setLastRun(final ZonedDateTime lastRun) {
      rule.getConfiguration().put(CRON_LAST_RUN, new Date(lastRun.toInstant().toEpochMilli()));
   }

   public ChronoUnit getUnit() {
      final String unit = rule.getConfiguration().getString(CRON_UNIT);
      return unit != null ? ChronoUnit.valueOf(unit.toUpperCase()) : null;
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

   public String getViewId() {
      return rule.getConfiguration().getString(CRON_VIEW_ID);
   }

   public void setViewId(final String viewId) {
      rule.getConfiguration().put(CRON_VIEW_ID, viewId);
   }

   public Language getLanguage() {
      return Language.fromString(rule.getConfiguration().getString(CRON_LANGUAGE));
   }

   public void setLanguage(final Language language) {
      rule.getConfiguration().put(CRON_LANGUAGE, language.toString());
   }

   public int getDaysOfWeek() {
      return rule.getConfiguration().getInteger(CRON_DAYS_OF_WEEK, 0);
   }

   public void setDaysOfWeek(final int dow) {
      rule.getConfiguration().put(CRON_DAYS_OF_WEEK, dow);
   }

   public int getOccurrence() {
      return rule.getConfiguration().getInteger(CRON_OCCURRENCE, 1);
   }

   public void setOccurrence(final int occurrence) {
      rule.getConfiguration().put(CRON_OCCURRENCE, occurrence);
   }

   public Integer getExecutionsLeft() {
      return rule.getConfiguration().getInteger(CRON_EXECUTIONS_LEFT);
   }

   public void setExecutionsLeft(final Integer executions) {
      rule.getConfiguration().put(CRON_EXECUTIONS_LEFT, executions);
   }
}

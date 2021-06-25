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
package io.lumeer.core.facade.translate;

import io.lumeer.api.model.DurationUnit;
import io.lumeer.api.model.Language;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TranslationManager {

   public Map<String, String> translateDurationUnitsMap(@Nullable  Language language) {
      switch (language != null ? language : Language.EN) {
         case CS:
            return translateDurationUnitsMapCs();
         case HU:
            return translateDurationUnitsMapHu();
         case JA:
            return translateDurationUnitsMapJa();
         default:
            return translateDurationUnitsMapEn();
      }
   }

   private Map<String, String> translateDurationUnitsMapEn() {
      return Arrays.stream(DurationUnit.values()).collect(Collectors.toMap(DurationUnit::getValue, DurationUnit::getValue));
   }

   private Map<String, String> translateDurationUnitsMapCs() {
      return Arrays.stream(DurationUnit.values()).collect(Collectors.toMap(DurationUnit::getValue, unit -> {
         switch (unit) {
            case Weeks:
               return "t";
            case Days:
               return "d";
            case Minutes:
               return "m";
            case Hours:
               return "h";
            case Seconds:
               return "s";
            default:
               return "";
         }
      }));
   }

   private Map<String, String> translateDurationUnitsMapHu() {
      return Arrays.stream(DurationUnit.values()).collect(Collectors.toMap(DurationUnit::getValue, unit -> {
         switch (unit) {
            case Weeks:
               return "h";
            case Days:
               return "n";
            case Minutes:
               return "ó";
            case Hours:
               return "p";
            case Seconds:
               return "m";
            default:
               return "";
         }
      }));
   }

   private Map<String, String> translateDurationUnitsMapJa() {
      return Arrays.stream(DurationUnit.values()).collect(Collectors.toMap(DurationUnit::getValue, unit -> {
         switch (unit) {
            case Weeks:
               return "週";
            case Days:
               return "日";
            case Minutes:
               return "分";
            case Hours:
               return "時";
            case Seconds:
               return "秒";
            default:
               return "";
         }
      }));
   }

   public List<String> translateAbbreviations(@Nullable Language language) {
      switch (language != null ? language : Language.EN) {
         case CS:
            return translateAbbreviationsCs();
         case HU:
            return translateAbbreviationsHu();
         case JA:
            return translateAbbreviationsJa();
         default:
            return translateAbbreviationsEn();
      }
   }

   private List<String> translateAbbreviationsEn() {
      return Arrays.asList("k", "m", "b", "t");
   }

   private List<String> translateAbbreviationsCs() {
      return Arrays.asList("tis.", "mil.", "mld.", "bil.");
   }

   private List<String> translateAbbreviationsHu() {
      return Arrays.asList("E", "M", "Mrd", "T");
   }

   private List<String> translateAbbreviationsJa() {
      return Arrays.asList("千", "百万", "十億", "兆");
   }

   public List<String> translateOrdinals(@Nullable Language language) {
      switch (language != null ? language : Language.EN) {
         case CS:
         case HU:
         case JA:
            return translateOrdinalsDot();
         default:
            return translateOrdinalsEn();
      }
   }

   private List<String> translateOrdinalsEn() {
      return Arrays.asList("st", "nd", "rd", "th");
   }

   private List<String> translateOrdinalsDot() {
      return Arrays.asList(".", ".", ".", ".");
   }

   public String getDefaultDateFormat(@Nullable Language language) {
      switch (language != null ? language : Language.EN) {
         case CS:
         case HU:
            return "DD.MM.YYYY";
         case JA:
         default:
            return "MM/DD/YYYY";
      }
   }

   public String getUnknownTaskName(@Nullable Language language) {
      switch (language != null ? language : Language.EN) {
         case CS:
            return "úkol bez názvu";
         case HU:
            return "feladat név nélkül";
         case JA:
            return "タスク名なし";
         default:
            return "no task name";
      }
   }
}

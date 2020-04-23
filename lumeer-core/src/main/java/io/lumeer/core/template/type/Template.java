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
package io.lumeer.core.template.type;

import io.lumeer.api.model.Language;

import java.util.Calendar;
import java.util.Date;

public abstract class Template {

   public abstract String getOrganizationCode(Language language);

   public abstract String getProjectCode(Language language);

   public abstract Date getRelativeDate(Language language);

   Date createDate(int year, int month, int day) {
      var calendar = Calendar.getInstance();
      calendar.set(year, month, day);
      return calendar.getTime();
   }

   public static Template create(TemplateType templateType) {
      switch (templateType) {
         case PROJ:
            return new ProjectTrackerTemplate();
         case HR:
            return new CandidatesCoordinationTemplate();
         case EDCAL:
            return new EditorialCalendarTemplate();
         case OKR:
            return new OKRTrackingTemplate();
         case CRM:
            return new SalesCRMTemplate();
         case SUPPLY:
            return new SupplyChainManagementTemplate();
         case TASK:
            return new TaskTrackerTemplate();
         case WORK:
            return new WorkTrackerTemplate();
         case BUG:
            return new IssueTrackerTemplate();
         case TIME:
            return new TimeManagementTemplate();
         case SCRUM:
            return new ScrumTemplate();
         case RMTW:
            return new RemoteWorkTemplate();
         case CMTRY:
            return new CemeteryTemplate();
         case ROADM:
            return new ProductRoadmapTemplate();
         case LAUNC:
            return new ProductLaunchTemplate();
         default:
            return null;
      }
   }
}

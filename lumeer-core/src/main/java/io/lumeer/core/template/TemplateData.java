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
package io.lumeer.core.template;

import java.util.Date;

public class TemplateData {
   private String organizationCode;
   private String projectCode;
   private Date dateStart;

   public TemplateData(final String organizationCode, final String projectCode, final Date dateStart) {
      this.organizationCode = organizationCode;
      this.projectCode = projectCode;
      this.dateStart = dateStart;
   }

   public String getOrganizationCode() {
      return organizationCode;
   }

   public String getProjectCode() {
      return projectCode;
   }

   public Date getDateStart() {
      return dateStart;
   }
}

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
package io.lumeer.core.task.executor.request;

public class GenericPrintRequest {

   protected final String organizationCode;
   protected final String projectCode;
   protected final boolean skipPrintDialog;

   public GenericPrintRequest(final String organizationCode, final String projectCode, final boolean skipPrintDialog) {
      this.organizationCode = organizationCode;
      this.projectCode = projectCode;
      this.skipPrintDialog = skipPrintDialog;
   }

   public String getOrganizationCode() {
      return organizationCode;
   }

   public String getProjectCode() {
      return projectCode;
   }

   public boolean isSkipPrintDialog() {
      return skipPrintDialog;
   }
}

/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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
package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class ServiceLimits {

   public static final String SERVICE_LEVEL = "serviceLevel";
   public static final String USERS = "users";
   public static final String PROJECTS = "projects";
   public static final String FILES = "files";
   public static final String DOCUMENTS = "documents";
   public static final String DB_SIZE_MB = "dbSizeMb";
   public static final String VALID_UNTIL = "validUntil";

   public static final ServiceLimits FREE_LIMITS = new ServiceLimits(Payment.ServiceLevel.FREE, 3, 1, 10, 200, -1, null);
   public static final ServiceLimits BASIC_LIMITS = new ServiceLimits(Payment.ServiceLevel.BASIC,99, 99, -1, -1, -1, new Date(0));

   private Payment.ServiceLevel serviceLevel;
   private int users;
   private int projects;
   private int files;
   private int documents;
   private int dbSizeMb;
   private Date validUntil;

   static {
       final Calendar c = Calendar.getInstance();
       c.add(Calendar.MONTH, 3);
       FREE_LIMITS.validUntil = c.getTime();
   }

   @JsonCreator
   public ServiceLimits(@JsonProperty(SERVICE_LEVEL) final Payment.ServiceLevel serviceLevel, @JsonProperty(USERS) final int users,
         @JsonProperty(PROJECTS) final int projects, @JsonProperty(FILES) final int files,
         @JsonProperty(DOCUMENTS) final int documents, @JsonProperty(DB_SIZE_MB) final int dbSizeMb,
         @JsonProperty(VALID_UNTIL) final Date validUntil) {
      this.serviceLevel = serviceLevel;
      this.users = users;
      this.projects = projects;
      this.files = files;
      this.documents = documents;
      this.dbSizeMb = dbSizeMb;
      this.validUntil = validUntil;
   }

   public Payment.ServiceLevel getServiceLevel() {
      return serviceLevel;
   }

   public int getUsers() {
      return users;
   }

   public int getProjects() {
      return projects;
   }

   public int getFiles() {
      return files;
   }

   public int getDocuments() {
      return documents;
   }

   public int getDbSizeMb() {
      return dbSizeMb;
   }

   public Date getValidUntil() {
      return validUntil;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final ServiceLimits that = (ServiceLimits) o;
      return users == that.users &&
            projects == that.projects &&
            files == that.files &&
            documents == that.documents &&
            dbSizeMb == that.dbSizeMb &&
            serviceLevel == that.serviceLevel &&
            Objects.equals(validUntil, that.validUntil);
   }

   @Override
   public int hashCode() {
      return Objects.hash(serviceLevel, users, projects, files, documents, dbSizeMb, validUntil);
   }

   @Override
   public String toString() {
      return "ServiceLimits{" +
            "serviceLevel=" + serviceLevel +
            ", users=" + users +
            ", projects=" + projects +
            ", files=" + files +
            ", documents=" + documents +
            ", dbSizeMb=" + dbSizeMb +
            ", validUntil=" + validUntil +
            '}';
   }
}

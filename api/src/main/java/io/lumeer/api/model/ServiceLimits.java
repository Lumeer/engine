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
package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class ServiceLimits {

   public static final String SERVICE_LEVEL = "serviceLevel";
   public static final String USERS = "users";
   public static final String PROJECTS = "projects";
   public static final String FILES = "files";
   public static final String GROUPS = "groups";
   public static final String DOCUMENTS = "documents";
   public static final String DB_SIZE_MB = "dbSizeMb";
   public static final String FILE_SIZE_MB = "fileSizeMb";
   public static final String AUDIT_DAYS = "auditDays";
   public static final String MAX_CREATED_RECORDS = "maxCreatedRecords";
   public static final String MAX_VIEW_READ_RECORDS = "maxViewReadRecords";
   public static final String AUTOMATION_TIMEOUT = "automattionTimeout";
   public static final String VALID_UNTIL = "validUntil";
   public static final String RULES_PER_COLLECTION = "rulesPerCollection";
   public static final String FUNCTIONS_PER_COLLECTION = "functionsPerCollection";

   public static final ServiceLimits FREE_LIMITS = new ServiceLimits(Payment.ServiceLevel.FREE, 3, 3, 10, 2000, -1, null, 1, 1, false, 10, 14, 50, 1000, 3000);
   public static final ServiceLimits BASIC_LIMITS = new ServiceLimits(Payment.ServiceLevel.BASIC, 99, 99, -1, -1, -1, new Date(0), -1, -1, true, 10, 14, 250, 5000, 5000);

   private Payment.ServiceLevel serviceLevel;
   private int users;
   private int projects;
   private int files;
   private int documents;
   private int dbSizeMb;
   private Date validUntil;
   private int rulesPerCollection;
   private int functionsPerCollection;
   private boolean groups;
   private int fileSizeMb;
   private int auditDays;
   private int maxCreatedRecords;
   private int maxViewReadRecords;
   private int automationTimeout;

   static {
      final Calendar c = Calendar.getInstance();
      c.add(Calendar.MONTH, 3);
      FREE_LIMITS.validUntil = c.getTime();
   }

   @JsonCreator
   public ServiceLimits(@JsonProperty(SERVICE_LEVEL) final Payment.ServiceLevel serviceLevel, @JsonProperty(USERS) final int users,
         @JsonProperty(PROJECTS) final int projects, @JsonProperty(FILES) final int files,
         @JsonProperty(DOCUMENTS) final int documents, @JsonProperty(DB_SIZE_MB) final int dbSizeMb,
         @JsonProperty(VALID_UNTIL) final Date validUntil,
         @JsonProperty(RULES_PER_COLLECTION) final int rulesPerCollection,
         @JsonProperty(FUNCTIONS_PER_COLLECTION) final int functionsPerCollection,
         @JsonProperty(GROUPS) final boolean groups,
         @JsonProperty(FILE_SIZE_MB) final int fileSizeMb, @JsonProperty(AUDIT_DAYS) final int auditDays,
         @JsonProperty(MAX_CREATED_RECORDS) final int maxCreatedRecords,
         @JsonProperty(MAX_VIEW_READ_RECORDS) final int maxViewReadRecords,
         @JsonProperty(AUTOMATION_TIMEOUT) final int automationTimeout) {
      this.serviceLevel = serviceLevel;
      this.users = users;
      this.projects = projects;
      this.files = files;
      this.documents = documents;
      this.dbSizeMb = dbSizeMb;
      this.validUntil = validUntil;
      this.rulesPerCollection = rulesPerCollection;
      this.functionsPerCollection = functionsPerCollection;
      this.groups = groups;
      this.fileSizeMb = fileSizeMb;
      this.auditDays = auditDays;
      this.maxCreatedRecords = maxCreatedRecords;
      this.maxViewReadRecords = maxViewReadRecords;
      this.automationTimeout = automationTimeout;
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

   public int getRulesPerCollection() {
      return rulesPerCollection;
   }

   public int getFunctionsPerCollection() {
      return functionsPerCollection;
   }

   public boolean isGroups() {
      return groups;
   }

   public int getAuditDays() {
      return auditDays;
   }

   public int getFileSizeMb() {
      return fileSizeMb;
   }

   public int getMaxCreatedRecords() {
      return maxCreatedRecords;
   }

   public int getMaxViewReadRecords() {
      return maxViewReadRecords;
   }

   public int getAutomationTimeout() {
      return automationTimeout;
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
            ", rulesPerCollection=" + rulesPerCollection +
            ", functionsPerCollection=" + functionsPerCollection +
            ", groups=" + groups +
            ", fileSizeMb=" + fileSizeMb +
            ", auditDays=" + auditDays +
            ", maxCreatedRecords=" + maxCreatedRecords +
            ", maxViewReadRecords=" + maxViewReadRecords +
            ", automationTimeout=" + automationTimeout +
            '}';
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ServiceLimits that = (ServiceLimits) o;
      return users == that.users && projects == that.projects && files == that.files && documents == that.documents && dbSizeMb == that.dbSizeMb && rulesPerCollection == that.rulesPerCollection && functionsPerCollection == that.functionsPerCollection && groups == that.groups && fileSizeMb == that.fileSizeMb && auditDays == that.auditDays && maxCreatedRecords == that.maxCreatedRecords && maxViewReadRecords == that.maxViewReadRecords && automationTimeout == that.automationTimeout && serviceLevel == that.serviceLevel && Objects.equals(validUntil, that.validUntil);
   }

   @Override
   public int hashCode() {
      return Objects.hash(serviceLevel, users, projects, files, documents, dbSizeMb, validUntil, rulesPerCollection, functionsPerCollection, groups, fileSizeMb, auditDays, maxCreatedRecords, maxViewReadRecords, automationTimeout);
   }

   @JsonIgnore
   public boolean fitsLimits(final ProjectDescription projectDescription) {
      return (files < 0 || projectDescription.getCollections() <= files) &&
            (documents < 0 || projectDescription.getDocuments() <= documents) &&
            (functionsPerCollection < 0 || projectDescription.getMaxFunctionPerResource() <= functionsPerCollection) &&
            (rulesPerCollection < 0 || projectDescription.getMaxRulesPerResource() <= rulesPerCollection);
   }

}

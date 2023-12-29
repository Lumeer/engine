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

import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CollectionPurpose {

   public static final String TYPE = "type";
   public static final String META_DATA = "metaData";

   public static final String META_DUE_DATE_ATTRIBUTE_ID = "dueDateAttributeId";
   public static final String META_ASSIGNEE_ATTRIBUTE_ID = "assigneeAttributeId";
   public static final String META_STATE_ATTRIBUTE_ID = "stateAttributeId";
   public static final String META_OBSERVERS_ATTRIBUTE_ID = "observersAttributeId";
   public static final String META_FINAL_STATES_LIST = "finalStatesList";
   public static final String META_REPEAT_DUE_NOTIFICATIONS = "repeatDueNotifications";

   private CollectionPurposeType type;
   private DataDocument metaData;

   @JsonCreator
   public CollectionPurpose(@JsonProperty("type") final CollectionPurposeType type,
         @JsonProperty("metaData") final DataDocument metaData) {
      this.type = type;
      this.metaData = metaData;
   }

   public CollectionPurposeType getType() {
      return type;
   }

   public void setType(final CollectionPurposeType type) {
      this.type = type;
   }

   public DataDocument getMetaData() {
      return metaData;
   }

   public void setMetaData(final DataDocument metaData) {
      this.metaData = metaData;
   }

   @JsonIgnore
   public String getDueDateAttributeId() {
      return this.notNullMetadata().getString(META_DUE_DATE_ATTRIBUTE_ID);
   }

   public void clearDueDateAttributeId() {
      this.notNullMetadata().remove(META_DUE_DATE_ATTRIBUTE_ID);
   }

   public void setDueDateAttributeId(String value) {
      this.notNullMetadata().append(META_DUE_DATE_ATTRIBUTE_ID, value);
   }

   @JsonIgnore
   public String getAssigneeAttributeId() {
      return this.notNullMetadata().getString(META_ASSIGNEE_ATTRIBUTE_ID);
   }

   public void clearAssigneeAttributeId() {
      this.notNullMetadata().remove(META_ASSIGNEE_ATTRIBUTE_ID);
   }

   public void setAssigneeAttributeId(String value) {
      this.notNullMetadata().append(META_ASSIGNEE_ATTRIBUTE_ID, value);
   }

   @JsonIgnore
   public String getStateAttributeId() {
      return this.notNullMetadata().getString(META_STATE_ATTRIBUTE_ID);
   }

   public void clearStateAttributeId() {
      this.notNullMetadata().remove(META_STATE_ATTRIBUTE_ID);
   }

   public void setStateAttributeId(String value) {
      this.notNullMetadata().append(META_STATE_ATTRIBUTE_ID, value);
   }

   @JsonIgnore
   public String getObserverAttributeId() {
      return this.notNullMetadata().getString(META_OBSERVERS_ATTRIBUTE_ID);
   }

   public void clearObserverAttributeId() {
      this.notNullMetadata().remove(META_OBSERVERS_ATTRIBUTE_ID);
   }

   public void setObserverAttributeId(String value) {
      this.notNullMetadata().append(META_OBSERVERS_ATTRIBUTE_ID, value);
   }

   @JsonIgnore
   public List<Object> getFinalStatesList() {
      return this.notNullMetadata().getArrayList(META_FINAL_STATES_LIST);
   }

   public void clearFinalStatesList() {
      this.notNullMetadata().remove(META_FINAL_STATES_LIST);
   }

   private DataDocument notNullMetadata() {
      if (this.metaData == null) {
         this.metaData = new DataDocument();
      }
      return this.metaData;
   }

   public DataDocument createIfAbsentMetaData() {
      if (metaData == null) {
         this.metaData = new DataDocument();
      }

      return metaData;
   }

   @Override
   public String toString() {
      return "CollectionPurpose{" +
            "type=" + type +
            ", metaData=" + metaData +
            '}';
   }
}

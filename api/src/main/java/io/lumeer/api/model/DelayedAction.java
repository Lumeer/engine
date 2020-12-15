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

import io.lumeer.api.adapter.ZonedDateTimeAdapter;
import io.lumeer.engine.api.data.DataDocument;

import java.time.ZonedDateTime;
import java.util.Objects;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Delayed actions executed in background. Check automatically and regularly.
 */
public class DelayedAction {
   public static final String CHECK_AFTER = "checkAfter";
   public static final String STARTED_PROCESSING = "startedProcessing";
   public static final String COMPLETED = "completed";
   public static final String PROGRESS = "progress";
   public static final String RESOURCE_PATH = "resourcePath";
   public static final String INITIATOR = "initiator";
   public static final String RECEIVER = "receiver";
   public static final String NOTIFICATION_TYPE = "notificationType";
   public static final String NOTIFICATION_CHANNEL = "notificationChannel";
   public static final String CORRELATION_ID = "correlationId";
   public static final String DATA = "data";

   public static final String DATA_ORGANIZATION_ID = "organizationId";
   public static final String DATA_ORGANIZATION_CODE = "organizationCode";
   public static final String DATA_ORGANIZATION_NAME = "organizationName";
   public static final String DATA_ORGANIZATION_ICON = "organizationIcon";
   public static final String DATA_ORGANIZATION_COLOR = "organizationColor";
   public static final String DATA_PROJECT_ID = "projectId";
   public static final String DATA_PROJECT_CODE = "projectCode";
   public static final String DATA_PROJECT_NAME = "projectName";
   public static final String DATA_PROJECT_ICON = "projectIcon";
   public static final String DATA_PROJECT_COLOR = "projectColor";
   public static final String DATA_COLLECTION_ID = "collectionId";
   public static final String DATA_COLLECTION_NAME = "collectionName";
   public static final String DATA_COLLECTION_ICON = "collectionIcon";
   public static final String DATA_COLLECTION_COLOR = "collectionColor";
   public static final String DATA_DOCUMENT_ID = "documentId";
   public static final String DATA_TASK_NAME = "taskName";
   public static final String DATA_TASK_NAME_ATTRIBUTE = "taskNameAttribute";
   public static final String DATA_TASK_DUE_DATE = "taskDueDate";
   public static final String DATA_DUE_DATE_FORMAT = "dueDateFormat";
   public static final String DATA_TASK_STATE = "taskState";
   public static final String DATA_TASK_COMPLETED = "taskCompleted";
   public static final String DATA_ASSIGNEE = "assignee";
   public static final String DATA_COLLECTION_QUERY = "collectionQuery";
   public static final String DATA_DOCUMENT_CURSOR = "documentCursor";
   public static final String DATA_VIEW_CODE = "viewCode";

   private String id;

   /**
    * Timestamp when the action could be executed at soonest.
    */
   @XmlJavaTypeAdapter(ZonedDateTimeAdapter.class)
   private ZonedDateTime checkAfter;

   /**
    * Timestamp of start of processing.
    */
   @XmlJavaTypeAdapter(ZonedDateTimeAdapter.class)
   private ZonedDateTime startedProcessing;

   /**
    * Timestamp of action completion.
    */
   @XmlJavaTypeAdapter(ZonedDateTimeAdapter.class)
   private ZonedDateTime completed;

   /**
    * Reports progress on the action in case it takes a long time.
    */
   private Integer progress;

   /**
    * Path to the resource, depending on the action type. For example organizationId/projectId/collectionId/documentId for tasks.
    */
   private String resourcePath;

   /**
    * User who initiated the action.
    */
   private String initiator;

   /**
    * Receiving user of the action result.
    */
   private String receiver;

   /**
    * What type of notification this action could result to.
    */
   private NotificationType notificationType;

   /**
    * What notification channel should be used.
    */
   private NotificationChannel notificationChannel;

   /**
    * Correlation ID used to notify back the originator for example.
    */
   private String correlationId;

   /**
    * Action data like task summary, due date, state...
    */
   private DataDocument data;

   public String getId() {
      return id;
   }

   public void setId(final String id) {
      this.id = id;
   }

   public ZonedDateTime getCheckAfter() {
      return checkAfter;
   }

   public void setCheckAfter(final ZonedDateTime checkAfter) {
      this.checkAfter = checkAfter;
   }

   public ZonedDateTime getStartedProcessing() {
      return startedProcessing;
   }

   public void setStartedProcessing(final ZonedDateTime startedProcessing) {
      this.startedProcessing = startedProcessing;
   }

   public ZonedDateTime getCompleted() {
      return completed;
   }

   public void setCompleted(final ZonedDateTime completed) {
      this.completed = completed;
   }

   public Integer getProgress() {
      return progress;
   }

   public void setProgress(final Integer progress) {
      this.progress = progress;
   }

   public String getResourcePath() {
      return resourcePath;
   }

   public void setResourcePath(final String resourcePath) {
      this.resourcePath = resourcePath;
   }

   public String getInitiator() {
      return initiator;
   }

   public void setInitiator(final String initiator) {
      this.initiator = initiator;
   }

   public String getReceiver() {
      return receiver;
   }

   public void setReceiver(final String receiver) {
      this.receiver = receiver;
   }

   public NotificationType getNotificationType() {
      return notificationType;
   }

   public void setNotificationType(final NotificationType notificationType) {
      this.notificationType = notificationType;
   }

   public NotificationChannel getNotificationChannel() {
      return notificationChannel;
   }

   public void setNotificationChannel(final NotificationChannel notificationChannel) {
      this.notificationChannel = notificationChannel;
   }

   public String getCorrelationId() {
      return correlationId;
   }

   public void setCorrelationId(final String correlationId) {
      this.correlationId = correlationId;
   }

   public DataDocument getData() {
      return data;
   }

   public void setData(final DataDocument data) {
      this.data = data;
   }

   public DataDocument createIfAbsentData() {
      if (data == null) {
         this.data = new DataDocument();
      }

      return data;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final DelayedAction that = (DelayedAction) o;
      return Objects.equals(id, that.id);
   }

   @Override
   public int hashCode() {
      return Objects.hash(id);
   }

   @Override
   public String toString() {
      return "DelayedAction{" +
            "id='" + id + '\'' +
            ", checkAfter=" + checkAfter +
            ", startedProcessing=" + startedProcessing +
            ", completed=" + completed +
            ", progress=" + progress +
            ", resourcePath='" + resourcePath + '\'' +
            ", initiator='" + initiator + '\'' +
            ", receiver='" + receiver + '\'' +
            ", notificationType=" + notificationType +
            ", notificationChannel=" + notificationChannel +
            ", correlationId=" + correlationId +
            ", data=" + data +
            '}';
   }
}

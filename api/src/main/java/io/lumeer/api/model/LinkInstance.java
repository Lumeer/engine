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

import io.lumeer.api.adapter.ZonedDateTimeAdapter;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class LinkInstance {

   public static final String ID = "id";
   public static final String LINK_TYPE_ID = "linkTypeId";
   public static final String DOCUMENTS_IDS = "documentIds";

   private String id;
   private String linkTypeId;

   @XmlJavaTypeAdapter(ZonedDateTimeAdapter.class)
   private ZonedDateTime creationDate;

   @XmlJavaTypeAdapter(ZonedDateTimeAdapter.class)
   private ZonedDateTime updateDate;

   private String createdBy;
   private String updatedBy;

   private List<String> documentIds;
   private Integer dataVersion;
   private DataDocument data;

   @JsonCreator
   public LinkInstance(@JsonProperty(LINK_TYPE_ID) final String linkTypeId,
         @JsonProperty(DOCUMENTS_IDS) final List<String> documentIds) {
      this.linkTypeId = linkTypeId;
      this.documentIds = documentIds;
      this.data = new DataDocument();
   }

   public LinkInstance(LinkInstance linkInstance) {
      this.id = linkInstance.getId();
      this.linkTypeId = linkInstance.getLinkTypeId();
      this.creationDate = linkInstance.getCreationDate();
      this.updateDate = linkInstance.getUpdateDate();
      this.createdBy = linkInstance.getCreatedBy();
      this.updatedBy = linkInstance.getUpdatedBy();
      this.documentIds = linkInstance.getDocumentIds();
      this.dataVersion = linkInstance.getDataVersion();
      this.data = linkInstance.getData();
   }

   public String getId() {
      return id;
   }

   public void setId(final String id) {
      this.id = id;
   }

   public String getLinkTypeId() {
      return linkTypeId;
   }

   public void setLinkTypeId(final String linkTypeId) {
      this.linkTypeId = linkTypeId;
   }

   public ZonedDateTime getCreationDate() {
      return creationDate;
   }

   public void setCreationDate(final ZonedDateTime creationDate) {
      this.creationDate = creationDate;
   }

   public ZonedDateTime getUpdateDate() {
      return updateDate;
   }

   public void setUpdateDate(final ZonedDateTime updateDate) {
      this.updateDate = updateDate;
   }

   public String getCreatedBy() {
      return createdBy;
   }

   public void setCreatedBy(final String createdBy) {
      this.createdBy = createdBy;
   }

   public String getUpdatedBy() {
      return updatedBy;
   }

   public void setUpdatedBy(final String updatedBy) {
      this.updatedBy = updatedBy;
   }

   public List<String> getDocumentIds() {
      return Collections.unmodifiableList(documentIds);
   }

   public void setDocumentIds(final List<String> documentIds) {
      this.documentIds = documentIds;
   }

   public DataDocument getData() {
      return data;
   }

   public void setData(final DataDocument data) {
      this.data = data;
   }

   public Integer getDataVersion() {
      return dataVersion;
   }

   public void setDataVersion(final Integer dataVersion) {
      this.dataVersion = dataVersion;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof LinkInstance)) {
         return false;
      }
      final LinkInstance that = (LinkInstance) o;
      return Objects.equals(getId(), that.getId());
   }

   @Override
   public int hashCode() {
      return Objects.hash(getId());
   }

   @Override
   public String toString() {
      return "LinkInstance{" +
            "id='" + id + '\'' +
            ", linkTypeId='" + linkTypeId + '\'' +
            ", creationDate=" + creationDate +
            ", updateDate=" + updateDate +
            ", createdBy='" + createdBy + '\'' +
            ", updatedBy='" + updatedBy + '\'' +
            ", documentIds=" + documentIds +
            ", dataVersion=" + dataVersion +
            ", data=" + data +
            '}';
   }
}

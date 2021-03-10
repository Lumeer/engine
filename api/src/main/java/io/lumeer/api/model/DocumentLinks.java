package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class DocumentLinks {

   private final String documentId;

   private final List<String> removedLinkInstancesIds;

   private final List<LinkInstance> createdLinkInstances;

   @JsonCreator
   public DocumentLinks(@JsonProperty("removedLinkInstancesIds") final List<String> removedLinkInstancesIds,
         @JsonProperty("documentId") final String documentId,
         @JsonProperty("createdLinkInstances") final List<LinkInstance> createdLinkInstances) {
      this.documentId = documentId;
      this.removedLinkInstancesIds = removedLinkInstancesIds;
      this.createdLinkInstances = createdLinkInstances;
   }

   public String getDocumentId() {
      return documentId;
   }

   public List<String> getRemovedLinkInstancesIds() {
      return removedLinkInstancesIds != null ? removedLinkInstancesIds : Collections.emptyList();
   }

   public List<LinkInstance> getCreatedLinkInstances() {
      return createdLinkInstances != null ? createdLinkInstances : Collections.emptyList();
   }
}


package io.lumeer.engine.api.event;

import io.lumeer.api.model.LinkInstance;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.stream.Collectors;

public class SetDocumentLinks {

   @JsonIgnore
   private List<LinkInstance> createdLinkInstances;
   @JsonIgnore
   private List<LinkInstance> removedLinkInstances;

   private String documentId;

   private List<String> createdLinkInstancesIds;
   private List<String> removedLinkInstancesIds;

   public SetDocumentLinks(final String documentId, final List<LinkInstance> createdLinkInstances, final List<LinkInstance> removedLinkInstances) {
      this.documentId = documentId;
      this.createdLinkInstances = createdLinkInstances;
      this.createdLinkInstancesIds = createdLinkInstances.stream().map(LinkInstance::getId).collect(Collectors.toList());
      this.removedLinkInstances = removedLinkInstances;
      this.removedLinkInstancesIds = removedLinkInstances.stream().map(LinkInstance::getId).collect(Collectors.toList());
   }

   public List<LinkInstance> getCreatedLinkInstances() {
      return createdLinkInstances;
   }

   public List<LinkInstance> getRemovedLinkInstances() {
      return removedLinkInstances;
   }

   public String getDocumentId() {
      return documentId;
   }

   public List<String> getCreatedLinkInstancesIds() {
      return createdLinkInstancesIds;
   }

   public List<String> getRemovedLinkInstancesIds() {
      return removedLinkInstancesIds;
   }
}

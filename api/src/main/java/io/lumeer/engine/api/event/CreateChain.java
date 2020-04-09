package io.lumeer.engine.api.event;

import io.lumeer.api.model.Document;
import io.lumeer.api.model.LinkInstance;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CreateChain {

   @JsonIgnore
   private List<Document> documents;
   private Set<String> documentsIds;

   @JsonIgnore
   private List<LinkInstance> linkInstances;
   private Set<String> linkInstancesIds;

   public CreateChain(final List<Document> documents, final List<LinkInstance> linkInstances) {
      this.documents = documents;
      this.documentsIds = documents.stream().map(Document::getId).collect(Collectors.toSet());
      this.linkInstances = linkInstances;
      this.linkInstancesIds = linkInstances.stream().map(LinkInstance::getId).collect(Collectors.toSet());
   }

   public List<Document> getDocuments() {
      return documents;
   }

   public List<LinkInstance> getLinkInstances() {
      return linkInstances;
   }

   public Set<String> getDocumentsIds() {
      return documentsIds;
   }

   public Set<String> getLinkInstancesIds() {
      return linkInstancesIds;
   }
}

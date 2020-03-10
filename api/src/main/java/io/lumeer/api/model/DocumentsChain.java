package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DocumentsChain {

   private final List<Document> documents;

   private final List<LinkInstance> linkInstances;

   @JsonCreator
   public DocumentsChain(@JsonProperty("documents") final List<Document> documents,
         @JsonProperty("linkInstances") final List<LinkInstance> linkInstances) {
      this.documents = documents;
      this.linkInstances = linkInstances;
   }

   public List<Document> getDocuments() {
      return documents;
   }

   public List<LinkInstance> getLinkInstances() {
      return linkInstances;
   }
}


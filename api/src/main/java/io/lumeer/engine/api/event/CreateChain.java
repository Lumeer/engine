package io.lumeer.engine.api.event;

import java.util.Set;

public class CreateChain {

   private Set<String> documentsIds;

   private Set<String> linkInstancesIds;

   public CreateChain(final Set<String> documentsIds, final Set<String> linkInstancesIds) {
      this.documentsIds = documentsIds;
      this.linkInstancesIds = linkInstancesIds;
   }

   public Set<String> getDocumentsIds() {
      return documentsIds;
   }

   public Set<String> getLinkInstancesIds() {
      return linkInstancesIds;
   }
}

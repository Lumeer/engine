package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CollectionAttributeFilter extends AttributeFilter {

   private final String collectionId;

   @JsonCreator
   public CollectionAttributeFilter(@JsonProperty("collectionId") final String collectionId,
         @JsonProperty("attributeId") final String attributeId,
         @JsonProperty("operator") final String operator,
         @JsonProperty("value") final Object value) {
      super(attributeId, operator, value);
      this.collectionId = collectionId;
   }

   public String getCollectionId() {
      return collectionId;
   }

}

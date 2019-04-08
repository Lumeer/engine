package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

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

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof CollectionAttributeFilter)) {
         return false;
      }
      if (!super.equals(o)) {
         return false;
      }
      final CollectionAttributeFilter that = (CollectionAttributeFilter) o;
      return Objects.equals(getAttributeId(), that.getAttributeId()) &&
            Objects.equals(getOperator(), that.getOperator()) &&
            Objects.equals(getValue(), that.getValue()) &&
            Objects.equals(getCollectionId(), that.getCollectionId());
   }

   @Override
   public int hashCode() {
      return Objects.hash(super.hashCode(), getAttributeId(), getOperator(), getValue(), getCollectionId());
   }

   @Override
   public String toString() {
      return "CollectionAttributeFilter{" +
            "collectionId='" + getCollectionId() + '\'' +
            ", attributeId='" + getAttributeId() + '\'' +
            ", operator='" + getOperator() + '\'' +
            ", value=" + getValue() +
            '}';
   }
}

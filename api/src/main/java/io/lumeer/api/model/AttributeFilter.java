package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class AttributeFilter {

   private final String collectionId;
   private final String attributeId;
   private final String operator;
   private Object value;

   @JsonCreator
   public AttributeFilter(@JsonProperty("collectionId") final String collectionId,
         @JsonProperty("attributeId") final String attributeId,
         @JsonProperty("operator") final String operator,
         @JsonProperty("value") final Object value) {
      this.collectionId = collectionId;
      this.attributeId = attributeId;
      this.operator = operator;
      this.value = value;
   }

   public String getCollectionId() {
      return collectionId;
   }

   public String getAttributeId() {
      return attributeId;
   }

   public String getOperator() {
      return operator;
   }

   public Object getValue() {
      return value;
   }

   public void setValue(final Object value) {
      this.value = value;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof AttributeFilter)) {
         return false;
      }
      final AttributeFilter that = (AttributeFilter) o;
      return Objects.equals(getCollectionId(), that.getCollectionId()) &&
            Objects.equals(getAttributeId(), that.getAttributeId()) &&
            Objects.equals(getOperator(), that.getOperator()) &&
            Objects.equals(getValue(), that.getValue());
   }

   @Override
   public int hashCode() {
      return Objects.hash(getCollectionId(), getAttributeId(), getOperator(), getValue());
   }

   @Override
   public String toString() {
      return "AttributeFilter{" +
            "collectionId='" + collectionId + '\'' +
            ", attributeId='" + attributeId + '\'' +
            ", operator='" + operator + '\'' +
            ", value='" + value + '\'' +
            '}';
   }
}

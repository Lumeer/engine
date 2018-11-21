package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class AttributeFilter {

   private final String attributeId;
   private final String operator;
   private final String value;

   @JsonCreator
   public AttributeFilter(@JsonProperty("attributeId") final String attributeId,
         @JsonProperty("operator") final String operator,
         @JsonProperty("value") final String value) {
      this.attributeId = attributeId;
      this.operator = operator;
      this.value = value;
   }

   public String getAttributeId() {
      return attributeId;
   }

   public String getOperator() {
      return operator;
   }

   public String getValue() {
      return value;
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
      return Objects.equals(getAttributeId(), that.getAttributeId()) &&
            Objects.equals(getOperator(), that.getOperator()) &&
            Objects.equals(getValue(), that.getValue());
   }

   @Override
   public int hashCode() {
      return Objects.hash(getAttributeId(), getOperator(), getValue());
   }

   @Override
   public String toString() {
      return "AttributeFilter{" +
            "attributeId='" + attributeId + '\'' +
            ", operator='" + operator + '\'' +
            ", value='" + value + '\'' +
            '}';
   }
}

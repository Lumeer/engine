package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class LinkAttributeFilter {

   private final String linkTypeId;
   private final String attributeId;
   private final String operator;
   private Object value;

   @JsonCreator
   public LinkAttributeFilter(@JsonProperty("linkTypeId") final String linkTypeId,
         @JsonProperty("attributeId") final String attributeId,
         @JsonProperty("operator") final String operator,
         @JsonProperty("value") final Object value) {
      this.linkTypeId = linkTypeId;
      this.attributeId = attributeId;
      this.operator = operator;
      this.value = value;
   }

   public String getLinkTypeId() {
      return linkTypeId;
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
      if (!(o instanceof LinkAttributeFilter)) {
         return false;
      }
      final LinkAttributeFilter that = (LinkAttributeFilter) o;
      return Objects.equals(getLinkTypeId(), that.getLinkTypeId()) &&
            Objects.equals(getAttributeId(), that.getAttributeId()) &&
            Objects.equals(getOperator(), that.getOperator()) &&
            Objects.equals(getValue(), that.getValue());
   }

   @Override
   public int hashCode() {
      return Objects.hash(getLinkTypeId(), getAttributeId(), getOperator(), getValue());
   }

   @Override
   public String toString() {
      return "AttributeFilter{" +
            "linkTypeId='" + linkTypeId + '\'' +
            ", attributeId='" + attributeId + '\'' +
            ", operator='" + operator + '\'' +
            ", value='" + value + '\'' +
            '}';
   }
}

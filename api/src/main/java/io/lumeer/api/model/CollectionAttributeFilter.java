package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CollectionAttributeFilter extends AttributeFilter {

   private final String collectionId;

   @JsonCreator
   public CollectionAttributeFilter(@JsonProperty("collectionId") final String collectionId,
         @JsonProperty("attributeId") final String attributeId,
         @JsonProperty("condition") final String condition,
         @JsonProperty("conditionValues") final List<ConditionValue> conditionValues) {
      super(attributeId, condition, conditionValues);
      this.collectionId = collectionId;
   }

   public static CollectionAttributeFilter createFromValue(final String collectionId, final String attributeId, final String condition, final Object value) {
      return new CollectionAttributeFilter(collectionId, attributeId, condition, Collections.singletonList(new ConditionValue(value)));
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
            Objects.equals(getCondition(), that.getCondition()) &&
            Objects.equals(getValue(), that.getValue()) &&
            Objects.equals(getCollectionId(), that.getCollectionId());
   }

   @Override
   public int hashCode() {
      return Objects.hash(super.hashCode(), getAttributeId(), getCondition(), getValue(), getCollectionId());
   }

   @Override
   public String toString() {
      return "CollectionAttributeFilter{" +
            "collectionId='" + getCollectionId() + '\'' +
            ", attributeId='" + getAttributeId() + '\'' +
            ", condition='" + getCondition() + '\'' +
            ", values=" + getConditionValues() +
            '}';
   }
}

package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CollectionAttributeFilter extends AttributeFilter {

   private final String collectionId;

   @JsonCreator
   public CollectionAttributeFilter(@JsonProperty("collectionId") final String collectionId,
         @JsonProperty("attributeId") final String attributeId,
         @JsonProperty("condition") final ConditionType condition,
         @JsonProperty("conditionValues") final List<ConditionValue> conditionValues) {
      super(attributeId, condition, conditionValues);
      this.collectionId = collectionId;
   }

   public CollectionAttributeFilter(CollectionAttributeFilter filter) {
      this(filter.getCollectionId(), filter.getAttributeId(), filter.getCondition(), filter.getConditionValues());
   }

   public static CollectionAttributeFilter createFromValues(final String collectionId, final String attributeId, final ConditionType condition, final Object... values) {
      return new CollectionAttributeFilter(collectionId, attributeId, condition, Arrays.stream(values).map(ConditionValue::new).collect(Collectors.toList()));
   }

   public static CollectionAttributeFilter createFromTypes(final String collectionId, final String attributeId, final ConditionType condition, final String... types) {
      return new CollectionAttributeFilter(collectionId, attributeId, condition, Arrays.stream(types).map(c -> new ConditionValue(c, null)).collect(Collectors.toList()));
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
            Objects.equals(getConditionValues(), that.getConditionValues()) &&
            Objects.equals(getCollectionId(), that.getCollectionId());
   }

   @Override
   public int hashCode() {
      return Objects.hash(super.hashCode(), getAttributeId(), getCondition(), getConditionValues(), getCollectionId());
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

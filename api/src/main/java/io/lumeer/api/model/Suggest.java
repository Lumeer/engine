package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Set;

public class Suggest {

   private final String text;
   private final SuggestionType type;
   private final Set<String> priorityCollectionIds;

   @JsonCreator
   public Suggest(@JsonProperty("text") final String text,
         @JsonProperty("type") final String type,
         @JsonProperty("priorityCollectionIds") final Set<String> priorityCollectionIds) {
      this.text = text;
      this.type = SuggestionType.fromString(type);
      this.priorityCollectionIds = priorityCollectionIds != null ? priorityCollectionIds : Collections.emptySet();
   }

   public Suggest(final String text, final SuggestionType type) {
      this(text, type.toString(), null);
   }

   public String getText() {
      return text;
   }

   public SuggestionType getType() {
      return type;
   }

   public Set<String> getPriorityCollectionIds() {
      return priorityCollectionIds;
   }
}

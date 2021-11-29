package io.lumeer.api.model.viewConfig;

import io.lumeer.api.model.Perspective;
import io.lumeer.api.model.View;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

public class FormConfig {

   private final String collectionId;

   @SuppressWarnings("unchecked")
   public FormConfig(View view) {
      Map<String, Object> formConfig = Collections.emptyMap();
      var configMap = (Map<String, Object>) view.getConfig();
      if (configMap != null) {
         formConfig = (Map<String, Object>) Objects.requireNonNullElse(configMap.get(Perspective.Form.getValue()), Collections.emptyMap());
      }

      collectionId = (String) formConfig.get("collectionId");
   }

   @Nullable
   public String getCollectionId() {
      return collectionId;
   }
}

/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 - 2017 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package io.lumeer.engine.api;

/**
 * Lumeer constants.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public final class LumeerConst {

   public static final String LUMEER_VERSION = "1.0";
   public static final String METADATA_VERSION_KEY = "_metadata-version";

   private LumeerConst() {
      // we do not want any instances to be created
      throw new UnsupportedOperationException(String.format("Creation of %s is forbidden.", this.getClass().getCanonicalName()));
   }

   public class LINKING {
      public static final String PREFIX = "_linking";

      public class MAIN_TABLE {
         public static final String NAME = "_system_linking";
         public static final String ATTR_COL1 = "collection1";
         public static final String ATTR_COL2 = "collection2";
         public static final String ATTR_COL_NAME = "collection_name";
         public static final String ATTR_COUNT = "count";
      }

      public class LINKING_TABLE {
         public static final String ATTR_DOC1 = "id_doc1";
         public static final String ATTR_DOC2 = "id_doc2";
      }
   }
}

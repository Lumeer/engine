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
package io.lumeer.engine.api.exception;

/**
 * Thrown in case metadata document does not exist, or when given key is not found in metadata document.
 *
 * @author <a href="mailto:alica.kacengova@gmail.com">Alica Kačengová</a>
 */
public class CollectionMetadataDocumentNotFoundException extends DbException {

   public CollectionMetadataDocumentNotFoundException(final String message) {
      super(message);
   }

   public CollectionMetadataDocumentNotFoundException(final String message, final Throwable cause) {
      super(message, cause);
   }

   public CollectionMetadataDocumentNotFoundException(final Throwable cause) {
      super(cause);
   }
}

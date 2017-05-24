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
package io.lumeer.engine.rest.dao;

import io.lumeer.engine.api.LumeerConst;
import io.lumeer.engine.api.data.DataDocument;

/**
 * DTO object for Organization
 */
public class Organization {

   private String name;
   private String code;
   private String icon;
   private String color;

   public Organization(final DataDocument dataDocument){
      this.name = dataDocument.getString(LumeerConst.Organization.ATTR_ORG_NAME);
      this.code = dataDocument.getString(LumeerConst.Organization.ATTR_ORG_CODE);
      this.icon = dataDocument.getString(LumeerConst.Organization.ATTR_META_ICON);
      this.color = dataDocument.getString(LumeerConst.Organization.ATTR_META_COLOR);
   }

   public String getName() {
      return name;
   }

   public void setName(final String name) {
      this.name = name;
   }

   public String getCode() {
      return code;
   }

   public void setCode(final String code) {
      this.code = code;
   }

   public String getIcon() {
      return icon;
   }

   public void setIcon(final String icon) {
      this.icon = icon;
   }

   public String getColor() {
      return color;
   }

   public void setColor(final String color) {
      this.color = color;
   }
}

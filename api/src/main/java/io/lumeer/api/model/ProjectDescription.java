package io.lumeer.api.model;/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class ProjectDescription {

   private final long collections;
   private final long documents;

   private final long maxFunctionPerResource;
   private final long maxRulesPerResource;

   public ProjectDescription(final long collections, final long documents, final long maxFunctionPerResource, final long maxRulesPerResource) {
      this.collections = collections;
      this.documents = documents;
      this.maxFunctionPerResource = maxFunctionPerResource;
      this.maxRulesPerResource = maxRulesPerResource;
   }

   public long getCollections() {
      return collections;
   }

   public long getDocuments() {
      return documents;
   }

   public long getMaxFunctionPerResource() {
      return maxFunctionPerResource;
   }

   public long getMaxRulesPerResource() {
      return maxRulesPerResource;
   }

   @Override
   public String toString() {
      return "ProjectDescription{" +
            "collections=" + collections +
            ", documents=" + documents +
            ", maxFunctionPerResource=" + maxFunctionPerResource +
            ", maxRulesPerResource=" + maxRulesPerResource +
            '}';
   }
}

/*
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
package io.lumeer.api.model;

import io.lumeer.api.model.common.WithId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Sequence implements WithId {

   public static final String ID = "id";
   public static final String NAME = "name";
   public static final String SEQ = "seq";


   private String id;
   private String name;
   private int seq;

   @JsonCreator
   public Sequence(@JsonProperty(NAME) final String name, @JsonProperty(SEQ) final int seq) {
      this.id = id;
      this.name = name;
      this.seq = seq;
   }

   public String getId() {
      return id;
   }

   public void setId(final String id) {
      this.id = id;
   }

   public String getName() {
      return name;
   }

   public void setName(final String name) {
      this.name = name;
   }

   public int getSeq() {
      return seq;
   }

   public void setSeq(final int seq) {
      this.seq = seq;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final Sequence sequence = (Sequence) o;
      return Objects.equals(id, sequence.id);
   }

   @Override
   public int hashCode() {
      return Objects.hash(id);
   }

   @Override
   public String toString() {
      return "Sequence{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", seq=" + seq +
            '}';
   }
}

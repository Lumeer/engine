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
package io.lumeer.api.model.function;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Function {

   public static final String JS = "js";
   public static final String XML = "xml";
   public static final String ERROR_REPORT = "errorReport";
   public static final String TIMESTAMP = "timestamp";
   public static final String EDITABLE = "editable";

   private String js;
   private String xml;
   private String errorReport;
   private long timestamp;
   private boolean editable;

   @JsonCreator
   public Function(@JsonProperty(JS) final String js, @JsonProperty(XML) final String xml, @JsonProperty(ERROR_REPORT) final String errorReport, @JsonProperty(TIMESTAMP) final long timestamp, @JsonProperty(EDITABLE) final boolean editable) {
      this.js = js;
      this.xml = xml;
      this.errorReport = errorReport;
      this.timestamp = timestamp;
      this.editable = editable;
   }

   public String getJs() {
      return js;
   }

   public void setJs(final String js) {
      this.js = js;
   }

   public String getXml() {
      return xml;
   }

   public void setXml(final String xml) {
      this.xml = xml;
   }

   public String getErrorReport() {
      return errorReport;
   }

   public void setErrorReport(final String errorReport) {
      this.errorReport = errorReport;
   }

   public long getTimestamp() {
      return timestamp;
   }

   public void setTimestamp(final long timestamp) {
      this.timestamp = timestamp;
   }

   public boolean isEditable() {
      return editable;
   }

   public void setEditable(final boolean editable) {
      this.editable = editable;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof Function)) {
         return false;
      }
      final Function function = (Function) o;
      return getTimestamp() == function.getTimestamp() &&
            isEditable() == function.isEditable() &&
            Objects.equals(getJs(), function.getJs()) &&
            Objects.equals(getXml(), function.getXml()) &&
            Objects.equals(getErrorReport(), function.getErrorReport());
   }

   @Override
   public int hashCode() {
      return Objects.hash(getJs(), getXml(), getErrorReport(), getTimestamp(), isEditable());
   }

   @Override
   public String toString() {
      return "Function{" +
            "js='" + js + '\'' +
            ", xml='" + xml + '\'' +
            ", errorReport='" + errorReport + '\'' +
            ", timestamp=" + timestamp +
            ", editable=" + editable +
            '}';
   }
}

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
package io.lumeer.engine.api.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single data record.
 *
 * @author <a href="mailto:kubedo8@gmail.com">Jakub Rodák</a>
 */
public class DataDocument extends LinkedHashMap<String, Object> {

   /**
    * Name of the document attribute that carries its id.
    */
   public static final String ID = "_id";
   public static final String KEY_SEPARATOR = ".";

   public DataDocument() {
      super(); // this is done automatically, but looks better than an empty constructor body ;-)
   }

   public DataDocument(final Map<String, Object> data) {
      super(data);
   }

   public DataDocument(final String key, final Object value) {
      super();
      put(key, value);
   }

   /**
    * Puts the key and value in the document and returns this.
    *
    * @param key
    *       The key to set.
    * @param value
    *       The value to set in the key.
    * @return Instance of this.
    */
   public DataDocument append(final String key, final Object value) {
      put(key, value);
      return this;
   }

   /**
    * Gets the document id.
    *
    * @return The document id.
    */
   public String getId() {
      return getString(ID);
   }

   /**
    * Sets the document id.
    *
    * @param id
    *       The document id.
    */
   public void setId(final String id) {
      put(ID, id);
   }

   /**
    * Gets the value of the given key as an Integer.
    *
    * @param key
    *       the key
    * @return the value as an integer, which may be null
    * @throws java.lang.ClassCastException
    *       if the value is not an integer
    * @throws java.lang.NullPointerException
    *       if key nested path is not valid
    */
   public Integer getInteger(final String key) {
      return (Integer) getObject(key);
   }

   /**
    * Gets the value of the given key as an Integer.
    *
    * @param key
    *       the key
    * @param defaultValue
    *       what to return if the value is null
    * @return the value as an integer, which may be null
    * @throws java.lang.ClassCastException
    *       if the value is not an integer
    * @throws java.lang.NullPointerException
    *       if key nested path is not valid
    */
   public Integer getInteger(final String key, final int defaultValue) {
      Object value = getObject(key);
      return value == null ? defaultValue : (Integer) value;
   }

   /**
    * Gets the value of the given key as a Long.
    *
    * @param key
    *       the key
    * @return the value as a long, which may be null
    * @throws java.lang.ClassCastException
    *       if the value is not an long
    * @throws java.lang.NullPointerException
    *       if key nested path is not valid
    */
   public Long getLong(final String key) {
      return (Long) getObject(key);
   }

   /**
    * Gets the value of the given key as a Long.
    *
    * @param key
    *       the key
    * @param defaultValue
    *       what to return if the value is null
    * @return the value as a long, which may be null
    * @throws java.lang.ClassCastException
    *       if the value is not a long
    * @throws java.lang.NullPointerException
    *       if key nested path is not valid
    */
   public Long getLong(final String key, final long defaultValue) {
      Object value = getObject(key);
      return value == null ? defaultValue : (Long) value;
   }

   /**
    * Gets the value of the given key as a Double.
    *
    * @param key
    *       the key
    * @return the value as a double, which may be null
    * @throws java.lang.ClassCastException
    *       if the value is not an double
    * @throws java.lang.NullPointerException
    *       if key nested path is not valid
    */
   public Double getDouble(final String key) {
      return (Double) getObject(key);
   }

   /**
    * Gets the value of the given key as a Double.
    *
    * @param key
    *       the key
    * @param defaultValue
    *       what to return if the value is null
    * @return the value as a double, which may be null
    * @throws java.lang.ClassCastException
    *       if the value is not a double
    * @throws java.lang.NullPointerException
    *       if key nested path is not valid
    */
   public Double getDouble(final String key, final double defaultValue) {
      Object value = getObject(key);
      return value == null ? defaultValue : (Double) value;
   }

   /**
    * Gets the value of the given key as a String.
    *
    * @param key
    *       the key
    * @return the value as a String, which may be null
    * @throws java.lang.ClassCastException
    *       if the value is not a String
    * @throws java.lang.NullPointerException
    *       if key nested path is not valid
    */
   public String getString(final String key) {
      return (String) getObject(key);
   }

   /**
    * Gets the value of the given key as a String.
    *
    * @param key
    *       the key
    * @param defaultValue
    *       what to return if the value is null
    * @return the value as a String, which may be null
    * @throws java.lang.ClassCastException
    *       if the value is not a String
    * @throws java.lang.NullPointerException
    *       if key nested path is not valid
    */
   public String getString(final String key, final String defaultValue) {
      Object value = getObject(key);
      return value == null ? defaultValue : (String) value;
   }

   /**
    * Gets the value of the given key as a Boolean.
    *
    * @param key
    *       the key
    * @return the value as a double, which may be null
    * @throws java.lang.ClassCastException
    *       if the value is not an boolean
    * @throws java.lang.NullPointerException
    *       if key nested path is not valid
    */
   public Boolean getBoolean(final String key) {
      return (Boolean) getObject(key);
   }

   /**
    * Gets the value of the given key as a primitive boolean.
    *
    * @param key
    *       the key
    * @param defaultValue
    *       what to return if the value is null
    * @return the value as a double, which may be null
    * @throws java.lang.ClassCastException
    *       if the value is not an boolean
    * @throws java.lang.NullPointerException
    *       if key nested path is not valid
    */
   public boolean getBoolean(final String key, final boolean defaultValue) {
      Object value = getObject(key);
      return value == null ? defaultValue : (Boolean) value;
   }

   /**
    * Gets the value of the given key as a Date.
    *
    * @param key
    *       the key
    * @return the value as a Date, which may be null
    * @throws java.lang.ClassCastException
    *       if the value is not a Date
    * @throws java.lang.NullPointerException
    *       if key nested path is not valid
    */
   public Date getDate(final String key) {
      return (Date) getObject(key);
   }

   /**
    * Gets the value of the given key as a typed ArrayList
    *
    * @param key
    *       the key
    * @return the value as a typed ArrayList, or null
    * @throws java.lang.NullPointerException
    *       if key nested path is not valid
    */
   public <T> ArrayList<T> getArrayList(final String key, final Class<T> cl) {
      List l = (List) getObject(key);
      if (l != null) {
         if (l.isEmpty()) {
            return new ArrayList<T>();
         } else if (l.get(0).getClass() == cl) {
            return (ArrayList<T>) l;
         }
      }
      return new ArrayList<T>();
   }

   /**
    * Gets the value of the given key as a DataDocument
    *
    * @param key
    *       the key
    * @return the value as a DataDocument
    * @throws java.lang.ClassCastException
    *       if the value is not a DataDocument
    * @throws java.lang.NullPointerException
    *       if key nested path is not valid
    */
   public DataDocument getDataDocument(final String key) {
      if (key.contains(KEY_SEPARATOR)) {
         String[] keys = key.split(KEY_SEPARATOR);
         DataDocument doc = this;
         for (final String key1 : keys) {
            doc = (DataDocument) doc.get(key1);
         }
         return doc;
      } else {
         return (DataDocument) get(key);
      }
   }

   /**
    * Gets the value of the given key as a Object
    *
    * @param key
    *       the key
    * @return the value as a Object
    * @throws java.lang.NullPointerException
    *       if key nested path is not valid
    */
   public Object getObject(String key) {
      if (key.contains(KEY_SEPARATOR)) {
         int lastIx = key.lastIndexOf(KEY_SEPARATOR);
         return getDataDocument(key.substring(0, lastIx)).get(key.substring(lastIx + 1));
      } else {
         return get(key);
      }
   }

   @Override
   public int hashCode() {
      String id = getId();

      if (id == null) {
         return super.hashCode();
      } else {
         return id.hashCode();
      }
   }

   @Override
   public boolean equals(Object compared) {
      if (compared == null) {
         return false;
      }

      if (compared instanceof DataDocument) {
         DataDocument comparedDoc = (DataDocument) compared;

         if (this.getId() == null && comparedDoc.getId() == null) {
            return true;
         }

         return this.getId().equals(comparedDoc.getId());
      }

      return false;
   }

   @Override
   public String toString() {
      StringBuilder result = new StringBuilder("DataDocument: ");
      this.forEach((key, value) -> result.append(key).append(": ").append(value).append(", "));
      return result.toString();
   }
}

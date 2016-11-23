/*
 * -----------------------------------------------------------------------\
 * Lumeer
 *  
 * Copyright (C) 2016 the original author or authors.
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
package io.lumeer.engine.api.data;

import java.util.Date;
import java.util.LinkedHashMap;
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
   public static final String DOCUMENT_ID_ATTR = "_id";

   public DataDocument() {
      super(); // this is done automatically, but looks better than an empty constructor body ;-)
   }

   public DataDocument(final Map<String, Object> data) {
      super(data);
   }

   /**
    * Gets the document id.
    * @return The document id.
    */
   public String getId() {
      return getString(DOCUMENT_ID_ATTR);
   }

   /**
    * Gets the value of the given key as an Integer.
    *
    * @param key
    *       the key
    * @return the value as an integer, which may be null
    * @throws java.lang.ClassCastException
    *       if the value is not an integer
    */
   public Integer getInteger(final String key) {
      return (Integer) get(key);
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
    */
   public Integer getInteger(final String key, final int defaultValue) {
      Object value = get(key);
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
    */
   public Long getLong(final String key) {
      return (Long) get(key);
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
    */
   public Long getLong(final String key, final long defaultValue) {
      Object value = get(key);
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
    */
   public Double getDouble(final String key) {
      return (Double) get(key);
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
    */
   public Double getDouble(final String key, final double defaultValue) {
      Object value = get(key);
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
    */
   public String getString(final String key) {
      return (String) get(key);
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
    */
   public String getString(final String key, final String defaultValue) {
      Object value = get(key);
      return value == null ? defaultValue : (String) value;
   }

   /**
    * Gets the value of the given key as a Boolean.
    *
    * @param key
    *       the key
    * @return the value as a double, which may be null
    * @throws java.lang.ClassCastException
    *       if the value is not an double
    */
   public Boolean getBoolean(final String key) {
      return (Boolean) get(key);
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
    *       if the value is not an double
    */
   public boolean getBoolean(final String key, final boolean defaultValue) {
      Object value = get(key);
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
    */
   public Date getDate(final String key) {
      return (Date) get(key);
   }

}

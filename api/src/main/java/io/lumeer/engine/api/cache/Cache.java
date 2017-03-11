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
package io.lumeer.engine.api.cache;

import java.util.function.Function;

/**
 * Cache of values of given type. It is possible to lock on particular cache entry.
 *
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public interface Cache<T> {

   String DEFAULT_ENTRY_KEY = "Cache.DEFAULT_KEY";

   /**
    * Gets a value from the cache.
    *
    * @param key
    *       Entry key.
    * @return Entry value.
    */
   T get(final String key);

   /**
    * Gets the default value.
    *
    * @return The default entry value.
    */
   default T get() {
      return get(DEFAULT_ENTRY_KEY);
   }

   /**
    * Gets a value from the cache or computes a new value when it was null.
    *
    * @param key
    *       Entry key.
    * @param fce
    *       Function to compute a new value.
    * @return Entry value.
    */
   T computeIfAbsent(final String key, final Function<String, T> fce);

   /**
    * Gets the default value or computes, sets and gets a new value if previously absent (was null).
    *
    * @param fce
    *       Function to compute a new value.
    * @return Original or new value if original was absent (was null).
    */
   default T computeIfAbsent(final Function<String, T> fce) {
      return computeIfAbsent(DEFAULT_ENTRY_KEY, fce);
   }

   /**
    * Stores a value to the cache.
    *
    * @param key
    *       Entry key.
    * @param t
    *       Entry value.
    */
   void set(final String key, final T t);

   /**
    * Sets the default entry value.
    *
    * @param t
    *       The default value to store.
    */
   default void set(final T t) {
      set(DEFAULT_ENTRY_KEY, t);
   }

   /**
    * Removes and returns the given entry.
    *
    * @param key
    *       Entry key.
    * @return Original entry value.
    */
   T remove(final String key);

   /**
    * Waits to obtain lock on the given entry.
    *
    * @param key
    *       Entry key.
    */
   void lock(final String key);

   /**
    * Unlocks a lock previously obteined on the given key.
    *
    * @param key
    *       Entry key.
    */
   void unlock(final String key);

   /**
    * Tries to obtain lock on the given key.
    *
    * @param key
    *       Entry key.
    * @return True if and only if a key was obtained.
    */
   boolean tryLock(final String key);

   /**
    * Clears the cache including all locks.
    */
   void clear();
}

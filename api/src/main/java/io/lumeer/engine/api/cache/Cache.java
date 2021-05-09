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
package io.lumeer.engine.api.cache;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Cache of values of given type. It is possible to lock on particular cache entry.
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

   /**
    * Gets the value stream.
    * @return Stream of values.
    */
   Stream<T> stream();
}

/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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
package io.lumeer.cache.simple;

import io.lumeer.engine.api.cache.Cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class SimpleCache<T> implements Cache<T> {

   private Map<String, T> cache = new ConcurrentHashMap<>();

   private Map<String, Lock> locks = new ConcurrentHashMap<>();

   @Override
   public T get(final String key) {
      return cache.get(key);
   }

   @Override
   public T computeIfAbsent(final String key, final Function<String, T> fce) {
      return cache.computeIfAbsent(key, fce);
   }

   @Override
   public void set(final String key, final T t) {
      cache.put(key, t);
   }

   @Override
   public T remove(final String key) {
      return cache.remove(key);
   }

   @Override
   public void lock(final String key) {
      final Lock l = locks.computeIfAbsent(key, k -> new ReentrantLock());
      l.lock();
   }

   @Override
   public void unlock(final String key) {
      final Lock l = locks.get(key);

      if (l != null) {
         l.unlock();
      }
   }

   @Override
   public boolean tryLock(final String key) {
      final Lock l = locks.computeIfAbsent(key, k -> new ReentrantLock());

      return l.tryLock();
   }

   @Override
   public void clear() {
      cache.clear();
      locks.clear();
   }
}

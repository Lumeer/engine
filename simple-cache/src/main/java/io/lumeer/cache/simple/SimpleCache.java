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

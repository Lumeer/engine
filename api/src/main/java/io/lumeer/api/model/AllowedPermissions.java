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

public class AllowedPermissions {

   private final Boolean read;
   private final Boolean write;
   private final Boolean manage;
   private final Boolean readWithView;
   private final Boolean writeWithView;
   private final Boolean manageWithView;

   public AllowedPermissions(final Boolean read, final Boolean write, final Boolean manage, final Boolean readWithView, final Boolean writeWithView, final Boolean manageWithView) {
      this.read = read;
      this.write = write;
      this.manage = manage;
      this.readWithView = readWithView;
      this.writeWithView = writeWithView;
      this.manageWithView = manageWithView;
   }

   public AllowedPermissions(final Boolean read, final Boolean write, final Boolean manage) {
      this.read = read;
      this.write = write;
      this.manage = manage;
      this.readWithView = read;
      this.writeWithView = write;
      this.manageWithView = manage;
   }

   public static AllowedPermissions getAllAllowed() {
      return new AllowedPermissions(true, true, true);
   }

   public Boolean getRead() {
      return read;
   }

   public Boolean getWrite() {
      return write;
   }

   public Boolean getManage() {
      return manage;
   }

   public Boolean getReadWithView() {
      return readWithView;
   }

   public Boolean getWriteWithView() {
      return writeWithView;
   }

   public Boolean getManageWithView() {
      return manageWithView;
   }
}

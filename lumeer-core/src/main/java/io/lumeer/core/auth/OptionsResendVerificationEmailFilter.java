package io.lumeer.core.auth;/*
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

import java.io.Serializable;
import javax.enterprise.context.SessionScoped;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SessionScoped
public class OptionsResendVerificationEmailFilter implements AuthFilter, Serializable {

   @Override
   public void init(final FilterConfig filterConfig) {
   }

   @Override
   public FilterResult doFilter(final HttpServletRequest req, final HttpServletResponse res) {
      if (req.getMethod().equals("OPTIONS")) {
         if (req.getPathInfo() != null && req.getPathInfo().startsWith("/users/current/resend")) {
            res.setStatus(HttpServletResponse.SC_OK);
            return FilterResult.BREAK;
         }
      }

      return FilterResult.CONTINUE;
   }
}

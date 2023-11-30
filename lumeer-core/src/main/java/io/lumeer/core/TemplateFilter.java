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
package io.lumeer.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

//@WebFilter(urlPatterns = "/*")
public class TemplateFilter implements Filter {

   private ServletContext servletContext;

   @Override
   public void init(final FilterConfig filterConfig) {
      servletContext = filterConfig.getServletContext();
   }

   @Override
   public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
      final HttpServletRequest req = (HttpServletRequest) request;
      final HttpServletResponse res = (HttpServletResponse) response;

      if (req.getServletPath() != null && req.getServletPath().startsWith("/template/")) {
         try (
            var rs = servletContext.getResourceAsStream("/index.html");
            var bais = new ByteArrayInputStream(rs.readAllBytes())
         ) {
            bais.transferTo(res.getOutputStream());
         }

         res.setStatus(HttpServletResponse.SC_OK);
         res.flushBuffer();

         return;
      }

      chain.doFilter(request, response);
   }
}

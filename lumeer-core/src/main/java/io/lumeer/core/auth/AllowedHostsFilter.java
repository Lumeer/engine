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

import io.lumeer.core.facade.ConfigurationFacade;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SessionScoped
public class AllowedHostsFilter implements AuthFilter, Serializable {

   @Inject
   private ConfigurationFacade configurationFacade;

   private Set<String> allowedHosts = new HashSet<String>();

   private boolean initialized = false;

   @Override
   public void init(final FilterConfig filterConfig) throws ServletException {
      if (!initialized && System.getenv("SKIP_SECURITY") == null) {
         final Optional<String> allowedHostsConfig = configurationFacade.getConfigurationString("allowed_hosts");
         allowedHostsConfig.ifPresent(s -> allowedHosts = Arrays.asList(s.split(",")).stream().map(String::trim).collect(Collectors.toSet()));
         initialized = true;
      }
   }

   @Override
   public FilterResult doFilter(final HttpServletRequest req, final HttpServletResponse res) throws IOException, ServletException {
      if (!allowedHost(req, res)) {
         res.sendError(HttpServletResponse.SC_FORBIDDEN, "Unknown host");
         return FilterResult.BREAK;
      }

      return FilterResult.CONTINUE;
   }

   private boolean allowedHost(HttpServletRequest req, HttpServletResponse res) {
      if (allowedHosts == null || allowedHosts.size() == 0) {
         return true;
      }

      final String host = req.getHeader("Host");
      if (host == null || "".equals(host)) {
         return false;
      }

      return allowedHosts.contains(host);
   }

}

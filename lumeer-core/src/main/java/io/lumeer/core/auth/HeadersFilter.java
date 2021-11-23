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
import java.util.Objects;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SessionScoped
public class HeadersFilter implements AuthFilter, Serializable {

   private static final String VIEW_ID = "X-Lumeer-View-Id";
   private static final String CORRELATION_ID = "X-Lumeer-Correlation-Id";
   private static final String APP_ID = "X-Lumeer-App-Id";
   private static final String TIMESTAMP_HEADER = "X-Lumeer-Start-Timestamp";
   private static final String LOCALE_HEADER = "X-Lumeer-Locale";
   private static final String TIMEZONE_HEADER = "X-Lumeer-Timezone";

   @Inject
   private ConfigurationFacade configurationFacade;

   @Inject
   private RequestDataKeeper requestDataKeeper;

   @Inject
   private PermissionsChecker permissionsChecker;

   @Override
   public void init(final FilterConfig filterConfig) throws ServletException {

   }

   @Override
   public FilterResult doFilter(final HttpServletRequest req, final HttpServletResponse res) throws IOException, ServletException {
      addCorsHeaders(req, res);

      parseViewId(req);
      parseRequestData(req);
      processStartTimestamp(req, res);
      processUserLocale(req, res);
      processUserTimezone(req, res);
      processCustomHeaders(req, res);

      return FilterResult.CONTINUE;
   }

   private void addCorsHeaders(HttpServletRequest req, HttpServletResponse res) {
      res.addHeader("Access-Control-Allow-Origin", req.getHeader("Origin"));
      res.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH");
      res.addHeader("Access-Control-Allow-Credentials", "true");
      res.addHeader("Access-Control-Expose-Headers", TIMESTAMP_HEADER);
      String reqHeader = req.getHeader("Access-Control-Request-Headers");
      if (reqHeader != null && !reqHeader.isEmpty()) {
         res.addHeader("Access-Control-Allow-Headers", reqHeader);
      }
   }

   private void parseViewId(final HttpServletRequest req) {
      final String viewId = req.getHeader(VIEW_ID);

      // there is no view, by setting it to an empty string, we lock any further view id changes
      permissionsChecker.setViewId(Objects.requireNonNullElse(viewId, ""));
   }

   private void processStartTimestamp(final HttpServletRequest req, final HttpServletResponse res) {
      String tm = req.getHeader(TIMESTAMP_HEADER);
      if (tm != null) {
         res.addHeader(TIMESTAMP_HEADER, tm);
      }
   }

   private void processUserLocale(final HttpServletRequest req, final HttpServletResponse res) {
      String locale = req.getHeader(LOCALE_HEADER);
      if (locale != null) {
         requestDataKeeper.setUserLocale(locale);
      }
   }

   private void processUserTimezone(final HttpServletRequest req, final HttpServletResponse res) {
      String timezone = req.getHeader(TIMEZONE_HEADER);
      if (timezone != null) {
         requestDataKeeper.setTimezone(timezone);
      }
   }

   private void processCustomHeaders(final HttpServletRequest req, final HttpServletResponse res) {
      res.addHeader("X-Frame-Options", "DENY");
   }

   private void parseRequestData(final HttpServletRequest req) {
      final String correlationId = req.getHeader(CORRELATION_ID);
      final String appId = req.getHeader(APP_ID);

      this.requestDataKeeper.setCorrelationId(Objects.requireNonNullElse(correlationId, ""));
      this.requestDataKeeper.setAppId(Objects.requireNonNullElse(appId, ""));
   }

}

/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.auth.common;


public interface SessionAttributeKeys {

    /**
     * The key for the session attribute containing the servlet request (an
     * instance of javax.servlet.http.HttpServletRequest).
     */
    public static final String FEDORA_SERVLET_REQUEST =
            "fedora-servlet-request";

    /**
     * The key for the session attribute containing the request's user
     * principal (an instance of java.security.Principal).
     */
    public static final String FEDORA_USER_PRINCIPAL =
            "fedora-user-principal";

    /**
     * The key for the session attribute containing the union of the sets of
     * principals obtained from the configured principal providers plus the
     * request's user principal (an instance of Set, containing
     * java.security.Principal instances).
     */
    public static final String FEDORA_ALL_PRINCIPALS = "fedora-all-principals";

}

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

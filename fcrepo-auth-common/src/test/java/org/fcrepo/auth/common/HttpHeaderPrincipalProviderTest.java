package org.fcrepo.auth.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.ServletCredentials;

import javax.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.util.Set;

public class HttpHeaderPrincipalProviderTest {

    @Mock
    private ServletCredentials credentials;

    @Mock
    private HttpServletRequest request;

    private HttpHeaderPrincipalProvider provider;

    @Before
    public void setUp() {
        initMocks(this);
        when(credentials.getRequest()).thenReturn(request);

        provider = new HttpHeaderPrincipalProvider();
    }

    @Test
    public void testPrincipalsExtractedFromHeaders() {

        when(request.getHeader("Groups")).thenReturn("a,b");

        provider.setHeaderName("Groups");
        provider.setSeparator(",");

        final Set<Principal> principals = provider.getPrincipals(credentials);

        assertEquals(2, principals.size());
        assertTrue("The principals should contain 'a'", principals
                .contains(provider.new HttpHeaderPrincipal("a")));
        assertTrue("The principals should contain 'b'", principals
                .contains(provider.new HttpHeaderPrincipal("b")));

    }

}

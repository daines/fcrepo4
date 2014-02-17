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
package org.fcrepo.auth.roles.basic;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.fcrepo.http.commons.test.util.TestHelpers.setField;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.fcrepo.auth.roles.common.AccessRolesProvider;
import org.fcrepo.http.commons.session.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.value.Path;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Mike Daines
 */
public class BasicRolesPEPRemoveChildrenRecursiveTest {

    private BasicRolesPEP pep;

    @Mock
    private AccessRolesProvider accessRolesProvider;

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session mockSession;

    @Mock
    private Principal principal;

    private Set<Principal> allPrincipals;

    @Mock
    private Path parentPath;

    @Mock
    private Node parentNode;

    @Mock
    private Path writablePath;

    @Mock
    private Node writableNode;

    @Mock
    private Path readablePath;

    @Mock
    private Node readableNode;

    @Mock
    private Path noAclPath;

    @Mock
    private Node noAclNode;

    @Before
    public void setUp() throws RepositoryException, NoSuchFieldException {
        initMocks(this);

        pep = new BasicRolesPEP();
        setField(pep, "accessRolesProvider", accessRolesProvider);
        setField(pep, "sessionFactory", sessionFactory);

        when(sessionFactory.getInternalSession()).thenReturn(mockSession);

        when(principal.getName()).thenReturn("user");
        allPrincipals = singleton(principal);

        // ACLs for paths and nodes

        final Map<String, List<String>> writerAcl =
                singletonMap("user", asList("writer"));
        final Map<String, List<String>> readerAcl =
                singletonMap("user", asList("reader"));

        when(accessRolesProvider.findRolesForPath(parentPath, mockSession))
                .thenReturn(writerAcl);
        when(accessRolesProvider.getRoles(parentNode, false)).thenReturn(
                writerAcl);

        when(accessRolesProvider.findRolesForPath(writablePath, mockSession))
                .thenReturn(writerAcl);
        when(accessRolesProvider.getRoles(writableNode, false)).thenReturn(
                writerAcl);

        when(accessRolesProvider.findRolesForPath(readablePath, mockSession))
                .thenReturn(readerAcl);
        when(accessRolesProvider.getRoles(readableNode, false)).thenReturn(
                readerAcl);

        when(accessRolesProvider.findRolesForPath(noAclPath, mockSession))
                .thenReturn(null);
        when(accessRolesProvider.getRoles(noAclNode, false)).thenReturn(null);

        // Paths for nodes and nodes for paths. The relationships between nodes
        // are actually defined below in the test cases.

        when(parentPath.toString()).thenReturn("parent");
        when(mockSession.getNode("parent")).thenReturn(parentNode);
        when(parentNode.getPath()).thenReturn("parent");

        when(writablePath.toString()).thenReturn("writable");
        when(mockSession.getNode("writable")).thenReturn(writableNode);
        when(writableNode.getPath()).thenReturn("writable");

        when(readablePath.toString()).thenReturn("readable");
        when(mockSession.getNode("readable")).thenReturn(readableNode);
        when(readableNode.getPath()).thenReturn("readable");

        when(noAclPath.toString()).thenReturn("noacl");
        when(mockSession.getNode("noacl")).thenReturn(noAclNode);
        when(noAclNode.getPath()).thenReturn("noacl");
    }

    @Test
    public void shouldPermitForChildlessNode() throws RepositoryException {
        when(parentNode.hasNodes()).thenReturn(false);

        assertTrue(pep.hasModeShapePermission(parentPath,
                new String[] {"remove"}, allPrincipals, principal));
    }

    @Test
    public void shouldPermitForWritableChild() throws RepositoryException {
        when(parentNode.hasNodes()).thenReturn(true);

        final NodeIterator iterator = mock(NodeIterator.class);
        when(iterator.hasNext()).thenReturn(true, false);
        when(iterator.nextNode()).thenReturn(writableNode);
        when(parentNode.getNodes()).thenReturn(iterator);

        assertTrue(pep.hasModeShapePermission(parentPath,
                new String[] {"remove"}, allPrincipals, principal));
    }

    @Test
    public void shouldDenyForUnwritableChild() throws RepositoryException {
        when(parentNode.hasNodes()).thenReturn(true);

        final NodeIterator iterator = mock(NodeIterator.class);
        when(iterator.hasNext()).thenReturn(true, true, false);
        when(iterator.nextNode()).thenReturn(writableNode, readableNode);
        when(parentNode.getNodes()).thenReturn(iterator);

        assertFalse(pep.hasModeShapePermission(parentPath,
                new String[] {"remove"}, allPrincipals, principal));
    }

    @Test
    public void shouldInheritParentRolesIfNoAcl() throws RepositoryException {
        when(parentNode.hasNodes()).thenReturn(true);

        final NodeIterator iterator = mock(NodeIterator.class);
        when(iterator.hasNext()).thenReturn(true, false);
        when(iterator.nextNode()).thenReturn(noAclNode);
        when(parentNode.getNodes()).thenReturn(iterator);

        assertTrue(pep.hasModeShapePermission(parentPath,
                new String[] {"remove"}, allPrincipals, principal));
    }

    @Test
    public void shouldDenyWithRecursion() throws RepositoryException {
        when(parentNode.hasNodes()).thenReturn(true);
        when(writableNode.hasNodes()).thenReturn(true);

        final NodeIterator parentIterator = mock(NodeIterator.class);
        when(parentIterator.hasNext()).thenReturn(true, false);
        when(parentIterator.nextNode()).thenReturn(writableNode);
        when(parentNode.getNodes()).thenReturn(parentIterator);

        final NodeIterator writableIterator = mock(NodeIterator.class);
        when(writableIterator.hasNext()).thenReturn(true, false);
        when(writableIterator.nextNode()).thenReturn(readableNode);
        when(writableNode.getNodes()).thenReturn(writableIterator);

        assertFalse(pep.hasModeShapePermission(parentPath,
                new String[] {"remove"}, allPrincipals, principal));
    }

}


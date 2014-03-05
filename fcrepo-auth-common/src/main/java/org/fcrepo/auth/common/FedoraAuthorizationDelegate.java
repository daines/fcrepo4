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

import org.modeshape.jcr.value.Path;

import javax.jcr.Session;

import java.security.Principal;
import java.util.Iterator;
import java.util.Set;

/**
 * Policy Enforcement Points implement the various authorization decisions
 * needed by Fedora. Implementations will translate enforcement calls into
 * their given policy framework.
 *
 * @author Gregory Jansen
 */
public interface FedoraAuthorizationDelegate {

    /**
     * Is the action permitted to the user or other any other principal on the
     * given node path?
     * 
     * @param session
     * @param absPath
     * @param actions
     * @return
     */
    boolean hasPermission(Session session, Path absPath, String[] actions);

    /**
     * Filter the collection of JCR paths, selecting those the user has
     * permission to read.
     *
     * @param paths an iterator of paths
     * @param allPrincipals all the authenticated principals
     * @param userPrincipal the user principal
     * @return an iterator of permitted paths
     */
    Iterator<Path> filterPathsForReading(Iterator<Path> paths,
            Set<Principal> allPrincipals, Principal userPrincipal);

}

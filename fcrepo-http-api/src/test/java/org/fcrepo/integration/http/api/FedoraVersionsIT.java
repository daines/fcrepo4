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

package org.fcrepo.integration.http.api;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.update.GraphStore;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import javax.ws.rs.core.Response;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import static com.google.common.collect.Lists.newArrayList;
import static com.hp.hpl.jena.graph.Node.ANY;
import static com.hp.hpl.jena.graph.NodeFactory.createLiteral;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static org.fcrepo.kernel.RdfLexicon.DC_TITLE;
import static org.fcrepo.kernel.RdfLexicon.HAS_PRIMARY_TYPE;
import static org.fcrepo.kernel.RdfLexicon.HAS_VERSION;
import static org.fcrepo.kernel.RdfLexicon.VERSIONING_POLICY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FedoraVersionsIT extends AbstractResourceIT {

    @Test
    public void testGetObjectVersionProfile() throws Exception {
        final String pid = UUID.randomUUID().toString();

        createObject(pid);
        final HttpGet getVersion =
            new HttpGet(serverAddress + pid + "/fcr:versions");
        logger.debug("Retrieved version profile:");
        final GraphStore results = getGraphStore(getVersion);
        final Resource subject = createResource(serverAddress + pid);
        assertTrue("Didn't find a version triple!",
                results.contains(Node.ANY, subject.asNode(), HAS_VERSION.asNode(), Node.ANY));
    }

    @Test
    public void testAddAndRetrieveVersion() throws Exception {
        final String pid = UUID.randomUUID().toString();
        createObject(pid);

        logger.info("Setting a title");
        patchLiteralProperty(serverAddress + pid, "http://purl.org/dc/elements/1.1/title", "First Title");

        final GraphStore nodeResults = getContent(serverAddress + pid);
        assertTrue("Should find original title", nodeResults.contains(Node.ANY, Node.ANY, DC_TITLE.asNode(), NodeFactory.createLiteral("First Title")));

        logger.info("Posting version v0.0.1");
        postObjectVersion(pid, "v0.0.1");

        logger.info("Replacing the title");
        patchLiteralProperty(serverAddress + pid, "http://purl.org/dc/elements/1.1/title", "Second Title");

        final GraphStore versionResults = getContent(serverAddress + pid + "/fcr:versions/v0.0.1");
        logger.info("Got version profile:");

        assertTrue("Didn't find a version triple!",
                      versionResults.contains(Node.ANY, Node.ANY, HAS_PRIMARY_TYPE.asNode(), NodeFactory.createLiteral("nt:frozenNode")));

        assertTrue("Should find a title in historic version", versionResults.contains(Node.ANY, Node.ANY, DC_TITLE.asNode(), Node.ANY));
        assertTrue("Should find original title in historic version", versionResults.contains(Node.ANY, Node.ANY, DC_TITLE.asNode(), NodeFactory.createLiteral("First Title")));
        assertFalse("Should not find the updated title in historic version", versionResults.contains(Node.ANY, Node.ANY, DC_TITLE.asNode(), NodeFactory.createLiteral("Second Title")));
    }

    @Test
    public void testVersioningANodeWithAVersionableChild() throws Exception {
        final String pid = UUID.randomUUID().toString();
        createObject(pid);

        logger.info("Adding a child");
        createDatastream(pid, "ds", "This DS will not be versioned");

        logger.info("Posting version");
        postObjectVersion(pid);

        final HttpGet getVersion =
                new HttpGet(serverAddress + pid + "/fcr:versions");
        logger.debug("Retrieved version profile:");
        final GraphStore results = getGraphStore(getVersion);
        final Resource subject =
                createResource(serverAddress + pid);
        assertTrue("Didn't find a version triple!",
                results.contains(Node.ANY, subject.asNode(), HAS_VERSION.asNode(), Node.ANY));


        final Iterator<Quad> versionIt = results.find(Node.ANY, subject.asNode(), HAS_VERSION.asNode(), Node.ANY);
        while (versionIt.hasNext()) {
            final String url = versionIt.next().getObject().getURI();
            assertEquals("Version " + url + " isn't accessible!",
                    200, getStatus(new HttpGet(url)));
        }
    }

    @Test
    public void testCreateUnlabeledVersion() throws Exception {
        logger.info("Creating an object");
        final String objId = UUID.randomUUID().toString();
        createObject(objId);

        logger.info("Setting a title");
        patchLiteralProperty(serverAddress + objId, "http://purl.org/dc/elements/1.1/title", "Example Title");

        logger.info("Posting an unlabeled version");
        postObjectVersion(objId);
    }

    @Test
    public void testCreateTwoVersionsWithSameLabel() throws Exception {
        logger.info("creating an object");
        final String objId = UUID.randomUUID().toString();
        createObject(objId);

        logger.info("Setting a title");
        patchLiteralProperty(serverAddress + objId, "http://purl.org/dc/elements/1.1/title", "First title");

        logger.info("posting a version with label \"label\"");
        postObjectVersion(objId, "label");

        logger.info("Resetting the title");
        patchLiteralProperty(serverAddress + objId, "http://purl.org/dc/elements/1.1/title", "Second title");

        logger.info("posting a version with label \"label\"");
        postObjectVersion(objId, "label");

        logger.info("Resetting the title");
        patchLiteralProperty(serverAddress + objId, "http://purl.org/dc/elements/1.1/title", "Third title");

        final GraphStore versionResults = getContent(serverAddress + objId + "/fcr:versions/label");
        logger.info("Got version profile:");

        assertTrue("Should find the title from the last version tagged with the label \"label\"",
                versionResults.contains(Node.ANY, Node.ANY, DC_TITLE.asNode(), NodeFactory.createLiteral("Second title")));

    }

    @Test
    public void testGetDatastreamVersionNotFound() throws Exception {
        final String pid = randomUUID().toString();

        createObject(pid);
        createDatastream(pid, "ds1", "foo");

        final HttpGet getVersion =
            new HttpGet(serverAddress
                    + pid + "/ds1/fcr:versions/lastVersion");
        final HttpResponse resp = execute(getVersion);
        assertEquals(404, resp.getStatusLine().getStatusCode());
    }

    public void mutateDatastream(final String objName, final String dsName,
                                 final String contentText) throws IOException {
        final HttpPut mutateDataStreamMethod =
                putDSMethod(objName, dsName, contentText);
        final HttpResponse response = client.execute(mutateDataStreamMethod);
        final int status = response.getStatusLine().getStatusCode();
        if (status != NO_CONTENT.getStatusCode()) {
            logger.error(EntityUtils.toString(response.getEntity()));
        }
        assertEquals("Couldn't mutate a datastream!", NO_CONTENT.getStatusCode(), status);

    }

    @Test
    public void isAutoVersionedContentStillAccessible() throws Exception {
        final String objName = UUID.randomUUID().toString();
        final String dsName = UUID.randomUUID().toString();
        final String firstVersionText = "foo";
        final String secondVersionText = "bar";

        createObject(objName);
        createDatastream(objName, dsName, firstVersionText);

        setAutoVersioning(serverAddress + objName + "/" + dsName);

        mutateDatastream(objName, dsName, secondVersionText);

        final HttpGet retrieveMutatedDataStreamMethod =
                new HttpGet(serverAddress +
                        objName + "/" + dsName + "/fcr:content");
        assertEquals("Datastream didn't accept mutation!", secondVersionText,
                EntityUtils.toString(
                        client.execute(
                                retrieveMutatedDataStreamMethod).getEntity()));

        final HttpGet getVersion =
                new HttpGet(serverAddress + objName + "/" + dsName + "/fcr:versions");
        logger.debug("Retrieved version profile:");
        final GraphStore results = getGraphStore(getVersion);
        final Resource subject =
                createResource(serverAddress + objName + "/" + dsName);
        assertTrue("Didn't find a version triple!",
                results.contains(Node.ANY, subject.asNode(), HAS_VERSION.asNode(), Node.ANY));

        verifyVersions(results, subject.asNode(), firstVersionText);
    }

    @Test
    public void testAddMixinAutoVersioning() throws IOException {
        postNodeTypeCNDSnippet("[fedora:autoVersioned] mixin\n" +
                "  - fedoraconfig:versioningPolicy (STRING) = \"auto-version\" autocreated");

        final String objName = UUID.randomUUID().toString();

        createObject(objName);
        addMixin(serverAddress + objName, "http://fedora.info/definitions/v4/rest-api#autoVersioned");

        final GraphStore initialVersion = getContent(serverAddress + objName);
        assertTrue("Should find auto-created versioning policy", initialVersion
                .contains(ANY,
                        createResource(serverAddress + objName).asNode(),
                        VERSIONING_POLICY.asNode(),
                        createLiteral("auto-version")));
    }

    @Test
    public void testRepositoryWideAutoVersioning() throws IOException {
        postNodeTypeCNDSnippet("[fedora:autoVersioned] mixin\n" +
                "  - fedoraconfig:versioningPolicy (STRING) = \"auto-version\" autocreated");
        postNodeTypeCNDSnippet("[fedora:resource] > fedora:relations, mix:created, mix:lastModified, mix:lockable, mix:versionable, fedora:autoVersioned, dc:describable mixin\n" +
                "  - rdf:type (URI) multiple\n" +
                "  - * (undefined) multiple\n" +
                "  - * (undefined)");

        final String objName = "testRepositoryWideAutoVersioning";
        final String dsName = "datastream";

        createObject(objName);

        final GraphStore initialVersion = getContent(serverAddress + objName);
        assertTrue("Should find auto-created versioning policy",
                initialVersion.contains(Node.ANY, createResource(serverAddress + objName).asNode(), VERSIONING_POLICY.asNode(), NodeFactory.createLiteral("auto-version")));

        testDatastreamContentUpdatesCreateNewVersions(objName, dsName);
    }

    private void testDatastreamContentUpdatesCreateNewVersions(final String objName, final String dsName) throws IOException {
        final String firstVersionText = "foo";
        final String secondVersionText = "bar";
        createDatastream(objName, dsName, firstVersionText);
        final GraphStore dsInitialVersion = getContent(serverAddress + objName);
        assertTrue("Should find auto-created versoning policy",
                dsInitialVersion.contains(Node.ANY, createResource(serverAddress + objName+ "/" + dsName).asNode(), VERSIONING_POLICY.asNode(), NodeFactory.createLiteral("auto-version")));

        mutateDatastream(objName, dsName, secondVersionText);
        final HttpGet retrieveMutatedDataStreamMethod =
                new HttpGet(serverAddress +
                        objName + "/" + dsName + "/fcr:content");
        assertEquals("Datastream didn't accept mutation!", secondVersionText,
                EntityUtils.toString(
                        client.execute(
                                retrieveMutatedDataStreamMethod).getEntity()));

        final HttpGet getVersion =
                new HttpGet(serverAddress + objName + "/" + dsName + "/fcr:versions");
        logger.debug("Retrieved version profile:");
        final GraphStore results = getGraphStore(getVersion);
        final Resource subject =
                createResource(serverAddress + objName + "/" + dsName);
        assertTrue("Didn't find a version triple!",
                results.contains(Node.ANY, subject.asNode(), HAS_VERSION.asNode(), Node.ANY));

        verifyVersions(results, subject.asNode(), firstVersionText, secondVersionText);
    }

    /**
     * Verifies that one version exists with each supplied value.  This method
     * makes assertions that each of the provided values is the content of a
     * version node and nothing else.  Order isn't important, and no assumption
     * is made about whether extra versions exist.
     */
    private void verifyVersions(final GraphStore graph, final Node subject, final String ... values) throws IOException {
        final ArrayList<String> remainingValues = newArrayList(values);
        final Iterator<Quad> versionIt = graph.find(ANY, subject, HAS_VERSION.asNode(), ANY);
        while (versionIt.hasNext() && !remainingValues.isEmpty()) {
            final String value =
                EntityUtils.toString(client
                        .execute(new HttpGet(versionIt.next().getObject().getURI() + "/fcr:content")).getEntity());
            remainingValues.remove(value);
        }
        if (!remainingValues.isEmpty()) {
            fail(remainingValues.get(0) + " was not preserved in the version history!");
        }
    }

    public void postNodeTypeCNDSnippet(final String snippet) throws IOException {
        final HttpPost httpPost = new HttpPost(serverAddress + "/fcr:nodetypes");
        final BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(new ByteArrayInputStream(snippet.getBytes()));
        httpPost.setEntity(entity);
        final HttpResponse response = client.execute(httpPost);

        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine().getStatusCode());
    }

    private static void setAutoVersioning(final String url) throws IOException {
        patchLiteralProperty(url,
                "http://fedora.info/definitions/v4/config#versioningPolicy",
                "auto-version");
    }

    private static void patchLiteralProperty(final String url, final String predicate, final String literal) throws IOException {
        final HttpPatch updateObjectGraphMethod =
                new HttpPatch(url);
        updateObjectGraphMethod.addHeader("Content-Type",
                "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();
        e.setContent(new ByteArrayInputStream(
                ("INSERT DATA { <> <" + predicate + "> \"" + literal + "\" } ")
                        .getBytes()));
        updateObjectGraphMethod.setEntity(e);
        final HttpResponse response = client.execute(updateObjectGraphMethod);
        assertEquals(NO_CONTENT.getStatusCode(), response.getStatusLine()
                .getStatusCode());
    }

    private static void addMixin(final String url, final String mixinUrl) throws IOException {
        final HttpPatch updateObjectGraphMethod =
                new HttpPatch(url);
        updateObjectGraphMethod.addHeader("Content-Type",
                "application/sparql-update");
        final BasicHttpEntity e = new BasicHttpEntity();

        e.setContent(new ByteArrayInputStream(
                ("INSERT DATA { <> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + mixinUrl + "> . } ")
                        .getBytes()));
        updateObjectGraphMethod.setEntity(e);
        final HttpResponse response = client.execute(updateObjectGraphMethod);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatusLine()
                .getStatusCode());
    }

    private GraphStore getContent(final String url) throws IOException {
        final HttpGet getVersion = new HttpGet(url);
        return getGraphStore(getVersion);
    }

    public void postObjectVersion(final String pid) throws IOException {
        postVersion(pid, null);
    }

    public void postObjectVersion(final String pid, final String versionLabel) throws IOException {
        postVersion(pid, versionLabel);
    }

    public void postDsVersion(final String pid, final String dsId) throws IOException {
        postVersion(pid + "/" + dsId, null);
    }

    public void postVersion(final String path, final String label) throws IOException {
        logger.info("Posting version");
        final HttpPost postVersion = postObjMethod(path + "/fcr:versions" + (label == null ? "" : "/" + label));
        execute(postVersion);
        assertEquals(NO_CONTENT.getStatusCode(), getStatus(postVersion));
    }

}

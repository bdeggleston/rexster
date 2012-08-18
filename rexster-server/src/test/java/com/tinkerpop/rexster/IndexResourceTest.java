package com.tinkerpop.rexster;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.rexster.server.RexsterApplication;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.IndexableGraph;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class IndexResourceTest extends BaseTest {

    @Test(expected = WebApplicationException.class)
    public void getAllIndicesNonIndexableGraph() {
        final Graph graph = this.mockery.mock(Graph.class);
        final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplication ra = this.mockery.mock(RexsterApplication.class);

        final UriInfo uri = this.mockery.mock(UriInfo.class);

        final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);

        this.mockery.checking(new Expectations() {{
            allowing(httpServletRequest).getParameterMap();
            will(returnValue(new HashMap<String, String>()));
            allowing(ra).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});

        final IndexResource resource = new IndexResource(uri, httpServletRequest, ra);
        resource.getAllIndices(graphName);
    }

    @Test
    public void getAllIndicesNoOffset() {
        final IndexResource resource = constructIndexResourceWithToyGraph().getResource();
        final Response response = resource.getAllIndices(graphName);

        this.assertIndexOkResponseJsonStructure(10, 10, response);
    }

    @Test
    public void getAllIndicesWithValidOffset() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "1");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "2");

        final IndexResource resource = constructIndexResource(true, parameters).getResource();
        final Response response = resource.getAllIndices(graphName);

        this.assertIndexOkResponseJsonStructure(1, 10, response);
    }

    @Test
    public void getAllIndicesWithOffsetNotEnoughResults() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "10");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");

        final IndexResource resource = constructIndexResource(true, parameters).getResource();
        final Response response = resource.getAllIndices(graphName);

        this.assertIndexOkResponseJsonStructure(0, 10, response);

    }

    @Test
    public void getAllIndicesWithOffsetStartAfterEnd() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "3");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "2");
        final IndexResource resource = constructIndexResource(true, parameters).getResource();
        final Response response = resource.getAllIndices(graphName);
        this.assertIndexOkResponseJsonStructure(0, 10, response);

    }

    @Test(expected = WebApplicationException.class)
    public void getIndexCountIndexNotFound() {
        final IndexResource resource = constructIndexResourceWithToyGraph().getResource();
        resource.getIndexCount(graphName, "index-name-20");
    }

    @Test(expected = WebApplicationException.class)
    public void getIndexCountIndexBadRequestNoKey() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("value", "name");
        final IndexResource resource = constructIndexResource(true, parameters).getResource();
        resource.getIndexCount(graphName, "index-name-0");
    }

    @Test(expected = WebApplicationException.class)
    public void getIndexCountIndexBadRequestNoValue() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("key", "name");
        final IndexResource resource = constructIndexResource(true, parameters).getResource();
        resource.getIndexCount(graphName, "index-name-0");
    }

    @Test
    public void getIndexCountIndexValid() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("key", "name");
        parameters.put("value", "marko");
        final IndexResource resource = constructIndexResource(true, parameters).getResource();

        final Response response = resource.getIndexCount(graphName, "index-name-0");

        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        final JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.TOTAL_SIZE));
        Assert.assertEquals(1, json.optInt(Tokens.TOTAL_SIZE));
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));
    }

    @Test
    public void getElementsFromIndexAll() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("key", "field");
        parameters.put("value", "X");
        final IndexResource resource = constructIndexResource(true, parameters).getResource();

        final Response response = resource.getElementsFromIndex(graphName, "index-name-9");

        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        final JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.TOTAL_SIZE));
        Assert.assertEquals(4, json.optInt(Tokens.TOTAL_SIZE));
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        final JSONArray results = json.optJSONArray(Tokens.RESULTS);
        Assert.assertEquals(4, results.length());
        final List<String> foundIds = new ArrayList<String>();
        for (int ix = 0; ix < results.length(); ix++) {
            foundIds.add(results.optJSONObject(ix).optString(Tokens._ID));
        }

        Assert.assertTrue(foundIds.contains("1"));
        Assert.assertTrue(foundIds.contains("2"));
        Assert.assertTrue(foundIds.contains("4"));
        Assert.assertTrue(foundIds.contains("6"));
    }

    @Test
    public void getElementsFromIndexAllOffset() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("key", "field");
        parameters.put("value", "X");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "1");
        parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "3");
        final IndexResource resource = constructIndexResource(true, parameters).getResource();

        final Response response = resource.getElementsFromIndex(graphName, "index-name-9");

        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        final JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.TOTAL_SIZE));
        Assert.assertEquals(4, json.optInt(Tokens.TOTAL_SIZE));
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        final JSONArray results = json.optJSONArray(Tokens.RESULTS);
        Assert.assertEquals(2, results.length());
        final List<String> foundIds = new ArrayList<String>();
        for (int ix = 0; ix < results.length(); ix++) {
            foundIds.add(results.optJSONObject(ix).optString(Tokens._ID));
        }

        Assert.assertTrue(foundIds.contains("1"));
        Assert.assertTrue(foundIds.contains("6"));
    }

    @Test(expected = WebApplicationException.class)
    public void deleteIndexNoIndexFound() {
        final IndexResource resource = constructIndexResourceWithToyGraph().getResource();
        resource.deleteIndex(graphName, "index-name-90000");
    }

    @Test
    public void deleteIndexValid() {
        final IndexResource resource = constructIndexResourceWithToyGraph().getResource();
        resource.deleteIndex(graphName, "index-name-9");

        final Index<Vertex> idx = ((IndexableGraph) this.toyGraph).getIndex("index-name-9", Vertex.class);
        Assert.assertNull(idx);
    }

    @Test
    public void deleteIndexVertexItem() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("key", "name");
        parameters.put("value", "0");
        parameters.put("id", "1");
        final IndexResource resource = constructIndexResource(true, parameters).getResource();
        resource.deleteIndex(graphName, "index-name-0");

        final Index<Vertex> idx = ((IndexableGraph) this.toyGraph).getIndex("index-name-9", Vertex.class);
        final Iterable<Vertex> vertices = idx.get("name", "marko");
        Assert.assertFalse(vertices.iterator().hasNext());
    }

    @Test
    public void deleteIndexEdgeItem() {
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("key", "weight");
        parameters.put("value", "0.2");
        parameters.put("id", "12");
        final IndexResource resource = constructIndexResource(true, parameters).getResource();
        resource.deleteIndex(graphName, "index-name-0");

        final Index<Edge> idx = ((IndexableGraph) this.toyGraph).getIndex("index-name-5", Edge.class);
        final Iterable<Edge> edges = idx.get("weight", "0.2");
        Assert.assertFalse(edges.iterator().hasNext());
    }

    private JSONObject assertIndexOkResponseJsonStructure(final int numberOfIndicesReturned,
                                                          final int numberOfIndicesTotal, final Response response) {
        Assert.assertNotNull(response);
        Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
        Assert.assertNotNull(response.getEntity());
        Assert.assertTrue(response.getEntity() instanceof JSONObject);

        final JSONObject json = (JSONObject) response.getEntity();
        Assert.assertTrue(json.has(Tokens.TOTAL_SIZE));
        Assert.assertEquals(numberOfIndicesTotal, json.optInt(Tokens.TOTAL_SIZE));
        Assert.assertTrue(json.has(Tokens.QUERY_TIME));

        Assert.assertTrue(json.has(Tokens.RESULTS));
        Assert.assertFalse(json.isNull(Tokens.RESULTS));

        final JSONArray jsonResults = json.optJSONArray(Tokens.RESULTS);
        Assert.assertEquals(numberOfIndicesReturned, jsonResults.length());

        return json;
    }
}
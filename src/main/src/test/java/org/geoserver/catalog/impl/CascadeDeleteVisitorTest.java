/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;


import static org.easymock.EasyMock.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import static org.geoserver.data.test.CiteTestData.*;
import static org.junit.Assert.*;

import java.io.IOException;


import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.junit.After;
import org.junit.Test;
import org.opengis.filter.Filter;

public class CascadeDeleteVisitorTest extends CascadeVisitorAbstractTest {

    @After
    public void resetChanges() throws IOException {
        // add layers
        revertLayer(LAKES);
        revertLayer(BRIDGES);
        revertLayer(FORESTS);
        revertLayer(BUILDINGS);

        Catalog catalog = getCatalog();
        StyleInfo style = catalog.getStyleByName(WS_STYLE);
        if (style != null) {
            catalog.remove(style);
        }
        LayerGroupInfo group = catalog.getLayerGroupByName(LAKES_GROUP);
        if (group != null) {
            catalog.remove(group);
        }

        setupExtras(getTestData(), catalog);
    }


    @Test
    public void testCascadeLayer() {
        Catalog catalog = getCatalog();
        String name = toString(LAKES);
        LayerInfo layer = catalog.getLayerByName(name);
        assertNotNull(layer);

        CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(catalog);
        visitor.visit(layer);

        LayerGroupInfo group = catalog.getLayerGroupByName(LAKES_GROUP);
        assertEquals(2, group.getLayers().size());
        assertFalse(group.getLayers().contains(layer));
    }

    @Test
    public void testCascadeStore() {
        Catalog catalog = getCatalog();
        DataStoreInfo store = (DataStoreInfo) catalog.getLayerByName(getLayerId(LAKES))
                .getResource().getStore();
        new CascadeDeleteVisitor(catalog).visit(store);

        // that store actually holds all layers, so check we got empty
        assertEquals(0, catalog.count(LayerInfo.class, Filter.INCLUDE));
        assertEquals(0, catalog.count(ResourceInfo.class, Filter.INCLUDE));
        assertEquals(0, catalog.count(StoreInfo.class, Filter.INCLUDE));
        assertEquals(0, catalog.count(LayerGroupInfo.class, Filter.INCLUDE));
    }

    @Test
    public void testCascadeWorkspace() {
        Catalog catalog = getCatalog();
        WorkspaceInfo ws = catalog.getWorkspaceByName(CITE_PREFIX);
        new CascadeDeleteVisitor(catalog).visit(ws);

        // check the namespace is also gone
        assertNull(catalog.getNamespaceByPrefix(CITE_PREFIX));

        // that workspace actually holds all layers, so check we got empty
        assertEquals(0, catalog.count(LayerInfo.class, Filter.INCLUDE));
        assertEquals(0, catalog.count(ResourceInfo.class, Filter.INCLUDE));
        assertEquals(0, catalog.count(StoreInfo.class, Filter.INCLUDE));
        assertEquals(0, catalog.count(LayerGroupInfo.class, Filter.INCLUDE));

        // the workspace specific style is also gone
        assertEquals(0, catalog.getStylesByWorkspace(CITE_PREFIX).size());
        assertNull(catalog.getStyleByName(WS_STYLE));
    }

    @Test
    public void testCascadeStyle() {
        Catalog catalog = getCatalog();
        StyleInfo style = catalog.getStyleByName(LAKES.getLocalPart());
        assertNotNull(style);

        new CascadeDeleteVisitor(catalog).visit(style);
        assertNull(catalog.getStyleByName(LAKES.getLocalPart()));
        LayerInfo layer = catalog.getLayerByName(getLayerId(LAKES));
        assertEquals("polygon", layer.getDefaultStyle().getName());
    }
}

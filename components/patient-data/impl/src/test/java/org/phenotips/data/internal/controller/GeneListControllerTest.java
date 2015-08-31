/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.data.internal.controller;

import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for the {@link GeneListController} Component,
 * only the overridden methods from {@link AbstractComplexController} are tested here
 */
public class GeneListControllerTest
{

    @Rule
    public MockitoComponentMockingRule<PatientDataController<Map<String, String>>> mocker =
        new MockitoComponentMockingRule<PatientDataController<Map<String, String>>>(GeneListController.class);

    private static final String GENES_STRING = "genes";

    private static final String CONTROLLER_NAME = GENES_STRING;

    private static final String GENES_ENABLING_FIELD_NAME = GENES_STRING;

    private static final String GENES_COMMENTS_ENABLING_FIELD_NAME = "genes_comments";

    private static final String GENE_KEY = "gene";

    private static final String COMMENTS_KEY = "comments";

    private DocumentAccessBridge documentAccessBridge;

    private List<BaseObject> geneXWikiObjects;

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject obj1;

    @Mock
    private BaseObject obj2;

    @Mock
    private BaseObject obj3;


    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);

        DocumentReference patientDocument = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocument).when(this.patient).getDocument();
        doReturn(this.doc).when(this.documentAccessBridge).getDocument(patientDocument);
        this.geneXWikiObjects = new LinkedList<>();
        this.geneXWikiObjects.add(obj1);
        this.geneXWikiObjects.add(obj2);
        this.geneXWikiObjects.add(obj3);
        int i = 1;
        for (BaseObject gene : this.geneXWikiObjects) {
            BaseStringProperty geneString = mock(BaseStringProperty.class);
            doReturn("gene" + i).when(geneString).getValue();
            BaseStringProperty commentString = mock(BaseStringProperty.class);
            doReturn("comment" + i).when(commentString).getValue();
            doReturn(geneString).when(gene).getField(GENE_KEY);
            doReturn(commentString).when(gene).getField(COMMENTS_KEY);
            i++;
        }
        doReturn(this.geneXWikiObjects).when(this.doc).getXObjects(any(EntityReference.class));
    }

    //-----------------------------------load() tests-----------------------------------

    @Test
    public void loadCatchesExceptionFromDocumentAccess() throws Exception
    {
        Exception exception = new Exception();
        doThrow(exception).when(this.documentAccessBridge).getDocument(any(DocumentReference.class));

        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadCatchesExceptionWhenPatientDoesNotHaveGeneClass() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getXObjects(any(EntityReference.class));

        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadAddsAllGenes() throws ComponentLookupException
    {
        PatientData<Map<String, String>> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(3, result.size());
        Assert.assertTrue(result instanceof IndexedPatientData);
        Assert.assertEquals("gene1", result.get(0).get(GENE_KEY));
        Assert.assertEquals("comment1", result.get(0).get(COMMENTS_KEY));
        Assert.assertEquals("gene2", result.get(1).get(GENE_KEY));
        Assert.assertEquals("comment2", result.get(1).get(COMMENTS_KEY));
        Assert.assertEquals("gene3", result.get(2).get(GENE_KEY));
        Assert.assertEquals("comment3", result.get(2).get(COMMENTS_KEY));
    }

    //-----------------------------------writeJSON() tests-----------------------------------

    @Test
    public void writeJSONDoesNotWriteNullData() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(GENES_ENABLING_FIELD_NAME);
        selectedFields.add(GENES_COMMENTS_ENABLING_FIELD_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNull(json.get(CONTROLLER_NAME));
        verify(this.patient).getData(CONTROLLER_NAME);

    }

    /* Tests that the passed JSON will not be affected by writeJSON in this controller if selected fields
     * is not null, and does not contain GeneListController.GENES_ENABLING_FIELD_NAME
     */
    @Test
    public void writeJSONChecksThatSelectedFieldsContainsGeneEnabler() throws ComponentLookupException
    {
        List<Map<String, String>> internalList = new LinkedList<>();
        PatientData<Map<String, String>> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        // selectedFields could contain any number of random strings; it should not affect the behaviour in this case
        selectedFields.add("some_string");
        selectedFields.add(GENES_COMMENTS_ENABLING_FIELD_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNull(json.get(CONTROLLER_NAME));
    }

    @Test
    public void writeJSONChecksThatDataContainsFields() throws ComponentLookupException
    {
        List<Map<String, String>> internalList = new LinkedList<>();
        PatientData<Map<String, String>> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(GENES_ENABLING_FIELD_NAME);
        selectedFields.add(GENES_COMMENTS_ENABLING_FIELD_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNull(json.get(CONTROLLER_NAME));
    }


    @Test
    public void writeJSONAddsContainerWithAllValues() throws ComponentLookupException
    {
        List<Map<String, String>> internalList = new LinkedList<>();
        Map<String, String> gene1 = new LinkedHashMap<>();
        Map<String, String> gene2 = new LinkedHashMap<>();
        gene1.put(GENE_KEY, "gene1");
        gene1.put(COMMENTS_KEY, "comment1");
        gene2.put(GENE_KEY, "gene2");
        gene2.put(COMMENTS_KEY, "comment2");
        internalList.add(gene1);
        internalList.add(gene2);
        PatientData<Map<String, String>> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(GENES_ENABLING_FIELD_NAME);
        selectedFields.add(GENES_COMMENTS_ENABLING_FIELD_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(CONTROLLER_NAME));
        Assert.assertTrue(json.get(CONTROLLER_NAME) instanceof JSONArray);
        JSONArray container = json.getJSONArray(CONTROLLER_NAME);
        Assert.assertEquals("gene1", container.getJSONObject(0).get(GENE_KEY));
        Assert.assertEquals("comment1", container.getJSONObject(0).get(COMMENTS_KEY));
        Assert.assertEquals("gene2", container.getJSONObject(1).get(GENE_KEY));
        Assert.assertEquals("comment2", container.getJSONObject(1).get(COMMENTS_KEY));

    }

    @Test
    public void writeJSONRemovesCommentsWhenSelectedFieldsDoesNotContainCommentEnabler()
        throws ComponentLookupException
    {
        List<Map<String, String>> internalList = new LinkedList<>();
        Map<String, String> gene1 = new LinkedHashMap<>();
        Map<String, String> gene2 = new LinkedHashMap<>();
        gene1.put(GENE_KEY, "gene1");
        gene1.put(COMMENTS_KEY, "comment1");
        gene2.put(GENE_KEY, "gene2");
        gene2.put(COMMENTS_KEY, "comment2");
        internalList.add(gene1);
        internalList.add(gene2);
        PatientData<Map<String, String>> patientData = new IndexedPatientData<>(CONTROLLER_NAME, internalList);
        doReturn(patientData).when(this.patient).getData(CONTROLLER_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(GENES_ENABLING_FIELD_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(CONTROLLER_NAME));
        Assert.assertTrue(json.get(CONTROLLER_NAME) instanceof JSONArray);
        JSONArray container = json.getJSONArray(CONTROLLER_NAME);
        Assert.assertEquals("gene1", container.getJSONObject(0).get(GENE_KEY));
        Assert.assertNull(container.getJSONObject(0).get(COMMENTS_KEY));
        Assert.assertEquals("gene2", container.getJSONObject(1).get(GENE_KEY));
        Assert.assertNull(container.getJSONObject(1).get(COMMENTS_KEY));
    }

    //-----------------------------------Abstract Method Tests-----------------------------------

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals(CONTROLLER_NAME, this.mocker.getComponentUnderTest().getName());
    }

    @Test
    public void checkGetJsonPropertyName() throws ComponentLookupException
    {
        Assert.assertEquals(CONTROLLER_NAME,
            ((AbstractComplexController) this.mocker.getComponentUnderTest()).getJsonPropertyName());
    }

    @Test
    public void checkGetProperties() throws ComponentLookupException
    {
        List<String> result =
             ((AbstractComplexController<Map<String, String>>) this.mocker.getComponentUnderTest()).getProperties();

        Assert.assertEquals(2, result.size());
        Assert.assertThat(result, Matchers.hasItem(GENE_KEY));
        Assert.assertThat(result, Matchers.hasItem(COMMENTS_KEY));
    }

    @Test
    public void checkGetBooleanFields() throws ComponentLookupException
    {
        Assert.assertTrue(
            ((AbstractComplexController) this.mocker.getComponentUnderTest()).getBooleanFields().isEmpty());
    }

    @Test
    public void checkGetCodeFields() throws ComponentLookupException
    {
        Assert.assertTrue(((AbstractComplexController) this.mocker.getComponentUnderTest()).getCodeFields().isEmpty());
    }
}
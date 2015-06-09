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
package org.phenotips.vocabulary.internal;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.matchers.CapturingMatcher;
import org.phenotips.vocabulary.SolrVocabularyResourceManager;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.cache.Cache;

import java.io.IOException;
import java.util.Collection;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;


public class GeneHGNCNomenclatureTest
{
    @Rule
    public MockitoComponentMockingRule<Vocabulary> mocker =
            new MockitoComponentMockingRule<Vocabulary>(GeneHGNCNomenclature.class);

    @Mock
    private SolrVocabularyResourceManager externalServicesAccess;

    @Mock
    private SolrClient server;

    @Mock
    private Cache<VocabularyTerm> cache;

    @Mock
    private QueryResponse response;

    @Mock
    private SolrDocumentList expectedDocList;

    @Mock
    private SolrDocument expectedDoc;

    @Mock
    private VocabularyTerm term;

    private SolrDocumentList solrDocList;

    private String dataPath;

    private String infoPath;

    private int indexReturn;


    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        solrDocList = null;
        indexReturn = -1;

        ReflectionUtils.setFieldValue(this.mocker.getComponentUnderTest(),
                "externalServicesAccess", this.externalServicesAccess);

        when(this.externalServicesAccess.getSolrConnection()).thenReturn(this.server);
        when(this.externalServicesAccess.getTermCache()).thenReturn(this.cache);

        this.dataPath = this.getClass().getResource("/HGNC-Sample.txt").toString();
        this.infoPath = this.getClass().getResource("/HGNC-info.xml").toString();

        ReflectionUtils.setFieldValue(this.mocker.getComponentUnderTest(), "dataServiceURL", this.dataPath);
    }


    @Test
    public void reindexClearsIndexAndCommitsNewTerms() throws SolrServerException, ComponentLookupException, IOException
    {
        this.indexFromResource();

        verify(this.server).deleteByQuery("*:*");
        verify(this.cache).removeAll();
        verify(this.server).add((Collection<SolrInputDocument>) Matchers.anyCollection());
        verify(this.server).commit();

        Assert.assertEquals(0 , indexReturn);

    }

    @Test
    public void testInitialize() throws ComponentLookupException, InitializationException
    {
        GeneHGNCNomenclature testInstance = (GeneHGNCNomenclature)this.mocker.getComponentUnderTest();
        testInstance.initialize();

    }

    @Test
    public void testGetTermQueryConstruction() throws ComponentLookupException, IOException, SolrServerException
    {
        CapturingMatcher<SolrQuery> queryCapture = new CapturingMatcher<>();
        when(this.server.query(Matchers.argThat(queryCapture))).thenReturn(response);
        when(this.response.getResults()).thenReturn(expectedDocList);
        this.mocker.getComponentUnderTest().getTerm("A1BG");
        Assert.assertEquals("symbol:A1BG OR prev_symbol:A1BG OR alias_symbol:A1BG",
                queryCapture.getLastValue().getQuery());
        Assert.assertNotEquals("symbol:A1CF OR prev_symbol:A1CF OR alias_symbol:A1CF",
                queryCapture.getLastValue().getQuery());
    }

    @Test
    public void testGetTermQueryConstructionWithEscapeChars() throws IOException,
            SolrServerException, ComponentLookupException
    {
        CapturingMatcher<SolrQuery> queryCapture = new CapturingMatcher<>();
        when(this.server.query(Matchers.argThat(queryCapture))).thenReturn(this.response);
        when(this.response.getResults()).thenReturn(expectedDocList);
        this.mocker.getComponentUnderTest().getTerm("+-&&||!(){}[]^\"~*?:\\");

        //TODO: make a function that
        String escapeSequenceStr = "symbol:\\+\\-\\&\\&\\|\\|\\!\\(\\)\\{\\}\\[\\]\\^\\\"\\~\\*\\?\\:\\\\ " +
                "OR prev_symbol:\\+\\-\\&\\&\\|\\|\\!\\(\\)\\{\\}\\[\\]\\^\\\"\\~\\*\\?\\:\\\\ " +
                "OR alias_symbol:\\+\\-\\&\\&\\|\\|\\!\\(\\)\\{\\}\\[\\]\\^\\\"\\~\\*\\?\\:\\\\";

        Assert.assertEquals(escapeSequenceStr, queryCapture.getLastValue().getQuery());
    }

    @Test
    public void getStringDistanceIsFlat() throws ComponentLookupException
    {
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getDistance("A", "B"));
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getDistance("A", "A"));
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getDistance("A", null));
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getDistance(null, "B"));
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getDistance((String) null, null));
    }

    @Test
    public void getTermDistanceIsFlat() throws ComponentLookupException
    {
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getDistance(this.term, this.term));
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getDistance(this.term, mock(VocabularyTerm.class)));
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getDistance(this.term, null));
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getDistance(null, this.term));
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getDistance((VocabularyTerm) null, null));
    }

    private void indexFromResource() throws IOException, SolrServerException, ComponentLookupException
    {
        CapturingMatcher<Collection<SolrInputDocument>> allTermsCap = new CapturingMatcher<>();
        when(this.server.add(Matchers.argThat(allTermsCap))).thenReturn(new UpdateResponse());

        indexReturn = this.mocker.getComponentUnderTest().reindex(dataPath);

        Collection<SolrInputDocument> allTerms = allTermsCap.getLastValue();
        solrDocList = new SolrDocumentList();
        for(SolrInputDocument i : allTerms) {
            solrDocList.add(ClientUtils.toSolrDocument(i));
        }
    }

}


package org.phenotips.export.internal;


import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DataToCellConverterTest {

    private DataToCellConverter converter;

    @Before
    public void setUp() throws Exception
    {
        this.converter = new DataToCellConverter();
    }

    @Test
    public void phenotypSetupAddsOnlyEnabledFields() throws Exception
    {
        Set<String> enablingFields = new HashSet<>();
        enablingFields.addAll(Arrays.asList("phenotype", "phenotype_combined"));

        this.converter.phenotypeSetup(enablingFields);
    }

    @Test
    public void phenotypeHeader() throws Exception
    {
        Set<String> enablingFields = new HashSet<>();
        enablingFields.addAll(Arrays.asList("phenotype", "phenotype_combined"));

        this.converter.phenotypeSetup(enablingFields);
    }
}



/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.ssehub.kernel_haven.pss_mapper;

import java.util.List;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * This class creates {@link ProblemSolutionSpaceMapping}s.
 * 
 * @author Christian Kröher
 *
 */
public class ProblemSolutionSpaceMapper extends AnalysisComponent<MappingElement> {
    
    private static final String VARIABLE_REGEX_PROPERTY = "analysis.pss_mapper.variable_regex";
    
    /**
     * The code extractor for providing information from code artifacts.
     */
    private @NonNull AnalysisComponent<SourceFile<?>> codeExtractor;
    
    /**
     * The build extractor for providing information from the build system. This extractor may be <code>null</code> if
     * build information is not desired (or possible) to be included during mapping creation.
     */
    private AnalysisComponent<BuildModel> buildExtractor;
    
    /**
     * The regular expression for identifying referenced variability model variables in build and code artifacts.
     */
    private String variableReferenceRegex;
    
    /**
     * The variability model extractor for providing information from variability model artifacts.
     */
    private @NonNull AnalysisComponent<VariabilityModel> vmExtractor;
    
    /**
     * Creates a {@link ProblemSolutionSpaceMapper} instance, which considers and, hence, requires information of all
     * three input pipelines.
     * 
     * @param config the global {@link Configuration}
     * @param codeExtractor a particular code extractor as defined in the properties file for providing code information
     * @param buildExtractor a particular build extractor as defined in the properties file for providing build
     *        information
     * @param vmExtractor a particular variability model extractor as defined in the properties file for providing
     *        variability model information
     * @throws SetUpException if setting-up this instance failed
     */
    @SuppressWarnings("deprecation")
    public ProblemSolutionSpaceMapper(@NonNull Configuration config,
            @NonNull AnalysisComponent<SourceFile<?>> codeExtractor,
            @NonNull AnalysisComponent<BuildModel> buildExtractor,
            @NonNull AnalysisComponent<VariabilityModel> vmExtractor) throws SetUpException {
        super(config);
        this.codeExtractor = codeExtractor;
        this.buildExtractor = buildExtractor;
        this.vmExtractor = vmExtractor;
        variableReferenceRegex = config.getProperty(VARIABLE_REGEX_PROPERTY);
    }
    
    /**
     * Creates a {@link ProblemSolutionSpaceMapper} instance, which only considers and, hence, requires information of
     * the code and variability model pipelines.
     * 
     * @param config the global {@link Configuration}
     * @param codeExtractor a particular code extractor as defined in the properties file for providing code information
     * @param vmExtractor a particular variability model extractor as defined in the properties file for providing
     *        variability model information
     * @throws SetUpException if setting-up this instance failed
     */
    @SuppressWarnings("deprecation")
    public ProblemSolutionSpaceMapper(@NonNull Configuration config,
            @NonNull AnalysisComponent<SourceFile<?>> codeExtractor,
            @NonNull AnalysisComponent<VariabilityModel> vmExtractor) throws SetUpException {
        super(config);
        this.codeExtractor = codeExtractor;
        this.buildExtractor = null;
        this.vmExtractor = vmExtractor;
        variableReferenceRegex = config.getProperty(VARIABLE_REGEX_PROPERTY);
    }
    
    @Override
    protected void execute() {
        // TODO we will not find something like obj-$(CONFIG_ADDITION) := test.o if test.c does not exist
        LOGGER.logInfo2("Using \"" + variableReferenceRegex 
                + "\" to identify variability model variables in build and code artifacts");

        // Get mandatory variability model
        VariabilityModel variabilityModel = vmExtractor.getNextResult();
        if (variabilityModel == null) {
            LOGGER.logError2("Variability modell is missig but fundamental for creating the mapping");
            return;
        }
        
        // Create new mapping using the variability model as baseline input
        ProblemSolutionSpaceMapping mapping = new ProblemSolutionSpaceMapping(variabilityModel);
        SourceFile<?> codeFile;
        // Get optional build model: if not available, there will be no build mapping
        if (buildExtractor != null) {
            BuildModel buildModel = buildExtractor.getNextResult();
            if (buildModel != null) {                
                /*
                 * Try to add all relations between code files and configuration variables (build mapping) as well as
                 * all relations between code elements of those code files and configuration variables (code mapping)
                 */
                while ((codeFile = codeExtractor.getNextResult()) != null) {
                    mapping.add(codeFile, buildModel.getPc(codeFile.getPath()), variableReferenceRegex);
                }
            } else {
                // Should not happen
                LOGGER.logError2("Build extractor does not provide build model",
                        "Either correct build extractor or exclude it from configuration");
            }
        } else {
            LOGGER.logWarning2("Build model is missig, which may lead to incomplete mapping");
            /*
             * Try to add all relations between code elements of available code files and configuration variables (code
             * mapping)
             */
            while ((codeFile = codeExtractor.getNextResult()) != null) {
                mapping.add(codeFile, variableReferenceRegex);
            }
        }
        
        /*
         * Try to resolve unused variables, e.g., to avoid false "UNUSED" states due to exclusive usage in the
         * variability model. We can argue here, that the variable is still unused with respect to the mapping to the
         * solution space artifacts, but for later processing we need to distinguish to what extent this variable is
         * unused: only in mapping, which means state "UNMAPPED", or really "UNUSED" as it is not even used in the
         * variability model.
         */
        mapping.resolveUnused();
        
        // Only used for debugging
        mapping.show();
        
        /*
         * As mapping elements may change as long as processing build and code information has not been finished, we can
         * add the final results only after all build and code artifacts are processed.
         */
        List<MappingElement> mappingElements = mapping.getElements();
        if (mappingElements != null) {
            for (MappingElement element : mappingElements) {
                addResult(element);
            }
        }
        LOGGER.logInfo2("Mapping with " + mapping.getElements().size() + " elements created");
    }
    
    @Override
    public @NonNull String getResultName() {
        return "PSS_Mapping";
    }
    
}

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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.Setting;
import net.ssehub.kernel_haven.config.Setting.Type;
import net.ssehub.kernel_haven.incremental.storage.HybridCache;
import net.ssehub.kernel_haven.incremental.storage.IncrementalPostExtraction;
import net.ssehub.kernel_haven.util.FormatException;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;

/**
 * This class creates {@link ProblemSolutionSpaceMapping}s.
 * 
 * @author Christian Kröher, Moritz Flöter
 *
 */
public class IncrementalProblemSolutionSpaceMapper extends AnalysisComponent<MappingElement> {

    private static final Setting<@Nullable Pattern> VARIABLE_REGEX_SETTING = new Setting<>(
            "analysis.pss_mapper.variable_regex", Type.REGEX, false, null,
            "This regular expression is used to "
                    + "identify variability variables in code and build model artifacts. If this is not specified, all "
                    + "variables are considerd to be variability variables.");

    /**
     * The code extractor for providing information from code artifacts.
     */
    private HybridCache hybridCache;
    private Pattern variableReferenceRegex;

    /**
     * Creates a special {@link IncrementalProblemSolutionSpaceMapper} which does
     * not have a variability model. This will create {@link MappingState#UNDEFINED}
     * entries for all variables found in the code model.
     * 
     * @param config                    the global {@link Configuration}
     * @param incrementalPostExtraction a particular code extractor as defined in
     *                                  the properties file for providing code
     *                                  information
     * 
     * @throws SetUpException if setting-up this instance failed
     */
    public IncrementalProblemSolutionSpaceMapper(@NonNull Configuration config,
            IncrementalPostExtraction incrementalPostExtraction) throws SetUpException {
        super(config);
        this.hybridCache = incrementalPostExtraction.getNextResult();
        config.registerSetting(VARIABLE_REGEX_SETTING);
        this.variableReferenceRegex = config.getValue(VARIABLE_REGEX_SETTING);
    }

    @Override
    protected void execute() {
        LOGGER.logInfo2("Using ", variableReferenceRegex != null ? variableReferenceRegex.pattern() : "null",
                " to identify variability model variables in build and code artifacts");

        ProblemSolutionSpaceMapping mapping;

        VariabilityModel variabilityModel = null;
        BuildModel buildModel = null;
        Collection<SourceFile<?>> codeModel = null;

        try {
            variabilityModel = hybridCache.readVm();
        } catch (FormatException | IOException e) {
            e.printStackTrace();
        }

        try {
            buildModel = hybridCache.readBm();
        } catch (FormatException | IOException e) {
            e.printStackTrace();
        }

        try {
            codeModel = hybridCache.readCm();
        } catch (FormatException | IOException e) {

        }

        LOGGER.logInfo2("Using ", variableReferenceRegex != null ? variableReferenceRegex.pattern() : "null",
                " to identify variability model variables in build and code artifacts");

        if (variabilityModel == null) {
            LOGGER.logError2("Creating a ProblemSolutionSpaceMapper without a variability model is only useful in a "
                    + "very few special cases", "You may want to supply a variability model");
            mapping = new ProblemSolutionSpaceMapping();
        } else {
            mapping = new ProblemSolutionSpaceMapping(variabilityModel);
        }

        if (buildModel != null && codeModel != null) {
            /*
             * Try to add all relations between code files and configuration variables
             * (build mapping) as well as all relations between code elements of those code
             * files and configuration variables (code mapping)
             */
            for (SourceFile<?> codeFile : codeModel) {
                mapping.add(codeFile, buildModel.getPc(codeFile.getPath()), variableReferenceRegex);
            }
        } else if (codeModel != null) {
            LOGGER.logWarning2("Build model is missig, which may lead to incomplete mapping");
            /*
             * Try to add all relations between code elements of available code files and
             * configuration variables (code mapping)
             */
            for (SourceFile<?> codeFile : codeModel) {
                mapping.add(codeFile, variableReferenceRegex);
            }

        }

        /*
         * Try to resolve unused variables, e.g., to avoid false "UNUSED" states due to
         * exclusive usage in the variability model. We can argue here, that the
         * variable is still unused with respect to the mapping to the solution space
         * artifacts, but for later processing we need to distinguish to what extent
         * this variable is unused: only in mapping, which means state "UNMAPPED", or
         * really "UNUSED" as it is not even used in the variability model.
         */
        mapping.resolveUnused();
        // Only used for debugging
        mapping.show();
        
        /*
         * As mapping elements may change as long as processing build and code
         * information has not been finished, we can add the final results only after
         * all build and code artifacts are processed.
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

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.pss_mapper.MappingElement.MappingState;
import net.ssehub.kernel_haven.util.Logger;
import net.ssehub.kernel_haven.util.logic.Formula;
import net.ssehub.kernel_haven.util.logic.VariableFinder;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * This class represents a mapping between problem and solution space artifacts. This mapping is variability-driven,
 * which means that it provides relations between these artifacts in terms of variables typically defined in the
 * {@link VariabilityModel} and references to these variables in the {@link BuildModel} and all {@link SourceFile}s
 * provided by the respective extractors. 
 * 
 * @author Christian Kröher
 *
 */
public class ProblemSolutionSpaceMapping {
    
    /**
     * This map contains all {@link MappingElement}s (values) created for the individual configuration variables (keys
     * [their name]).
     */
    private @NonNull Map<String, MappingElement> mappingElements;
    
    /**
     * Defines whether the {@link VariabilityVariable}s of the {@link VariabilityModel} provide additional information
     * about their usage in constraints (<code>true</code>) or not (<code>false</code>). The availability of this
     * information depends on the variability model extractor in use. 
     */
    private boolean variableConstraintUsageAvailable;
    
    /**
     * Creates a {@link ProblemSolutionSpaceMapping} instance.
     * 
     * @param variabilityModel the {@link VariabilityModel} provided by the respective extractor
     */
    public ProblemSolutionSpaceMapping(@NonNull VariabilityModel variabilityModel) {
        /*
         * Initialize the map of VariabilityVariables and associated MappingElements based on the variables defined in
         * the variability model. The initial state of these variables is UNUSED. This state may change, if references
         * to these variables will be found in build or code artifacts. 
         */
        mappingElements = new HashMap<String, MappingElement>();
        variableConstraintUsageAvailable = variabilityModel.getDescriptor().hasConstraintUsage();
        Set<VariabilityVariable> variables = variabilityModel.getVariables();
        for (VariabilityVariable variable : variables) {
            mappingElements.put(variable.getName(), new MappingElement(variable));
        }
    }
    
    /**
     * Extends this mapping by additional relations between problem and solution space artifacts, if such relations
     * exit in the given parameters. Based on the set of {@link VariabilityVariable}s provided for constructing this
     * mapping, this method performs a:
     * <ul>
     * <li><b>Build mapping update</b> by scanning the given presence condition for configuration variables. If such a
     *        variable is found, a new relation between that variable and the given source file is added. This also
     *        leads to either defining the variable to be {@link MappingState#USED}, if it is defined in the
     *        {@link VariabilityModel}, or saving it in a separate set as being {@link MappingState#UNDEFINED}.</li>
     * <li><b>Code mapping update</b> by scanning the presence conditions of each {@link CodeElement} of the given
     *        source file in a similar way as during the build mapping update described above. The difference here is,
     *        that relations between variables and the individual code elements are added.</li>
     * </ul>
     * 
     * @param sourceFile the {@link SourceFile}, which may be added entirely to this mapping and/or its individual
     *        {@link CodeElement}s if a relation to a variable is detected
     * @param presenceCondition the {@link Formula} representing the presence condition, which controls the presence
     *        or absence of the given source file; while the value can be <code>null</code> if a {@link BuildModel}
     *        is not available, using {@link #add(SourceFile)} is recommended in this case
     * @param variableReferenceRegex the regular expression for identifying referenced variability model variables in
     *        build and code artifacts; can be <code>null</code> if no such expression is defined by the user, which
     *        leads to including all variables found
     */
    public void add(@NonNull SourceFile sourceFile, Formula presenceCondition, String variableReferenceRegex) {
        if (presenceCondition != null) {
            updateBuildMapping(sourceFile, presenceCondition, variableReferenceRegex);
        }
        add(sourceFile, variableReferenceRegex);
    }
    
    /**
     * Updates the internal {@link ProblemSolutionSpaceMapping#mappingElements} by scanning the given presence condition
     * for configuration variables. If such a variable is found, a new relation between that variable and the given
     * source file is added. This also leads to either defining the variable to be {@link MappingState#USED}, if it is
     * defined in the {@link VariabilityModel}, or creating a new {@link MappingElement} for that variable, which then
     * has the state {@link MappingState#UNDEFINED}.
     * 
     * @param sourceFile the {@link SourceFile}, which may be added to this mapping, if a configuration variable in the
     *        given presence condition is detected
     * @param presenceCondition the {@link Formula} representing the presence condition, which controls the presence or
     *        absence of the given source file
     * @param variableReferenceRegex the regular expression for identifying referenced variability model variables in
     *        build and code artifacts; can be <code>null</code> if no such expression is defined by the user, which
     *        leads to including all variables found
     */
    private void updateBuildMapping(@NonNull SourceFile sourceFile, @NonNull Formula presenceCondition,
            String variableReferenceRegex) {
        // Get all variables used to define the presence condition
        VariableFinder variableFinder = new VariableFinder();
        variableFinder.visit(presenceCondition);
        List<String> presenceConditionVariables =
                cleanVariableList(variableFinder.getVariableNames(), variableReferenceRegex);
        // Iterate presence condition variables to create new or extend existing relations
        for (String variableName : presenceConditionVariables) {
            MappingElement mappingElement = mappingElements.get(variableName);
            if (mappingElement == null) {
                // The variable is unknown and, hence, not defined in the variability model
                mappingElement = new MappingElement(variableName);
                mappingElements.put(variableName, mappingElement);
            }
            mappingElement.addBuildMapping(sourceFile);
        }
    }
    
    /**
     * Extends this mapping by additional relations between problem and solution space artifacts. Based on the set of
     * {@link VariabilityVariable}s provided for constructing this mapping, this method <b>exclusively performs a code
     * mapping update</b> by scanning the presence conditions of each {@link CodeElement} of the given source file for 
     * configuration variables. If such a variable is found, a new relation between that variable and the scanned code
     * element is added. This also leads to either defining the variable to be {@link MappingState#USED}, if it is
     * defined in the {@link VariabilityModel}, or creating a new {@link MappingElement} for that variable, which then
     * has the state {@link MappingState#UNDEFINED}.
     * 
     * @param sourceFile the {@link SourceFile} containing the {@link CodeElement}s, which may be added if their
     *        presence conditions include references to configuration variables
     * @param variableReferenceRegex the regular expression for identifying referenced variability model variables in
     *        build and code artifacts; can be <code>null</code> if no such expression is defined by the user, which
     *        leads to including all variables found
     */
    public void add(@NonNull SourceFile sourceFile, String variableReferenceRegex) {
        updateCodeMapping(sourceFile, variableReferenceRegex);
    }
    
    /**
     * Updates the {@link ProblemSolutionSpaceMapping#mappingElements} by scanning the presence conditions of each
     * {@link CodeElement} of the given source file for configuration variables. If such a variable is found, a new
     * relation between that variable and the scanned code element is added. This also leads to either defining the
     * variable to be {@link MappingState#USED}, if it is defined in the {@link VariabilityModel}, or creating a new
     * {@link MappingElement} for that variable, which then has the state {@link MappingState#UNDEFINED}.
     * 
     * @param sourceFile the {@link SourceFile} containing the {@link CodeElement}s, which may be added if their
     *        presence conditions include references to configuration variables
     * @param variableReferenceRegex the regular expression for identifying referenced variability model variables in
     *        build and code artifacts; can be <code>null</code> if no such expression is defined by the user, which
     *        leads to including all variables found
     */
    private void updateCodeMapping(@NonNull SourceFile sourceFile, String variableReferenceRegex) {
        for (CodeElement codeElement : sourceFile) {
            updateCodeMapping(codeElement, variableReferenceRegex);
        }
    }
    
    /**
     * Updates the {@link ProblemSolutionSpaceMapping#mappingElements} by scanning <b>recursively</b> the presence
     * conditions of the given {@link CodeElement} <b>and all nested code elements</b> for configuration variables. If
     * such a variable is found, a new relation between that variable and the scanned code element is added. This also
     * leads to either defining the variable to be {@link MappingState#USED}, if it is defined in the
     * {@link VariabilityModel}, or creating a new {@link MappingElement} for that variable, which then has the state
     * {@link MappingState#UNDEFINED}.
     * 
     * @param codeElement the {@link CodeElement}, which may be added if its presence condition includes references to
     *        configuration variables; nested elements will be considered recursively
     * @param variableReferenceRegex the regular expression for identifying referenced variability model variables in
     *        build and code artifacts; can be <code>null</code> if no such expression is defined by the user, which
     *        leads to including all variables found
     */
    private void updateCodeMapping(CodeElement codeElement, String variableReferenceRegex) {
        if (codeElement != null) {
            Formula codeElementPresenceCondition = codeElement.getPresenceCondition();
            if (codeElementPresenceCondition != null) {
                // Get all variables used to define the presence condition
                VariableFinder variableFinder = new VariableFinder();
                variableFinder.visit(codeElementPresenceCondition);
                List<String> presenceConditionVariables =
                        cleanVariableList(variableFinder.getVariableNames(), variableReferenceRegex);
                // Iterate presence condition variables to create new or extend existing relations
                for (String variableName : presenceConditionVariables) {
                    MappingElement mappingElement = mappingElements.get(variableName);
                    if (mappingElement == null) {
                        // The variable is unknown and, hence, not defined in the variability model
                        mappingElement = new MappingElement(variableName);
                        mappingElements.put(variableName, mappingElement);
                    }
                    mappingElement.addCodeMapping(codeElement);
                }
            }
            // Check nested code elements recursively
            for (CodeElement nestedCodeElement : codeElement.iterateNestedElements()) {
                updateCodeMapping(nestedCodeElement, variableReferenceRegex);
            }
        }
    }
    
    /**
     * Returns a clean list of variable names, which contains only those names of the given list matching the given
     * regular expression.
     * 
     * @param variableNames the list of variables names to be cleaned from those names not matching the given regular
     *        expression
     * @param variableNameRegex the regular expression for removing those variable names from the given list, which are
     *        not matching the expression; can be <code>null</code> or <i>empty</i>, which results in returning the same
     *        list of variables names without modifications
     * @return a subset list of variable names matching the given regular expression; can be <i>empty</i> if no variable
     *         name is matching the given regular expression
     */
    public @NonNull List<String> cleanVariableList(@NonNull List<String> variableNames, String variableNameRegex) {
        List<String> cleanVariableList = new ArrayList<String>();
        if (variableNameRegex == null || variableNameRegex.isEmpty()) {
            cleanVariableList = variableNames;
        } else {
            for (String variableName : variableNames) {
                if (variableName.matches(variableNameRegex)) {
                    cleanVariableList.add(variableName);
                }
            }
        }
        return cleanVariableList;
    }
    
    /**
     * Resolves all {@link MappingElement}s with a {@link VariabilityVariable} in state {@link MappingState#UNUSED} by
     * checking whether the respective variable is used within the {@link VariabilityModel} only, e.g., for constraining
     * other variables. If and only if this is the case, the state will be changed to {@link MappingState#UNMAPPED}.
     * 
     * @return <code>true</code> if at least one unused variable was resolved; <code>false</code> otherwise
     */
    public boolean resolveUnused() {
        boolean unusedVariablesResolved = false;
        if (variableConstraintUsageAvailable) {
            List<MappingElement> mappingElementsList = this.getElements();
            for (MappingElement mappingElement : mappingElementsList) {
                if (mappingElement.getVariableState() == MappingState.UNUSED) {
                    Set<VariabilityVariable> referencingVariables = 
                            mappingElement.getVariable().getUsedInConstraintsOfOtherVariables();
                    if (referencingVariables != null && !referencingVariables.isEmpty()) {
                        mappingElement.setVariableState(MappingState.UNMAPPED);
                        unusedVariablesResolved = true;
                    }
                }
            }
        }
        return unusedVariablesResolved;
    }
    
    /**
     * Shows this mapping by calling the {@link Logger} to log the string-representation of each {@link MappingElement}
     * this {@link ProblemSolutionSpaceMapping} contains.
     */
    public void show() {
        for (MappingElement e : mappingElements.values()) {
            Logger.get().logDebug2(e.toString());
        }
    }
    
    /**
     * Returns all {@link MappingElement}s of this mapping.
     * 
     * @return the list of all {@link MappingElement}s or <code>null</code> if this mapping is empty.
     */
    public List<MappingElement> getElements() {
        List<MappingElement> elements = null;
        if (!mappingElements.isEmpty()) {
            elements = new ArrayList<MappingElement>(mappingElements.values());
        }
        return elements;
    }
}
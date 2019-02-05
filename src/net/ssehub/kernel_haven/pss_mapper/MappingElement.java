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

import java.util.HashSet;
import java.util.Set;

import net.ssehub.kernel_haven.build_model.BuildModel;
import net.ssehub.kernel_haven.code_model.CodeElement;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.util.io.TableElement;
import net.ssehub.kernel_haven.util.io.TableRow;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.variability_model.VariabilityModel;
import net.ssehub.kernel_haven.variability_model.VariabilityVariable;

/**
 * This class represents a single element in the {@link ProblemSolutionSpaceMapping}. It relates a particular
 * {@link VariabilityVariable} defined in the {@link VariabilityModel} or any other configuration variable, e.g., as
 * defined in build or code artifacts, with the (set of) {@link SourceFile}(s) or {@link CodeElement}s it controls. This
 * relation is determined by the {@link ProblemSolutionSpaceMapping} based on the information of the {@link BuildModel}
 * and the extracted {@link SourceFile}s.
 * 
 * @author Christian Kröher
 *
 */
@TableRow
public class MappingElement {
    
    /**
     * This enumeration defines the possible state of a {@link VariabilityVariable} within the mapping between problem
     * and solution space artifacts. This can be one of the following:
     * <ul>
     * <li>{@link MappingState#USED}</li>
     * <li>{@link MappingState#UNMAPPED}</li>
     * <li>{@link MappingState#UNUSED}</li>
     * <li>{@link MappingState#UNDEFINED}</li>
     * </ul>
     * 
     * @author Christian Kröher
     *
     */
    public enum MappingState {
        /**
         * Defines a variable as being defined in the {@link VariabilityModel} and referenced in at least one code or
         * build artifact.
         */
        USED,
        /**
         * Defines a variable as being defined in the {@link VariabilityModel} and used there as part of constraints,
         * but <b>not</b> referenced in any code or build artifact.
         */
        UNMAPPED,
        /**
         * Defines a variable as being defined in the {@link VariabilityModel}, but <b>neither</b> used in there as part
         * of constraints <b>nor</b> referenced in any code or build artifact.
         */
        UNUSED,
        /**
         * Defines a variable as <b>not</b> being defined in the {@link VariabilityModel}, but referenced in at least
         * one code or build artifact.
         */
        UNDEFINED
    };
    
    /**
     * The {@link VariabilityVariable} of this mapping element, which may control the presence or absence of the 
     * {@link MappingElement#sourceFile}s or the {@link MappingElement#codeElement}s, if these elements are not
     * <i>empty</i>. If they are <i>empty</i>, the {@link MappingElement#variableState} must be
     * {@link MappingState#UNUSED}, which is also its initial status.<br><br>
     * 
     * The value of this attribute can be <code>null</code>, if {@link MappingElement#variableState} is
     * {@link MappingState#UNDEFINED}.
     */   
    private VariabilityVariable variable;
    
    /**
     * The name of the configuration variable of this mapping, which may control the presence or absence of the 
     * {@link MappingElement#sourceFile}s or the {@link MappingElement#codeElement}s, if these elements are not
     * <i>empty</i>. In particular, this is the only available information about the configuration variable, if
     * {@link MappingElement#variableState} is {@link MappingState#UNDEFINED}.
     */
    private @NonNull String variableName;
    
    /**
     * The current {@link MappingState} of the {@link VariabilityVariable} of this mapping. The default value is
     * {@link MappingState#UNUSED}.
     */
    private @NonNull MappingState variableState;
    
    /**
     * The set of {@link SourceFile}s of this mapping element, which are controlled by the
     * {@link MappingElement#variable}. Can be <i>empty</i>, if no such relation exists.
     */
    private @NonNull Set<SourceFile<?>> sourceFiles;
    
    /**
     * The set of {@link CodeElement}s of this mapping element, which are controlled by the 
     * {@link MappingElement#variable}. Can be <i>empty</i>, if no such relation exists.
     */
    private @NonNull Set<CodeElement<?>> codeElements;
    
    /**
     * Constructs a {@link MappingElement} instance.
     * 
     * @param variable a {@link VariabilityVariable} of the {@link VariabilityModel}, which may control the presence or
     *        absence of an entire source file or a particular code element; its {@link MappingState} is automatically
     *        set to {@link MappingState#UNUSED} by this constructor
     */
    public MappingElement(VariabilityVariable variable) {
        this.variable = variable;
        this.variableName = variable.getName();
        variableState = MappingState.UNUSED;
        sourceFiles = new HashSet<>();
        codeElements = new HashSet<>();
    }
    
    /**
     * Constructs a {@link MappingElement} instance.
     * 
     * @param variable a name of a configuration variable, which is used to control the presence or absence of an entire
     *        source file or a particular code element, but is not defined as {@link VariabilityVariable} in the
     *        {@link VariabilityModel} and, hence, automatically has the status {@link MappingState#UNDEFINED}
     */
    public MappingElement(String variable) {
        this.variable = null;
        this.variableName = variable;
        variableState = MappingState.UNDEFINED;
        sourceFiles = new HashSet<>();
        codeElements = new HashSet<>();
    }
    
    /**
     * Returns the {@link VariabilityVariable} of this mapping element, which may control the presence or absence of an
     * entire {@link SourceFile} or a particular {@link CodeElement} within such a file.
     * 
     * @return the {@link VariabilityVariable} or <code>null</code>, if the status of the configuration variable of this
     *         mapping element is {@link MappingState#UNDEFINED}
     */
    public VariabilityVariable getVariable() {
        return variable;
    }
    
    /**
     * Returns the name of the configuration variable of this mapping element, which may control the presence or absence
     * of an entire {@link SourceFile} or a particular {@link CodeElement} within such a file.
     * 
     * @return the name of the variable
     */
    @TableElement(name = "Variable Name", index = 0)
    public @NonNull String getVariableName() {
        return variableName;
    }
    
    /**
     * Return the current {@link MappingState} of the configuration variable of this mapping element.
     * 
     * @return the {@link MappingState} of the variable
     */
    @TableElement(name = "Variable State", index = 1) 
    public @NonNull MappingState getVariableState() {
        return variableState;
    }
    
    /**
     * Returns the mapping of the configuration variable of this mapping element in terms of the {@link SourceFile}s the
     * variable controls during the build process.
     * 
     * @return the set of {@link SourceFile}s controlled by the variable of this mapping element; can be <i>empty</i>,
     *         if no such relation exists
     */
    public @NonNull Set<SourceFile<?>> getBuildMapping() {
        return sourceFiles;
    }
    
    /**
     * Returns the mapping of the configuration variable of this mapping element in terms of the {@link SourceFile}s the
     * variable controls during the build process as a single string.
     * 
     * @return the set of {@link SourceFile}s controlled by the variable of this mapping element as a single string; can
     *         be <i>empty</i>, if no such relation exists
     */
    @TableElement(name = "Controlled Source Files", index = 2)
    public @NonNull String getBuildMappingString() {
        String sourceFilesString = "";
        if (!sourceFiles.isEmpty()) {
            StringBuilder sourceFilesStringBuilder = new StringBuilder();
            for (SourceFile<?> sourceFile : sourceFiles) {
                sourceFilesStringBuilder.append(sourceFile.getPath().getName());
                sourceFilesStringBuilder.append(' ');
            }
            sourceFilesString = sourceFilesStringBuilder.toString();
        }
        return sourceFilesString;
    }
    
    /**
     * Returns the mapping of the configuration variable of this mapping element in terms of the {@link CodeElement}s
     * the variable controls within a certain {@link SourceFile}.
     * 
     * @return the set of {@link CodeElement}s controlled by the variable of this mapping element; can be <i>empty</i>,
     *         if no such relation exists
     */
    public @NonNull Set<CodeElement<?>> getCodeMapping() {
        return codeElements;
    }
    
    /**
     * Returns the mapping of the configuration variable of this mapping element in terms of the {@link CodeElement}s
     * the variable controls within a certain {@link SourceFile} as single string.
     * 
     * @return the set of {@link CodeElement}s controlled by the variable of this mapping element as a single string;
     *         can be <i>empty</i>, if no such relation exists
     */
    @TableElement(name = "Controlled Code Elements", index = 3)
    public @NonNull String getCodeMappingString() {
        String codeElementsString = "";
        if (!codeElements.isEmpty()) {
            StringBuilder codeElementsStringBuilder = new StringBuilder();
            for (CodeElement<?> codeElement : codeElements) {
                codeElementsStringBuilder.append(codeElement.getSourceFile().getName()
                        + "[" + codeElement.getLineStart() + ":" + codeElement.getLineEnd() + "]");
                codeElementsStringBuilder.append(' ');
            }
            codeElementsString = codeElementsStringBuilder.toString();
        }
        return codeElementsString;
    }
    
    /**
     * Sets the {@link MappingState} of the {@link VariabilityVariable} of this element to the given one.
     * 
     * @param state the new {@link MappingState} of the {@link VariabilityVariable} of this element
     */
    public void setVariableState(MappingState state) {
        variableState = state;
    }
    
    /**
     * Adds the given {@link SourceFile} to this mapping element. This means that the configuration variable of this
     * mapping element controls the presence or absence of the given file during the build process, which also changes
     * the state of that variable from {@link MappingState#UNUSED} to {@link MappingState#USED}.
     * 
     * @param sourceFile the {@link SourceFile} controlled by the variable of this mapping element
     */
    public void addBuildMapping(@NonNull SourceFile<?> sourceFile) {
        sourceFiles.add(sourceFile);
        // Do not change UNDEFINED as that state already defines that the variable is used
        if (variableState == MappingState.UNUSED) {
            variableState = MappingState.USED;
        }
    }
    
    /**
     * Adds the given {@link CodeElement} to this mapping element. This means that the configuration variable of this
     * mapping element controls the presence or absence of the given code element within a {@link SourceFile}, which
     * also changes the state of that variable from {@link MappingState#UNUSED} to {@link MappingState#USED}
     * 
     * @param codeElment the {@link CodeElement} controlled by the variable of this mapping element
     */
    public void addCodeMapping(@NonNull CodeElement<?> codeElment) {
        codeElements.add(codeElment);
        // Do not change in state UNDEFINED as that state already defines that the variable is used
        if (variableState == MappingState.UNUSED) {
            variableState = MappingState.USED;
        }
    }
    
    @Override
    public @NonNull String toString() {
        StringBuilder elementStringBuilder = new StringBuilder();
        elementStringBuilder.append(variableName);
        elementStringBuilder.append('\t');
        elementStringBuilder.append(variableState);
        elementStringBuilder.append('\t');
        elementStringBuilder.append(getBuildMappingString());
        elementStringBuilder.append('\t');
        elementStringBuilder.append(getCodeMappingString());
        return elementStringBuilder.toString();
    }
}

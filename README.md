# ProblemSolutionSpaceMapperAnalysis
The Problem-Solution-Space (PSS) Mapper is an analysis plug-in, which identifies relations between artifacts in the problem space and the solution space of a Software Product Line (SPL). In particular, it creates a mapping between features defined in the variability model (problem space) and the build rules and code elements (solution space) they control. Control structures, like runtime if- or preprocessor #ifdef-statements with conditions referencing features of the variability model, typically realize this control over build rules and code elements. Hence, the plug-in provides the mapping in terms of a list of key-value-pairs, which define a relation between a feature (key) and the (set of) entire code files (via build rules) and particular code elements it controls (value). Further, an additional attribute extends each feature in the mapping by a state. This state defines the type of relation of the respective feature, which can be one of those listed in the following table:

Feature State | Description
------------- | -----------
USED | Defines a feature as being defined in the variability model and referenced in at least one code or build artefact.
UNMAPPED | Defines a feature as being defined in the variability model and used there as part of constraints, but not referenced in any code or build artifacts. While this means that there is no mapping, we keep such features in the mapping for the sake of understandability, .e.g., to distinguish whether a feature is only unused for the mapping but used in the variability (state UNMAPPED) or unused entirely (state UNUSED).
UNUSED | Defines a feature as being defined in the variability model, but neither used there as part of constraints nor referenced in any code or build artifact.
UNDEFINED | Defines a “feature” as not being defined in the variability model, but referenced in at least one code or build artifact. As this element is not defined in the variability model, but follows the same naming convention as features, we assume that this element also represents a feature, which is currently missing in the variability model.

## Tutorials
* [Basic Video Tutorial](https://www.youtube.com/watch?v=gpBT9wiDRhE)
* [Incremental Variant Slide Tutorial](https://github.com/KernelHaven/ProblemSolutionSpaceMapperAnalysis/blob/master/Tutorials/PSS-CE%20Incremental%20Tutorial.pdf)

## KernelHaven Setup
In order to provide a problem-solution-space mapping, the analysis plug-in requires at least a variability model extractor and a code extractor. The build extractor is optional. Further, this plug-in supports a non-incremental and an incremental analysis variant. The non-incremental variant analyzes a given SPL in its current state completely, while the incremental variant only analyzes the latest changes to it. A KernelHaven configuration file for executing the respective variant of this analysis plug-in should contain the following information.

### Non-Incremental Variant
```Properties
######################
#     Directories    #
######################
resource_dir = res/
output_dir = output/
plugins_dir = plugins/
cache_dir = cache/
archive = false
source_tree = <TODO: PATH_TO_SPL>
arch = x86

##################
#     Logging    #
##################
log.dir = log/
log.console = true
log.file = true
log.level = INFO

################################
#     Code Model Parameters    #
################################
code.provider.timeout = 0
code.provider.cache.write = false
code.provider.cache.read = false
code.extractor.class =  net.ssehub.kernel_haven.undertaker.UndertakerExtractor
code.extractor.files = main
# Undertaker parses header and code files separately
code.extractor.file_regex = .*\\.(c|h)
code.extractor.threads = 2
code.extractor.add_linux_source_include_dirs = false
code.extractor.parse_to_ast = false


################################
#    Build Model Parameters    #
################################
build.provider.timeout = 0
build.provider.cache.write = false
build.provider.cache.read = false
build.extractor.class = net.ssehub.kernel_haven.kbuildminer.KbuildMinerExtractor
build.extractor.top_folders = main


#######################################
#     Variability Model Parameters    #
#######################################
variability.provider.timeout = 0
variability.provider.cache.write = false
variability.provider.cache.read = false
variability.extractor.class = net.ssehub.kernel_haven.kconfigreader.KconfigReaderExtractor

##############################
#     Analysis Parameters    #
##############################
analysis.class = net.ssehub.kernel_haven.analysis.ConfiguredPipelineAnalysis
analysis.pipeline = net.ssehub.kernel_haven.pss_mapper.ProblemSolutionSpaceMapper(cmComponent(), bmComponent(), vmComponent())
analysis.output.type = xlsx
analysis.pss_mapper.variable_regex = CONFIG_.*
```

### Incremental Variant
```Properties
######################
#     Directories    #
######################
resource_dir = res/
output_dir = output/
plugins_dir = plugins/
cache_dir = cache/
archive = false
source_tree = <TODO: PATH_TO_EMPTY_DIR>
arch = x86

##################
#     Logging    #
##################
log.dir = log/
log.console = true
log.file = true
log.level = INFO

################################
#     Code Model Parameters    #
################################
code.provider.timeout = 0
code.provider.cache.write = false
code.provider.cache.read = false
code.extractor.class = net.ssehub.kernel_haven.block_extractor.CodeBlockExtractor
code.extractor.file_regex = .*(\\.c|\\.h|\\.S)
code.extractor.fuzzy_parsing = true
code.extractor.add_pseudo_block = false

################################
#    Build Model Parameters    #
################################
build.provider.timeout = 0
build.provider.cache.write = false
build.provider.cache.read = false
build.extractor.class = net.ssehub.kernel_haven.kbuildminer.KbuildMinerExtractor

#######################################
#     Variability Model Parameters    #
#######################################
variability.provider.timeout = 0
variability.provider.cache.write = false
variability.provider.cache.read = false
variability.extractor.class = net.ssehub.kernel_haven.kconfigreader.KconfigReaderExtractor

##############################
#     Analysis Parameters    #
##############################
analysis.class = net.ssehub.kernel_haven.analysis.ConfiguredPipelineAnalysis
analysis.pipeline = net.ssehub.kernel_haven.pss_mapper.ProblemSolutionSpaceMapper(cmComponent(), bmComponent(), vmComponent())
analysis.output.type = xlsx
analysis.pss_mapper.variable_regex = CONFIG_.*

# Incremental extension setup
preparation.class.0 = net.ssehub.kernel_haven.incremental.preparation.IncrementalPreparation
incremental.hybrid_cache.dir = <TODO: PATH_TO_EMPTY_DIR>
incremental.input.source_tree_diff = <TODO: PATH_TO_GIT_DIFF_FILE>
incremental.variability_change_analyzer.execute = true
incremental.variability_change_analyzer.class = net.ssehub.kernel_haven.incremental.diff.analyzer.ComAnAnalyzer
incremental.code.filter = net.ssehub.kernel_haven.incremental.preparation.filter.ChangeFilter
incremental.build.filter = net.ssehub.kernel_haven.incremental.preparation.filter.VariabilityChangeFilter
incremental.variability.filter = net.ssehub.kernel_haven.incremental.preparation.filter.VariabilityChangeFilter
cnf.solver = SAT4J
```

Please note that the PSS Mapper plug-in is currently under development and, hence, tested only with these particular configurations.

## Usage
The PSS Mapper can be used either as single analysis for providing such a mapping for a particular SPL, or as part of an analysis pipeline. The latter allows to combine this plug-in with the [Problem-Solution-Space Divergence Detector](https://github.com/KernelHaven/ProblemSolutionSpaceDivergenceDetectorAnalysis) to identify unintended divergences between the two spaces.

## License
This plug-in is licensed under the Apache License 2.0.

## Acknowledgments
This work is partially supported by the ITEA3 project [REVaMP2](http://www.revamp2-project.eu/), funded by the BMBF (German Ministry of Research and Education) under grant 01IS16042H.

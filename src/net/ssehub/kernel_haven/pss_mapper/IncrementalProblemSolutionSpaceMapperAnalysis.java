package net.ssehub.kernel_haven.pss_mapper;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.analysis.AnalysisComponent;
import net.ssehub.kernel_haven.analysis.PipelineAnalysis;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.incremental.storage.IncrementalPostExtraction;
import net.ssehub.kernel_haven.util.null_checks.NonNull;

/**
 * Incremental variant of the  problem solution space mapper analysis.
 * 
 * @author Moritz Floeter
 */
public class IncrementalProblemSolutionSpaceMapperAnalysis extends PipelineAnalysis {

    /**
     * Instantiates a new  IncrementalProblemSolutionSpaceMapperAnalysis
     *
     * @param config the config
     */
    public IncrementalProblemSolutionSpaceMapperAnalysis(@NonNull Configuration config) {
        super(config);
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.ssehub.kernel_haven.analysis.PipelineAnalysis#createPipeline()
     */
    @Override
    protected AnalysisComponent<?> createPipeline() throws SetUpException {

    	IncrementalProblemSolutionSpaceMapper pssmapper = new IncrementalProblemSolutionSpaceMapper(config,
                new IncrementalPostExtraction(config, getCmComponent(), getBmComponent(), getVmComponent()));

        return pssmapper;

    }
}

package net.sf.mzmine.modules.rawdatamethods.filtering.scanmerger;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;

/**
 * Holds scan merger module parameters.
 *
 * @author $Author$
 * @version $Revision$
 */
public class ScanMergerParameters extends SimpleParameterSet {

    /**
     * Raw data files.
     */
    public static final RawDataFilesParameter DATA_FILES = new RawDataFilesParameter(2);

    /**
     * Raw data file suffix.
     */
    public static final StringParameter SUFFIX = new StringParameter(
            "Filename suffix", "Suffix to be appended to raw data file names.",
            "merged");

    /**
     * Remove original data file.
     */
    public static final BooleanParameter REMOVE_ORIGINAL = new BooleanParameter(
            "Remove source file after merger",
            "If checked, original files will be replaced by the merged version",
            true);

    /**
     * Create the parameter set.
     */
    public ScanMergerParameters() {
        super(new Parameter[]{DATA_FILES, SUFFIX, REMOVE_ORIGINAL});
    }
}

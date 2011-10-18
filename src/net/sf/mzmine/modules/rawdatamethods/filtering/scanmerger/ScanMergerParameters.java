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
    public static final StringParameter FILE_NAME = new StringParameter(
            "Merged file name", "Name of the resulting merged raw data file",
            "merged scans");

    /**
     * Remove original data files.
     */
    public static final BooleanParameter REMOVE_ORIGINALS = new BooleanParameter(
            "Remove source files after merger",
            "If checked, original files will be replaced by the merged version",
            true);

    /**
     * Create the parameter set.
     */
    public ScanMergerParameters() {
        super(new Parameter[]{DATA_FILES, FILE_NAME, REMOVE_ORIGINALS});
    }
}

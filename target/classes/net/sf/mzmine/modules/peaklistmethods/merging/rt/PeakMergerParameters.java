/*
 * @Author Gauthier Boaglio
 */

package net.sf.mzmine.modules.peaklistmethods.merging.rt;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.RTToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;

public class PeakMergerParameters extends SimpleParameterSet {

	public static final PeakListsParameter peakLists = new PeakListsParameter();

	public static final StringParameter suffix = new StringParameter(
			"Name suffix", "Suffix to be added to peak list name", "merged");

	public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

	public static final RTToleranceParameter rtTolerance = new RTToleranceParameter();

	/////////////// !!!!!!  The following warning is now DEPRECATED  !!!!!!
	/////////////// WARNING: In case of batch processing, if checked, the oldest ancestor will be 
	///////////////          used / transmitted in the next "batch steps" if applicable!
	// INFO:    The oldest RDF can be identical to the direct one, if none of the 
	//          rawdatamethods was applied, or if it has been removed.
	public static final BooleanParameter useOldestRDFAncestor = new BooleanParameter(
			"Use original raw data file", 
			"If checked, get peaks information from the very oldest Raw Data File ancestor (if it has not been removed). "
					+ "Otherwise: information are grabbed as usual (from the data file the peak list to be merged was built from).",
					false
			);
	// The correct value for this would be the same of the "m/z bin width" - from "Baseline correction" module.
	// (aka: something like the m/z width of a m/z peak. See 2D (or 3D) view to find out which value is appropriate)
	public static final DoubleParameter detectedMZSearchWidth = new DoubleParameter(
			"DETECTED m/z search window", 
			"Ignored if \"Use DETECTED peaks only\" or \"Use original raw data file\" is unchecked, or if \"Cumulative computing\" is checked. " 
					+ "Search the top data point among the DETECTED m/z peaks according to this window (Use 0.1, if you don't know).",
					MZmineCore.getConfiguration().getMZFormat(), PeakMergerTask.getDoublePrecision()
			);

	public static final BooleanParameter useOnlyDetectedPeaks = new BooleanParameter(
			"Use DETECTED peaks only",
			"If checked, resulting merged peaks are computed from DETECTED peaks only. "
					+ "Otherwise: information are grabbed from the whole raw data file " 
					+ "(takes all the intermediate m/z values within the specified \"m/z tolerance\").",
					true
			);

	public static final BooleanParameter cumulativeComputing = new BooleanParameter(
			"Cumulative computing mode (TIC)",
			"YABON cocher ça pour toi Julien !!! "
					+ "If checked, merged peaks are computed cumulatively: "
					+ "Peaks and their area, intensity and misc averages are processed from TIC (Total Ion Count). "
					+ "Otherwise: everything is determined from Base Peak Intensity.",
					false
			);


	public static final BooleanParameter autoRemove = new BooleanParameter(
			"Remove original peaklist",
			"If checked, original peaklist will be removed and only merged version remains");

	public PeakMergerParameters() {
		super(new Parameter[] { 
				peakLists, suffix, mzTolerance, rtTolerance,
				useOldestRDFAncestor, detectedMZSearchWidth, useOnlyDetectedPeaks, cumulativeComputing,
				autoRemove });
	}

}

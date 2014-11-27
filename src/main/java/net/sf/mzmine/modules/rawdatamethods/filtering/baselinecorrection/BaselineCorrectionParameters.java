/*
 * Copyright 2006-2014 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection;


import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection.correctors.AsymmetryCorrector;
import net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection.correctors.LocMinLoessCorrector;
import net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection.correctors.PeakDetectionCorrector;
import net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection.correctors.RollingBallCorrector;
import net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection.correctors.RubberBandCorrector;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.MSLevelParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.ModuleComboParameter;
import net.sf.mzmine.parameters.parametertypes.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.RSession.RengineType;
import java.util.logging.Logger;

/**
 * Holds baseline correction module COMMON parameters.
 * See "net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection.correctors" sub-package
 * for method specific parameters.
 * 
 * @author $Author$
 * @version $Revision$
 * 
 * Reduced strictly to the parameters common to all the BaselineCorrectors.
 */
public class BaselineCorrectionParameters extends SimpleParameterSet {
	
	// Logger.
	private static final Logger LOG = Logger.getLogger(BaselineCorrectionParameters.class.getName());
	
	// Keep access to those parameters (use only from ParametersDialogs).
	private static BaselineCorrectionParameters baselineCorrectionParameters = null;
	protected static BaselineCorrectionParameters getBaselineCorrectionParameters() {
		return baselineCorrectionParameters;
	}
	
	

	public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();
	
	/**
	 * Raw data file suffix.
	 */
	public static final StringParameter SUFFIX = new StringParameter(
			"Filename suffix", "Suffix to be appended to raw data file names.",
			"baseline-corrected");

	/**
	 * R engine type.
	 */
	public static final ComboParameter<RengineType> RENGINE_TYPE = new ComboParameter<RengineType>(
			"R engine type",
			"The type of R engine to be used for computing.",
			RengineType.values(), RengineType.Rserve);
	
	/**
	 * Chromatogram type.
	 */
	public static final ComboParameter<ChromatogramType> CHROMOTAGRAM_TYPE = new ComboParameter<ChromatogramType>(
			"Chromatogram type",
			"The type of chromatogram from which infer a baseline to be corrected.",
			ChromatogramType.values(), ChromatogramType.TIC);

	/**
	 * List of available baseline correctors 
	 */
	public static /*final*/ BaselineCorrector baselineCorrectors[] = { 
		new AsymmetryCorrector(),		// (Package R "ptw"				- http://cran.r-project.org/web/packages/ptw/ptw.pdf)
		new RollingBallCorrector(),		// (Package R "baseline"		- http://cran.r-project.org/web/packages/baseline/baseline.pdf)
		new PeakDetectionCorrector(), 	// (Package R "baseline" 		- http://cran.r-project.org/web/packages/baseline/baseline.pdf)
		new RubberBandCorrector(),  	// (Package R "hyperSpec" 		- http://cran.r-project.org/web/packages/hyperSpec/vignettes/baseline.pdf)
		new LocMinLoessCorrector()		// (Package R/Bioc. "PROcess" 	- http://bioconductor.org/packages/release/bioc/manuals/PROcess/man/PROcess.pdf)
	};

	public static final ModuleComboParameter<BaselineCorrector> BASELINE_CORRECTORS = new ModuleComboParameter<BaselineCorrector>(
			"Correction method", "Alternative baseline correction methods", baselineCorrectors);

	/**
	 * Apply in bins.
	 */
	public static final BooleanParameter USE_MZ_BINS = new BooleanParameter(
			"Use m/z bins",
			"If checked, then full m/z range will be divided into bins a baseline correction applied to each bin (see m/z bin width).",
			true);

	/**
	 * M/Z bin width.
	 */
	public static final DoubleParameter MZ_BIN_WIDTH = new DoubleParameter(
			"m/z bin width", "The m/z bin size (>= 0.001) to use when the \""
					+ USE_MZ_BINS.getName() + "\" option is enabled.",
					MZmineCore.getConfiguration().getMZFormat(), 1.0, 0.001, null);

	/**
	 * MS-level.
	 */
	public static final MSLevelParameter MS_LEVEL = new MSLevelParameter();

	/**
	 * Remove original data file.
	 */
	public static final BooleanParameter REMOVE_ORIGINAL = new BooleanParameter(
			"Remove source file after baseline correction",
			"If checked, original file will be replaced by the corrected version",
			true);


	/**
	 * Create the parameter set.
	 */
	public BaselineCorrectionParameters() {
		super(new Parameter[] { dataFiles, SUFFIX, 
				RENGINE_TYPE,
				CHROMOTAGRAM_TYPE, MS_LEVEL,
				USE_MZ_BINS, MZ_BIN_WIDTH,
				BASELINE_CORRECTORS,
				REMOVE_ORIGINAL });
	}

	/**
	 * Use an InstantUpdateSetupDialog setup dialog instead of the regular one.
	 */
	@Override
	public ExitCode showSetupDialog() {
		Parameter[] parameters = this.getParameters();
		if ((parameters == null) || (parameters.length == 0))
			return ExitCode.OK;
		
		baselineCorrectionParameters = this;
		ParameterSetupDialog dialog = new InstantUpdateSetupDialog(this);
		dialog.setVisible(true);
		return dialog.getExitCode();
	}

}

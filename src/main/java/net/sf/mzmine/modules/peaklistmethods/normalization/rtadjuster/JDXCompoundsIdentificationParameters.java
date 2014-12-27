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

package net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster;

import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.RTRangeParameter;
import net.sf.mzmine.parameters.parametertypes.RTToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;

public class JDXCompoundsIdentificationParameters extends SimpleParameterSet {

	public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

    public static final StringParameter SUFFIX = new StringParameter(
    	    "Name suffix", "Suffix to be added to peak list name", "adjusted");

    //    public static final ComboParameter<IonizationType> ionizationType = new ComboParameter<IonizationType>(
	//	    "Ionization type", "Ionization type", IonizationType.values());
	//
	//    public PeakListIdentificationParameters() {
	//	super(new Parameter[] { peakLists,
	//		SingleRowIdentificationParameters.DATABASE, ionizationType,
	//		SingleRowIdentificationParameters.MAX_RESULTS,
	//		SingleRowIdentificationParameters.MZ_TOLERANCE,
	//		SingleRowIdentificationParameters.ISOTOPE_FILTER });
	//    }

	public static final FileNameParameter JDX_FILE_C1 = new FileNameParameter(
			"Compound 1 file (JDX)", "JDX file for standard compound 1", ".jdx");
	public static final RTRangeParameter RT_SEARCH_WINDOW_C1 = new RTRangeParameter(
			"Compound 1 search window", "Search window for standard compound 1");
	public static final FileNameParameter JDX_FILE_C2 = new FileNameParameter(
			"Compound 2 file (JDX)", "JDX file for standard compound 2", ".jdx");
	public static final RTRangeParameter RT_SEARCH_WINDOW_C2 = new RTRangeParameter(
			"Compound 2 search window", "Search window for standard compound 2");

	public static final ComboParameter<SimilarityMethodType> SIMILARITY_METHOD = new ComboParameter<SimilarityMethodType>(
			"Similarity method", "Similarity method", SimilarityMethodType.values());
	public static final DoubleParameter MIX_FACTOR = new DoubleParameter(
			"Area Mix factor", "Weight for balancing between Similarity and Area (0.0 is Similarity Only)",
			MZmineCore.getConfiguration().getIntensityFormat(), 0.0, 0.0, 1.0);
	
	public JDXCompoundsIdentificationParameters() {
		super(new Parameter[] { PEAK_LISTS,
				SUFFIX,
				JDX_FILE_C1, RT_SEARCH_WINDOW_C1, 
				JDX_FILE_C2, RT_SEARCH_WINDOW_C2, 
				SIMILARITY_METHOD, MIX_FACTOR });
	}

}

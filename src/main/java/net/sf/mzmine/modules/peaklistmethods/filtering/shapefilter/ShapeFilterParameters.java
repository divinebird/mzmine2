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

package net.sf.mzmine.modules.peaklistmethods.filtering.shapefilter;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;

/**
 * @description TODO
 * @author Gauthier Boaglio
 * @date Nov 30, 2014
 */
public class ShapeFilterParameters extends SimpleParameterSet {

	public static final PeakListsParameter PEAKLISTS = new PeakListsParameter();

	public static final StringParameter SUFFIX = new StringParameter("Suffix",
			"This string is added to filename as suffix", "shape filtered peaks");


	public static final BooleanParameter AVG_EDGES = new BooleanParameter(
            "Average edges",
            "If checked, the ratio of peak top/edge uses the average between left and right edges. Uses the minimum between the two of them otherwise.",
            true);

	public static final DoubleParameter MIN_RATIO = new DoubleParameter(
            "Min ratio of peak top/edge",
            "Minimum ratio between peak's top intensity and side (lowest) data points. This parameter helps to reduce detection of false peaks in case the chromatogram is not smooth.");

	public static final DoubleParameter MASS_RESOLUTION = new DoubleParameter(
			"Mass resolution",
			"Mass resolution is the dimensionless ratio of the mass of the peak divided by its width."
					+ " Peak width is taken as the full width at half maximum intensity (FWHM).");

	public static final ComboParameter<FilterShapeModel> SHAPEMODEL_TYPE = new ComboParameter<FilterShapeModel>(
			"Shape model", "This value defines the type of shape model",
			FilterShapeModel.values());
	

	public static final BooleanParameter AUTOREMOVE = new BooleanParameter(
			"Remove original peaklist",
			"If checked, original peaklist will be removed and only deisotoped version remains");


	
	public ShapeFilterParameters() {
		super(new Parameter[] { PEAKLISTS, SUFFIX, AVG_EDGES, MIN_RATIO, MASS_RESOLUTION, SHAPEMODEL_TYPE, AUTOREMOVE });
	}

}

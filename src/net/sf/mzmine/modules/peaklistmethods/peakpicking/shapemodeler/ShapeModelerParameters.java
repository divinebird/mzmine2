/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.peakpicking.shapemodeler;

import java.text.NumberFormat;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;

public class ShapeModelerParameters extends SimpleParameterSet {

	public static final String shapeModelerNames[] = { "Triangle", "Gaussian",
			"EMG" };

	public static final String shapeModelerClasses[] = {
			"net.sf.mzmine.modules.peaklistmethods.peakpicking.shapemodeler.peakmodels.TrianglePeakModel",
			"net.sf.mzmine.modules.peaklistmethods.peakpicking.shapemodeler.peakmodels.GaussianPeakModel",
			"net.sf.mzmine.modules.peaklistmethods.peakpicking.shapemodeler.peakmodels.EMGPeakModel" };

	public static final Parameter shapeModelerType = new SimpleParameter(
			ParameterType.STRING, "Shape model",
			"This value defines the type of shape model", null,
			shapeModelerNames);

	public static final Parameter suffix = new SimpleParameter(
			ParameterType.STRING, "Suffix",
			"This string is added to filename as suffix",
			(Object) "shaped peaks");

	public static final Parameter massResolution = new SimpleParameter(
			ParameterType.INTEGER,
			"Mass resolution",
			"Mass resolution is the dimensionless ratio of the mass of the peak divided by its width."
					+ " Peak width is taken as the full width at half maximum intensity (FWHM).",
			null, new Integer(60000), new Integer(0), null, NumberFormat
					.getIntegerInstance());

	public static final Parameter autoRemove = new SimpleParameter(
			ParameterType.BOOLEAN,
			"Remove original peak list",
			"If checked, original peak list will be removed and only resolved version remains",
			new Boolean(false));

	public ShapeModelerParameters() {
		super(new Parameter[] { suffix, massResolution, shapeModelerType,
				autoRemove });
	}

}

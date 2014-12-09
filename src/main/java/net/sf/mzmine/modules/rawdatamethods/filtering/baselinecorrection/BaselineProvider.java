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

package net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection;

import org.rosuda.REngine.Rserve.RserveException;

import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.RSessionWrapper;
import net.sf.mzmine.util.RSessionWrapperException;

/**
 * @description Base interface for providing a new way for computing baselines.
 * 
 * @author Gauthier Boaglio
 * @date Nov 6, 2014
 */
public interface BaselineProvider {

	/**
	 * Gets R required packages for the corrector's method, if applicable
	 */
	public String[] getRequiredRPackages();

	/**
	 * Returns a baseline for correcting the given chromatogram using R
	 * @throws RSessionWrapperException 
	 * @throws BaselineCorrectionException 
	 * @throws RserveException 
	 * @throws InterruptedException 
	 */
	public double[] computeBaseline(final RSessionWrapper rSession, final RawDataFile origDataFile, 
			final double[] chromatogram, ParameterSet parameters) throws RSessionWrapperException;

}

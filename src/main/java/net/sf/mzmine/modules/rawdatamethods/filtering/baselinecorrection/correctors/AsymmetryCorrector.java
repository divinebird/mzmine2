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

package net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection.correctors;

import javax.annotation.Nonnull;

import org.rosuda.JRI.Rengine;

import net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection.BaselineCorrector;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.RUtilities;

/**
 * @description Asymmetric baseline corrector. Estimates a trend based on asymmetric least squares.
 * Uses "asysm" feature from "ptw" R-package (http://cran.r-project.org/web/packages/ptw/ptw.pdf).
 * 
 * @author Gauthier Boaglio
 * @date Nov 6, 2014
 */
public class AsymmetryCorrector extends BaselineCorrector {
	
	@Override
	public String[] getRequiredRPackages() {
		return new String[] { "ptw" };
	}

	@Override
	public double[] computeBaseline(double[] chromatogram, ParameterSet parameters) {

		// Smoothing and asymmetry parameters.
		double smoothing = parameters.getParameter(AsymmetryCorrectorParameters.SMOOTHING).getValue();
		double asymmetry = parameters.getParameter(AsymmetryCorrectorParameters.ASYMMETRY).getValue();

		// Get R engine.
		final Rengine rEngine;
		try {
			rEngine = RUtilities.getREngine();
		}
		catch (Throwable t) {
			throw new IllegalStateException(
					"Baseline correction requires R but it couldn't be loaded (" + t.getMessage() + ')');
		}

		final double[] baseline;
		synchronized (RUtilities.R_SEMAPHORE) {

			try {
				// Set chromatogram.
				rEngine.assign("chromatogram", chromatogram);
				// Calculate baseline.
				baseline = rEngine.eval("asysm(chromatogram," + smoothing + ',' + asymmetry + ')').asDoubleArray();
			}
			catch (Throwable t) {
				throw new IllegalStateException("R error during baseline correction: ", t);
			}
		}
		return baseline;
	}


	@Override
	public @Nonnull String getName() {
		return "Asymmetric baseline corrector";
	}

	@Override
	public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
		return AsymmetryCorrectorParameters.class;
	}

}

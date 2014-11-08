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
 * @description Rolling Ball baseline corrector. Estimates a trend based on Rolling Ball algorithm.
 * Uses "rollingBall" feature from "baseline" R-package (http://cran.r-project.org/web/packages/baseline/baseline.pdf).
 * (Ideas from Rolling Ball algorithm for X-ray spectra by M.A.Kneen and H.J. Annegarn. Variable window width has been left out).
 * 
 * @author Gauthier Boaglio
 * @date Nov 6, 2014
 */
public class RollingBallCorrector extends BaselineCorrector {
	
	@Override
	public String[] getRequiredRPackages() {
		return new String[] { "rJava", "baseline" };
	}

	@Override
	public double[] computeBaseline(double[] chromatogram, ParameterSet parameters) {

		// Rolling Ball parameters.
		double wm = parameters.getParameter(RollingBallCorrectorParameters.MIN_MAX_WIDTH).getValue();
		double ws = parameters.getParameter(RollingBallCorrectorParameters.SMOOTHING).getValue();

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
				// Transform chromatogram.
				rEngine.eval("mat = matrix(chromatogram, nrow=1)");
				// Calculate baseline.
				baseline = rEngine.eval("getBaseline(baseline(mat, wm=" + wm + ", ws=" + ws + ", method='rollingBall'))").asDoubleArray();
			}
			catch (Throwable t) {
				throw new IllegalStateException("R error during baseline correction: ", t);
			}
		}
		return baseline;
	}


	@Override
	public @Nonnull String getName() {
		return "RollingBall baseline corrector";
	}

	@Override
	public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
		return RollingBallCorrectorParameters.class;
	}

}

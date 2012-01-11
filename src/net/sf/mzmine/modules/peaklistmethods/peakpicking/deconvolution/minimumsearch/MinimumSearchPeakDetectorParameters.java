/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.minimumsearch;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.PeakResolver;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.PeakResolverSetupDialog;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.RangeParameter;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.dialogs.ExitCode;

public class MinimumSearchPeakDetectorParameters extends SimpleParameterSet {

    public static final PercentParameter CHROMATOGRAPHIC_THRESHOLD_LEVEL = new PercentParameter(
            "Chromatographic threshold",
            "Threshold for removing noise. The algorithm finds such intensity that given percentage of the chromatogram data points is below that intensity, and removes all data points below that level.");

    public static final DoubleParameter SEARCH_RT_RANGE = new DoubleParameter(
            "Search minimum in RT range",
            "If a local minimum is minimal in this range of retention time, it will be considered a border between two peaks",
            MZmineCore.getRTFormat(),
            null,
            0.001,
            null);

    public static final PercentParameter MIN_RELATIVE_HEIGHT = new PercentParameter(
            "Minimum relative height",
            "Minimum height of a peak relative to the chromatogram top data point");

    public static final DoubleParameter MIN_ABSOLUTE_HEIGHT = new DoubleParameter(
            "Minimum absolute height",
            "Minimum absolute height of a peak to be recognized",
            MZmineCore.getIntensityFormat());

    public static final DoubleParameter MIN_RATIO = new DoubleParameter(
            "Min ratio of peak top/edge",
            "Minimum ratio between peak's top intensity and side (lowest) data points. This parameter helps to reduce detection of false peaks in case the chromatogram is not smooth.");

    public static final RangeParameter PEAK_DURATION = new RangeParameter(
            "Peak duration range",
            "Range of acceptable peak lengths",
            MZmineCore.getRTFormat(),
            new Range(0.0, 600.0));

    private final PeakResolver peakResolver;

    public MinimumSearchPeakDetectorParameters(final PeakResolver resolver) {

        super(new Parameter[]{
                CHROMATOGRAPHIC_THRESHOLD_LEVEL, SEARCH_RT_RANGE, MIN_RELATIVE_HEIGHT, MIN_ABSOLUTE_HEIGHT, MIN_RATIO,
                PEAK_DURATION});
        peakResolver = resolver;
    }

    @Override
    public ExitCode showSetupDialog() {

        final PeakResolverSetupDialog dialog = new PeakResolverSetupDialog(peakResolver);
        dialog.setVisible(true);
        return dialog.getExitCode();
    }
}

/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.filtering.mergefilter;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.*;

public class MergeFilterParameters extends SimpleParameterSet {

    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

    public static final StringParameter SUFFIX = new StringParameter(
            "Name suffix",
            "Suffix to be added to peak list name", "merged");

    public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter(
            "m/z tolerance",
            "Maximum m/z difference between peaks to be merged");

    public static final RTToleranceParameter RT_TOLERANCE = new RTToleranceParameter(
            "RT tolerance",
            "Maximum retention time difference between peaks to be merged");

    public static final BooleanParameter REQUIRE_SAME_ID = new BooleanParameter(
            "Require same identification",
            "If checked, merged peaks must have the same identification(s)");

    public static final BooleanParameter AUTO_REMOVE = new BooleanParameter(
            "Remove original peaklist",
            "If checked, original peak list will be removed and only the merged version remains");

    public MergeFilterParameters() {
        super(new Parameter[]{PEAK_LISTS, SUFFIX, MZ_TOLERANCE, RT_TOLERANCE, REQUIRE_SAME_ID, AUTO_REMOVE,});
    }
}

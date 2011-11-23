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

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;

/**
 * Merge peaks module.
 */
public class MergeFilterModule implements MZmineProcessingModule {

    // Module title.
    private static final String TITLE = "Merge peak filter";

    // Parameters.
    private final ParameterSet parameters = new MergeFilterParameters();

    public String toString() {
        return TITLE;
    }

    @Override
    public ParameterSet getParameterSet() {
        return parameters;
    }

    @Override
    public Task[] runModule(final ParameterSet params) {

        // Get peak lists to process.
        final PeakList[] peakLists = params.getParameter(MergeFilterParameters.PEAK_LISTS).getValue();

        // Create a new task for each peak list.
        final Task[] tasks = new MergeFilterTask[peakLists.length];
        int i = 0;
        for (final PeakList list : peakLists) {
            tasks[i++] = new MergeFilterTask(list, params);
        }

        // Queue and return the tasks.
        MZmineCore.getTaskController().addTasks(tasks);
        return tasks;
    }

    @Override
    public MZmineModuleCategory getModuleCategory() {

        return MZmineModuleCategory.PEAKLISTFILTERING;
    }
}

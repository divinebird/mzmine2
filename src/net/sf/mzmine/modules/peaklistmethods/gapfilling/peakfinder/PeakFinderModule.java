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

package net.sf.mzmine.modules.peaklistmethods.gapfilling.peakfinder;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;

public class PeakFinderModule implements MZmineProcessingModule {

	public static final String MODULE_NAME = "Peak finder";

	private PeakFinderParameters parameters = new PeakFinderParameters();

	public String toString() {
		return MODULE_NAME;
	}

	/**
	 * @see net.sf.mzmine.modules.MZmineModule#getParameterSet()
	 */
	public ParameterSet getParameterSet() {
		return parameters;
	}

	/**
	 * @see 
	 *      net.sf.mzmine.modules.BatchStep#runModule(net.sf.mzmine.data.RawDataFile
	 *      [], net.sf.mzmine.data.PeakList[], net.sf.mzmine.data.ParameterSet,
	 *      net.sf.mzmine.taskcontrol.Task[]Listener)
	 */
	public Task[] runModule(ParameterSet parameters) {

		PeakList[] peakLists = parameters.getParameter(
				PeakFinderParameters.peakLists).getValue();

		// prepare a new group of tasks
		Task tasks[] = new PeakFinderTask[peakLists.length];
		for (int i = 0; i < peakLists.length; i++) {
			tasks[i] = new PeakFinderTask(peakLists[i], parameters);
		}

		MZmineCore.getTaskController().addTasks(tasks);

		return tasks;

	}

	public MZmineModuleCategory getModuleCategory() {
		return MZmineModuleCategory.GAPFILLING;
	}

}
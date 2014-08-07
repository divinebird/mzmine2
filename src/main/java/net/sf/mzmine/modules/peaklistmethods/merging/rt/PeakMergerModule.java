/*
 * @Author Gauthier Boaglio
 */

package net.sf.mzmine.modules.peaklistmethods.merging.rt;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

/**
 * This class implements a simple peaks merger method based on
 * searching for neighbouring peaks from expected locations (RT based).
 * The m/z dimension is updated for the resulting peaks, but not used while 
 * detecting groups. 
 * 
 */
public class PeakMergerModule implements MZmineProcessingModule {

	private static final String MODULE_NAME = "RT based peaks merger";
	private static final String MODULE_DESCRIPTION = "This module groups DETECTED peaks (prior to RT) in given RT and MZ ranges and merges them together into a single one.";

	@Override
	public @Nonnull String getName() {
		return MODULE_NAME;
	}

	@Override
	public @Nonnull String getDescription() {
		return MODULE_DESCRIPTION;
	}

	@Override
	@Nonnull
	public ExitCode runModule(@Nonnull ParameterSet parameters,
			@Nonnull Collection<Task> tasks) {

		PeakList peakLists[] = parameters.getParameter(
				PeakMergerParameters.peakLists).getValue();

		for (final PeakList peakList : peakLists) {
			Task newTask = new PeakMergerTask(peakList, parameters);
			tasks.add(newTask);
		}

		return ExitCode.OK;

	}

	@Override
	public @Nonnull MZmineModuleCategory getModuleCategory() {
		return MZmineModuleCategory.PEAKLISTMERGING;
	}

	@Override
	public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
		return PeakMergerParameters.class;
	}

}

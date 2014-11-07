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

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.RUtilities;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Task that performs baseline correction.
 *
 * @author $Author$
 * @version $Revision$
 * 
 * Deeply modified to delegate baseline correction to various correctors (whose implement specific
 * methods by them-selves). 
 * Those correctors all share a common behavior by inheriting from the base class "BaselineCorrector", 
 * and apply there specific way of building the baselines via the various algorithms implemented in
 * the sub-package "net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection.correctors".
 */
public class BaselineCorrectionTask extends AbstractTask {

	// Logger.
	private static final Logger LOG = Logger.getLogger(BaselineCorrectionTask.class.getName());

	// Original data file and newly created baseline corrected file.
	private final RawDataFile origDataFile;
	private RawDataFile correctedDataFile;

	// Remove original data file.
	private final boolean removeOriginal;

	// Baseline corrector processing step
	private MZmineProcessingStep<BaselineCorrector> baselineCorrectorProcStep;


	/**
	 * Creates the task.
	 *
	 * @param dataFile   raw data file on which to perform correction.
	 * @param parameters correction parameters.
	 */
	public BaselineCorrectionTask(final RawDataFile dataFile,
			final ParameterSet parameters) {

		// Check R availability.
		try {
			RUtilities.getREngine();
		}
		catch (Throwable t) {
			throw new IllegalStateException(
					"Baseline correction requires R but it couldn't be loaded (" + t.getMessage() + ')');
		}

		// Initialize.
		origDataFile = dataFile;
		correctedDataFile = null;
		removeOriginal = parameters.getParameter(BaselineCorrectionParameters.REMOVE_ORIGINAL).getValue();
		baselineCorrectorProcStep = parameters.getParameter(BaselineCorrectionParameters.BASELINE_CORRECTORS).getValue();

	}


	@Override
	public String getTaskDescription() {
		return "Correcting baseline for " + origDataFile;
	}

	@Override
	public double getFinishedPercentage() {
		int progressMax = baselineCorrectorProcStep.getModule().getProgressMax();
		int progress = baselineCorrectorProcStep.getModule().getProgress();
		return progressMax == 0 ? 0.0 : (double) progress / (double) progressMax;
	}

	@Override
	public Object[] getCreatedObjects() {
		return new Object[]{correctedDataFile};
	}

	@Override
	public void run() {

		// Update the status of this task
		setStatus(TaskStatus.PROCESSING);

		try {

			final RawDataFile correctedDataFile = 
					this.baselineCorrectorProcStep.getModule().correctDatafile(origDataFile, baselineCorrectorProcStep.getParameterSet());

			// If this task was canceled, stop processing
			if (!isCanceled() && correctedDataFile != null) {

				// Add the newly created file to the project
				final MZmineProject project = MZmineCore.getCurrentProject();
				project.addFile(correctedDataFile);

				// Remove the original data file if requested
				if (removeOriginal) {
					project.removeFile(origDataFile);
				}

				// Set task status to FINISHED
				setStatus(TaskStatus.FINISHED);

				LOG.info("Baseline corrected " + origDataFile.getName());
			}
		}
		catch (Throwable t) {

			LOG.log(Level.SEVERE, "Baseline correction error", t);
			setStatus(TaskStatus.ERROR);
			errorMessage = t.getMessage();
		}
	}

	@Override
	public void cancel() {
		// Ask running module to stop
		baselineCorrectorProcStep.getModule().setAbortProcessing(true);
		super.cancel();
	}

}

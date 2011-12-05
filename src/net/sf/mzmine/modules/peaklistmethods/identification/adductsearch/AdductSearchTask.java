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
package net.sf.mzmine.modules.peaklistmethods.identification.adductsearch;

import java.util.Arrays;
import java.util.logging.Logger;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class AdductSearchTask extends AbstractTask {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private int finishedRows, totalRows;
	private PeakList peakList;

	private double rtTolerance, mzTolerance, maxAdductHeight,
			customMassDifference;
	private AdductType[] selectedAdducts;

	private ParameterSet parameters;

	/**
	 * @param parameters
	 * @param peakList
	 */
	public AdductSearchTask(ParameterSet parameters, PeakList peakList) {

		this.peakList = peakList;
		this.parameters = parameters;

		rtTolerance = parameters.getParameter(
				AdductSearchParameters.rtTolerance).getValue();
		mzTolerance = parameters.getParameter(
				AdductSearchParameters.mzTolerance).getValue();

		selectedAdducts = parameters.getParameter(
				AdductSearchParameters.adducts).getValue();

		customMassDifference = parameters.getParameter(
				AdductSearchParameters.customAdductValue).getValue();

		maxAdductHeight = parameters.getParameter(
				AdductSearchParameters.maxAdductHeight).getValue();

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		if (totalRows == 0)
			return 0;
		return ((double) finishedRows) / totalRows;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Identification of adducts in " + peakList;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		setStatus(TaskStatus.PROCESSING);

		logger.info("Starting adducts search in " + peakList);

		PeakListRow rows[] = peakList.getRows();
		totalRows = rows.length;

		// Start with the highest peaks
		Arrays.sort(rows, new PeakListRowSorter(SortingProperty.Height,
				SortingDirection.Descending));

		// Compare each two rows against each other
		for (int i = 0; i < totalRows; i++) {

			for (int j = i + 1; j < rows.length; j++) {

				// Task canceled?
				if (isCanceled())
					return;

				// Treat the smaller m/z peak as main peak and check if the
				// bigger one may be an adduct
				if (rows[i].getAverageMZ() > rows[j].getAverageMZ()) {
					checkAllAdducts(rows[j], rows[i]);
				} else {
					checkAllAdducts(rows[i], rows[j]);
				}

			}

			finishedRows++;

		}

		// Add task description to peakList
		((SimplePeakList) peakList)
				.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
						"Identification of adducts", parameters));

		// Repaint the window to reflect the change in the peak list
		MZmineCore.getDesktop().getMainFrame().repaint();
		
		setStatus(TaskStatus.FINISHED);

		logger.info("Finished adducts search in " + peakList);

	}

	/**
	 * Check if candidate peak may be a possible adduct of a given main peak
	 * 
	 * @param mainPeak
	 * @param possibleFragment
	 */
	private void checkAllAdducts(PeakListRow mainRow, PeakListRow possibleAdduct) {

		for (AdductType adduct : selectedAdducts) {
			if (checkAdduct(mainRow, possibleAdduct, adduct)) {
				addAdductInfo(mainRow, possibleAdduct, adduct);
			}
		}
	}

	/**
	 * Check if candidate peak is a given type of adduct of given main peak
	 * 
	 * @param mainPeak
	 * @param possibleFragment
	 * @param adduct
	 * @return
	 */
	private boolean checkAdduct(PeakListRow mainPeak,
			PeakListRow possibleAdduct, AdductType adduct) {

		// Calculate expected mass difference of this adduct
		double expectedMzDifference;
		if (adduct == AdductType.CUSTOM)
			expectedMzDifference = customMassDifference;
		else
			expectedMzDifference = adduct.getMassDifference();

		// Check mass difference condition
		double mzDifference = Math.abs(mainPeak.getAverageMZ()
				+ expectedMzDifference - possibleAdduct.getAverageMZ());
		if (mzDifference > mzTolerance)
			return false;

		// Check retention time condition
		double rtDifference = Math.abs(mainPeak.getAverageRT()
				- possibleAdduct.getAverageRT());
		if (rtDifference > rtTolerance)
			return false;

		// Check height condition
		if (possibleAdduct.getAverageHeight() > mainPeak.getAverageHeight()
				* maxAdductHeight)
			return false;

		return true;

	}

	/**
	 * Add new identity to the adduct row
	 * 
	 * @param mainRow
	 * @param fragmentRow
	 */
	private void addAdductInfo(PeakListRow mainRow, PeakListRow adductRow,
			AdductType adduct) {
		AdductIdentity newIdentity = new AdductIdentity(mainRow, adductRow,
				adduct);
		adductRow.addPeakIdentity(newIdentity, false);
		
		// Notify the GUI about the change in the project
		MZmineCore.getCurrentProject().notifyObjectChanged(adductRow, false);
	}

}

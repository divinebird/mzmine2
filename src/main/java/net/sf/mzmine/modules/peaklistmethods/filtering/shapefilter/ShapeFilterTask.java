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

package net.sf.mzmine.modules.peaklistmethods.filtering.shapefilter;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

/**
 * @description A task to filter out false peaks (shape criterion) list rows.
 * @author Gauthier Boaglio
 * @date Nov 30, 2014
 */
public class ShapeFilterTask extends AbstractTask {

	// Logger.
	private static final Logger LOG = Logger.getLogger(ShapeFilterTask.class.getName());

	// Original and resultant peak lists.
	private final PeakList peakList;
	private PeakList filteredPeakList;

	// Counters.
	private int processedRows;
	private int totalRows;

	// Parameters.
	private final ParameterSet parameters;

	public ShapeFilterTask(final PeakList list, final ParameterSet params) {

		// Initialize.
		parameters = params;
		peakList = list;
		filteredPeakList = null;
		totalRows = 0;
		processedRows = 0;
	}

	@Override
	public String getTaskDescription() {

		return "Filtering duplicate peak list rows of " + peakList;
	}

	@Override
	public double getFinishedPercentage() {

		return totalRows == 0 ? 0.0 : (double) processedRows / (double) totalRows;
	}

	@Override
	public Object[] getCreatedObjects() {

		return new Object[]{filteredPeakList};
	}

	@Override
	public void run() {

		if (!isCanceled()) {
			try {

				LOG.info("Filtering duplicate peaks list rows of " + peakList);
				setStatus(TaskStatus.PROCESSING);

				// Filter out false peaks.
				filteredPeakList = filterBadShapedPeakListRows(
						peakList,
						parameters.getParameter(ShapeFilterParameters.SUFFIX).getValue(),
						parameters.getParameter(ShapeFilterParameters.AVG_EDGES).getValue(),
						parameters.getParameter(ShapeFilterParameters.MIN_RATIO).getValue(),
						parameters.getParameter(ShapeFilterParameters.MASS_RESOLUTION).getValue(),
						(FilterShapeModel)parameters.getParameter(ShapeFilterParameters.SHAPEMODEL_TYPE).getValue());

				if (!isCanceled()) {

					// Add new peakList to the project.
					final MZmineProject project = MZmineCore.getCurrentProject();
					project.addPeakList(filteredPeakList);

					// Remove the original peakList if requested.
					if (parameters.getParameter(ShapeFilterParameters.AUTOREMOVE).getValue()) {

						project.removePeakList(peakList);
					}

					// Finished.
					LOG.info("Finished filtering duplicate peak list rows on " + peakList);
					setStatus(TaskStatus.FINISHED);
				}
			}
			catch (Throwable t) {

				LOG.log(Level.SEVERE, "Shape based filter error", t);
				errorMessage = t.getMessage();
				setStatus(TaskStatus.ERROR);
			}
		}
	}

	/**
	 * Filter our duplicate peak list rows.
	 *
	 * @param origPeakList  the original peak list.
	 * @param suffix        the suffix to apply to the new peak list name.
	 * @param mzTolerance   m/z tolerance.
	 * @param rtTolerance   RT tolerance.
	 * @param requireSameId must duplicate peaks have the same identities?
	 * @return the filtered peak list.
	 */
	private PeakList filterBadShapedPeakListRows(final PeakList origPeakList,
			final String suffix,
			final boolean avgEdges,
			final Double minRatio,
			final Double massResolution,
			final FilterShapeModel shapeModel) {

		final PeakListRow[] peakListRows = origPeakList.getRows();
		final int rowCount = peakListRows.length;

		Arrays.sort(peakListRows, new PeakListRowSorter(SortingProperty.Area, SortingDirection.Descending));

		// Loop through all peak list rows
		processedRows = 0;
		totalRows = rowCount;
		for (int rowIndex = 0; !isCanceled() && rowIndex < rowCount; rowIndex++) {

			final PeakListRow firstRow = peakListRows[rowIndex];
			if (firstRow != null) {

				//                for (int secondRowIndex = firstRowIndex + 1;
				//                     !isCanceled() && secondRowIndex < rowCount;
				//                     secondRowIndex++) {
				//
				////                    final PeakListRow secondRow = peakListRows[secondRowIndex];
				////                    if (secondRow != null) {
				////
				////                        // Compare identifications
				////                        final boolean sameID = !requireSameId || PeakUtils.compareIdentities(firstRow, secondRow);
				////
				////                        // Compare m/z
				////                        final boolean sameMZ = mzTolerance.getToleranceRange(firstRow.getAverageMZ())
				////                                .contains(secondRow.getAverageMZ());
				////
				////                        // Compare rt
				////                        final boolean sameRT = rtTolerance.getToleranceRange(firstRow.getAverageRT())
				////                                .contains(secondRow.getAverageRT());
				////
				////                        // Duplicate peaks?
				////                        if (sameID && sameMZ && sameRT) {
				////
				////                            peakListRows[secondRowIndex] = null;
				////                        }
				////                    }
				//                }

				// Min top/edge ratio requirement
				// Find the intensity at the sides (lowest data points).
				//				final double peakMinLeft = intensities[currentRegionStart];
				//				final double peakMinRight = intensities[currentRegionEnd];
				double peakMinLeft = Double.MAX_VALUE;
				double peakMinRight = Double.MAX_VALUE;
				Feature peak = firstRow.getBestPeak();
				int[] scanNumbers = peak.getScanNumbers();
				int topScanIndex = peak.getRepresentativeScanNumber();
				double topScanIntensity = peak.getDataPoint(topScanIndex).getIntensity();

				//				// Debug
				//				LOG.info("topScanIndex = " + peak.getRepresentativeScanNumber());
				//				LOG.info("peak.getRepresentativeScanNumber() = " + peak.getRepresentativeScanNumber());
				//				LOG.info("nb datapoint top = " + peak.getDataPoint(topScanIndex));
				//				for (int i=scanNumbers[0]; i < scanNumbers[scanNumbers.length-1]; ++i) {
				//					LOG.info("peak.getScanNumbers()[" + i + "] = " + i);
				//					LOG.info("peak.getDataPoint(" + i + ") = " + peak.getDataPoint(i));
				//				}


				// If top scan is an edge, then peak cannot be a well shaped: remove it...
				// (includes cases where peak is composed of less than 3 scans).
				if (scanNumbers.length > 0 && topScanIndex == scanNumbers[0] || topScanIndex == scanNumbers[scanNumbers.length-1]) {
					peakListRows[rowIndex] = null;
					continue;
				}

				// Find min intensity scan index on the left
				for (int i=topScanIndex; i >= scanNumbers[0]; --i) {
					double intensity = peak.getDataPoint(i).getIntensity(); 
					if (intensity < peakMinLeft) { peakMinLeft = intensity; }
				}
				// Find min intensity scan index on the right
				for (int i=topScanIndex; i < scanNumbers[scanNumbers.length-1]; ++i) {
					double intensity = peak.getDataPoint(i).getIntensity(); 
					if (intensity < peakMinRight) { peakMinRight = intensity; }
				}


				// If the peak shape does not fulfill the ratio condition, remove it.
				//**if (topScanIntensity < peakMinRight * minRatio) {

					// Check the shape of the peak.
					//					if (topScanIntensity < peakMinLeft * minRatio
					//							|| topScanIntensity < peakMinRight * minRatio) {
					double bottom = (avgEdges) ? (peakMinLeft + peakMinRight) / 2.0 * minRatio : Math.min(peakMinLeft, peakMinRight);
					if (topScanIntensity < bottom) {

						peakListRows[rowIndex] = null;
						continue;
					}
				//**}

			}

			processedRows++;
		}

		// Create the new peak list.
		final PeakList newPeakList = new SimplePeakList(origPeakList + " " + suffix, origPeakList.getRawDataFiles());

		// Add all remaining rows to a new peak list.
		for (int i = 0; !isCanceled() && i < rowCount; i++) {

			final PeakListRow row = peakListRows[i];

			if (row != null) {

				// Copy the peak list row.
				final PeakListRow newRow = new SimplePeakListRow(row.getID());
				PeakUtils.copyPeakListRowProperties(row, newRow);

				// Copy the peaks.
				for (final Feature peak : row.getPeaks()) {

					final Feature newPeak = new SimpleFeature(peak);
					PeakUtils.copyPeakProperties(peak, newPeak);
					newRow.addPeak(peak.getDataFile(), newPeak);
				}

				newPeakList.addRow(newRow);
			}
		}

		if (!isCanceled()) {

			// Load previous applied methods.
			for (final PeakListAppliedMethod method : origPeakList.getAppliedMethods()) {

				newPeakList.addDescriptionOfAppliedTask(method);
			}

			// Add task description to peakList
			newPeakList.addDescriptionOfAppliedTask(
					new SimplePeakListAppliedMethod("Shape based peak list rows filter", parameters));
		}

		return newPeakList;
	}

	static public PeakList getBadShapedPeakListRows(final PeakList origPeakList,
			final String suffix,
			final Double minRatio,
			final Double massResolution,
			final FilterShapeModel shapeModel) {

		final PeakListRow[] peakListRows = origPeakList.getRows();
		final int rowCount = peakListRows.length;

		Arrays.sort(peakListRows, new PeakListRowSorter(SortingProperty.Area, SortingDirection.Descending));

		// Create the new peak list.
		final PeakList newPeakList = new SimplePeakList(origPeakList + " bad-shaped", origPeakList.getRawDataFiles());

		// Loop through all peak list rows
		for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {

			final PeakListRow firstRow = peakListRows[rowIndex];
			if (firstRow != null) {

				// Min top/edge ratio requirement
				// Find the intensity at the sides (lowest data points).
				//final double peakMinLeft = intensities[currentRegionStart];
				//final double peakMinRight = intensities[currentRegionEnd];
				double peakMinLeft = Double.MAX_VALUE;
				double peakMinRight = Double.MAX_VALUE;
				Feature peak = firstRow.getBestPeak();
				int[] scanNumbers = peak.getScanNumbers();
				int topScanIndex = peak.getRepresentativeScanNumber();
				double topScanIntensity = peak.getDataPoint(topScanIndex).getIntensity();

				//// Debug
				//LOG.info("topScanIndex = " + peak.getRepresentativeScanNumber());
				//LOG.info("peak.getRepresentativeScanNumber() = " + peak.getRepresentativeScanNumber());
				//LOG.info("nb datapoint top = " + peak.getDataPoint(topScanIndex));
				//for (int i=scanNumbers[0]; i < scanNumbers[scanNumbers.length-1]; ++i) {
				//LOG.info("peak.getScanNumbers()[" + i + "] = " + i);
				//LOG.info("peak.getDataPoint(" + i + ") = " + peak.getDataPoint(i));
				//}


				// If top scan is an edge, then peak cannot be a well shaped: remove it...
				// (includes cases where peak is composed of less than 3 scans).
				if (scanNumbers.length > 0 && topScanIndex == scanNumbers[0] || topScanIndex == scanNumbers[scanNumbers.length-1]) {
					//peakListRows[rowIndex] = null;
					final PeakListRow row = peakListRows[rowIndex];

					if (row != null) {

//						// Copy the peak list row.
//						final PeakListRow newRow = new SimplePeakListRow(row.getID());
//						PeakUtils.copyPeakListRowProperties(row, newRow);
//
//						// Copy the peaks.
//						for (final Feature a_peak : row.getPeaks()) {
//
//							final Feature newPeak = new SimpleFeature(a_peak);
//							PeakUtils.copyPeakProperties(a_peak, newPeak);
//							newRow.addPeak(peak.getDataFile(), newPeak);
//						}
//
//						newPeakList.addRow(newRow);
						newPeakList.addRow(row);
					}
					continue;
				}

				// Find min intensity scan index on the left
				for (int i=topScanIndex; i >= scanNumbers[0]; --i) {
					double intensity = peak.getDataPoint(i).getIntensity(); 
					if (intensity < peakMinLeft) { peakMinLeft = intensity; }
				}
				// Find min intensity scan index on the right
				for (int i=topScanIndex; i < scanNumbers[scanNumbers.length-1]; ++i) {
					double intensity = peak.getDataPoint(i).getIntensity(); 
					if (intensity < peakMinRight) { peakMinRight = intensity; }
				}


				// If the peak shape does not fulfill the ratio condition, remove it.
				//**if (topScanIntensity <= peakMinRight * minRatio) {
				//*****if (topScanIntensity >= 0) {

					// Check the shape of the peak.
					//if (topScanIntensity < peakMinLeft * minRatio
					//|| topScanIntensity < peakMinRight * minRatio) {
					if (topScanIntensity < (peakMinLeft + peakMinRight) / 2.0 * minRatio) {
					//*****if (topScanIntensity < 500.0) {

						//peakListRows[rowIndex] = null;
						final PeakListRow row = peakListRows[rowIndex];
						
						if (row != null) {

//							// Copy the peak list row.
//							final PeakListRow newRow = new SimplePeakListRow(row.getID());
//							PeakUtils.copyPeakListRowProperties(row, newRow);
//
//							// Copy the peaks.
//							for (final Feature a_peak : row.getPeaks()) {
//
//								final Feature newPeak = new SimpleFeature(a_peak);
//								PeakUtils.copyPeakProperties(a_peak, newPeak);
//								newRow.addPeak(peak.getDataFile(), newPeak);
//							}
//
//							newPeakList.addRow(newRow);
							newPeakList.addRow(row);
						}
						continue;
					}
				//**}

			}

		}

		return newPeakList;
	}

}

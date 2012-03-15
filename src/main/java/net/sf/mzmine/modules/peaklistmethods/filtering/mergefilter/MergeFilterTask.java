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

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListAppliedMethod;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimpleChromatographicPeak;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.RTTolerance;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.sf.mzmine.modules.peaklistmethods.filtering.mergefilter.MergeFilterParameters.*;

/**
 * A task to perform peak list row merging.
 */
public class MergeFilterTask extends AbstractTask {

    // Logger.
    private static final Logger LOG = Logger.getLogger(MergeFilterTask.class.getName());

    // Original and resultant peak lists.
    private final PeakList peakList;
    private PeakList filteredPeakList;

    // Counters.
    private int processedRows;
    private int totalRows;

    // Parameters.
    private final ParameterSet parameters;

    public MergeFilterTask(final PeakList list, final ParameterSet params) {

        // Initialize.
        parameters = params;
        peakList = list;
        filteredPeakList = null;
        totalRows = 0;
        processedRows = 0;
    }

    @Override
    public String getTaskDescription() {

        return "Merging peak list rows of " + peakList;
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

                LOG.info("Merging peaks list rows of " + peakList);
                setStatus(TaskStatus.PROCESSING);

                // Merge peak list row.
                filteredPeakList = mergePeakListRows(
                        peakList,
                        parameters.getParameter(SUFFIX).getValue(),
                        parameters.getParameter(MZ_TOLERANCE).getValue(),
                        parameters.getParameter(RT_TOLERANCE).getValue(),
                        parameters.getParameter(REQUIRE_SAME_ID).getValue());

                if (!isCanceled()) {

                    // Add new peakList to the project.
                    final MZmineProject project = MZmineCore.getCurrentProject();
                    project.addPeakList(filteredPeakList);

                    // Remove the original peakList if requested.
                    if (parameters.getParameter(AUTO_REMOVE).getValue()) {

                        project.removePeakList(peakList);
                    }

                    // Finished.
                    LOG.info("Finished merging peak list rows on " + peakList);
                    setStatus(TaskStatus.FINISHED);
                }
            }
            catch (Throwable t) {

                LOG.log(Level.SEVERE, "Merge peak list rows error", t);
                errorMessage = t.getMessage();
                setStatus(TaskStatus.ERROR);
            }
        }
    }

    /**
     * Merge peak list rows.
     *
     * @param origPeakList  the original peak list.
     * @param suffix        the suffix to apply to the new peak list name.
     * @param mzTolerance   m/z tolerance.
     * @param rtTolerance   RT tolerance.
     * @param requireSameId must merged peaks have the same identities?
     * @return the filtered peak list.
     */
    private PeakList mergePeakListRows(final PeakList origPeakList,
                                       final String suffix,
                                       final MZTolerance mzTolerance,
                                       final RTTolerance rtTolerance,
                                       final boolean requireSameId) {

        final PeakListRow[] peakListRows = origPeakList.getRows();
        final int rowCount = peakListRows.length;

        Arrays.sort(peakListRows, new PeakListRowSorter(SortingProperty.Area, SortingDirection.Descending));

        // Loop through all peak list rows
        processedRows = 0;
        totalRows = rowCount;
        for (int firstRowIndex = 0;
             !isCanceled() && firstRowIndex < rowCount;
             firstRowIndex++) {

            final PeakListRow firstRow = peakListRows[firstRowIndex];
            if (firstRow != null) {

                for (int secondRowIndex = firstRowIndex + 1;
                     !isCanceled() && secondRowIndex < rowCount;
                     secondRowIndex++) {

                    final PeakListRow secondRow = peakListRows[secondRowIndex];
                    if (secondRow != null) {

                        // Compare identifications
                        final boolean sameID = !requireSameId || PeakUtils.compareIdentities(firstRow, secondRow);

                        // Compare m/z
                        final boolean sameMZ = mzTolerance.getToleranceRange(firstRow.getAverageMZ())
                                .contains(secondRow.getAverageMZ());

                        // Compare rt
                        final boolean sameRT = rtTolerance.getToleranceRange(firstRow.getAverageRT())
                                .contains(secondRow.getAverageRT());

                        // Merge?
                        if (sameID && sameMZ && sameRT) {

                            peakListRows[secondRowIndex] = null;
                        }
                    }
                }
            }

            processedRows++;
        }

        // Create the new peak list.
        final PeakList newPeakList = new SimplePeakList(origPeakList + " " + suffix, origPeakList.getRawDataFiles());

        if (!isCanceled()) {

            // Add all remaining rows to a new peak list.
            int peakID = 1;
            for (final PeakListRow peakListRow : peakListRows) {

                if (peakListRow != null) {

                    // Copy the peak list row.
                    final PeakListRow newRow = new SimplePeakListRow(peakID++);
                    PeakUtils.copyPeakListRowProperties(peakListRow, newRow);

                    // Copy the peaks.
                    for (final ChromatographicPeak peak : peakListRow.getPeaks()) {

                        final ChromatographicPeak newPeak = new SimpleChromatographicPeak(peak);
                        PeakUtils.copyPeakProperties(peak, newPeak);
                        newRow.addPeak(peak.getDataFile(), newPeak);
                    }

                    newPeakList.addRow(newRow);
                }
            }

            // Load previous applied methods.
            for (final PeakListAppliedMethod method : origPeakList.getAppliedMethods()) {

                newPeakList.addDescriptionOfAppliedTask(method);
            }

            // Add task description to peakList
            newPeakList.addDescriptionOfAppliedTask(
                    new SimplePeakListAppliedMethod("Merge peak list rows filter", parameters));
        }

        return newPeakList;
    }
}

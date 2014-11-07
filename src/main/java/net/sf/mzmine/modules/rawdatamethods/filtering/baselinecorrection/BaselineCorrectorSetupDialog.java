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

package net.sf.mzmine.modules.rawdatamethods.filtering.baselinecorrection;

import java.io.IOException;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.modules.visualization.tic.PlotType;
import net.sf.mzmine.modules.visualization.tic.TICDataSet;
import net.sf.mzmine.modules.visualization.tic.TICPlot;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialogWithChromatogramPreview;
import net.sf.mzmine.util.Range;

/**
 * @description This class extends ParameterSetupDialogWithChromatogramPreview class. 
 * This is used to preview how the selected baseline correction method and his parameters 
 * works over the raw data file.
 * 
 * @author Gauthier Boaglio
 * @date Nov 6, 2014
 */
public class BaselineCorrectorSetupDialog extends ParameterSetupDialogWithChromatogramPreview {

	private ParameterSet correctorParameters;
	private BaselineCorrector baselineCorrector;

	/**
	 * 
	 * @param correctorParameters Method specific parameters
	 * @param correctorClass Chosen corrector to be instantiated 
	 */
	public BaselineCorrectorSetupDialog(ParameterSet correctorParameters,
			Class<? extends BaselineCorrector> correctorClass) {

		super(correctorParameters);

		this.correctorParameters = correctorParameters;

		try {
			this.baselineCorrector = correctorClass.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Default plot type. Initialized according to the chosen chromatogram type.
		this.setPlotType( (this.baselineCorrector.getChromatogramType() == ChromatogramType.TIC) ? 
				PlotType.TIC : PlotType.BASEPEAK );
	}

	/**
	 * This function sets all the information into the plot chart
	 */
	@Override
	protected void loadPreview(TICPlot ticPlot, RawDataFile dataFile, Range rtRange, Range mzRange) {

		// First, remove all current data sets
		ticPlot.removeAllTICDataSets();
		ticPlot.setPlotType(this.getPlotType());

		// Add the original raw data file
		int scanNumbers[] = dataFile.getScanNumbers(1, rtRange);
		TICDataSet ticDataset = new TICDataSet(dataFile, scanNumbers, mzRange, null, this.getPlotType());
		ticPlot.addTICDataset(ticDataset);

		try {
			// Create a new corrected raw data file
			RawDataFile newDataFile = baselineCorrector.correctDatafile(dataFile, this.correctorParameters);

			// If successful, add the new data file
			if (newDataFile != null) {
				int newScanNumbers[] = newDataFile.getScanNumbers(1, rtRange);
				TICDataSet newDataset = new TICDataSet(newDataFile, newScanNumbers, mzRange, null, this.getPlotType());
				ticPlot.addTICDataset(newDataset);

				// Show the trend line as well
				XYDataset tlDataset = createBaselineDataset(dataFile, newDataFile, this.getPlotType());
				ticPlot.addTICDataset(tlDataset);				
			}

		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
	}

	/**
	 * Quick way to recover the baseline plot (by subtracting the corrected file to the original one).
	 * @param dataFile    original datafile
	 * @param newDataFile corrected datafile
	 * @param plotType    expected plot type
	 * @return the baseline additional dataset
	 */
	private XYDataset createBaselineDataset(RawDataFile dataFile, RawDataFile newDataFile, PlotType plotType) {

		XYSeriesCollection dataset = new XYSeriesCollection();
		XYSeries bl_series = new XYSeries("Baseline");

		double intensity;
		Scan sc, new_sc;
		DataPoint dp, new_dp;
		
		// Get scan numbers from original file.
		final int[] scanNumbers = dataFile.getScanNumbers(1);
		final int numScans = scanNumbers.length;
		for (int scanIndex=0; scanIndex < numScans; ++scanIndex) {

			sc = dataFile.getScan(scanNumbers[scanIndex]);
			new_sc = newDataFile.getScan(scanNumbers[scanIndex]);

			if (plotType == PlotType.BASEPEAK) {
				dp = sc.getHighestDataPoint();
				new_dp = new_sc.getHighestDataPoint();
				if (dp == null) { intensity = 0.0; }
				else if (new_dp == null) { intensity = dp.getIntensity(); }
				else { intensity = dp.getIntensity() - new_dp.getIntensity(); }
			} else {
				intensity = sc.getTIC() - new_sc.getTIC();
			}

			bl_series.add(sc.getRetentionTime(), intensity);
		}

		dataset.addSeries(bl_series);

		return dataset;
	}

}

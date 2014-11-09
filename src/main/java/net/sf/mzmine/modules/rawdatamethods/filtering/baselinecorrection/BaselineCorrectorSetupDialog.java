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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JProgressBar;
import javax.swing.border.Border;

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
public class BaselineCorrectorSetupDialog extends ParameterSetupDialogWithChromatogramPreview  {

	// Logger.
	private static final Logger LOG = Logger.getLogger(BaselineCorrectionTask.class.getName());

	private ParameterSet correctorParameters;
	private BaselineCorrector baselineCorrector;

	private PreviewThread previewThread = null;
	private JProgressBar progressBar;
	private ProgressThread progressThread = null;

	// Listen to VK_ESCAPE KeyEvent and allow to abort preview computing 
	// if input parameters gave a computation task that is about to take forever...
	KeyListener keyListener = new KeyListener() {
		@Override
		public void keyPressed(KeyEvent ke) {
			int keyCode = ke.getKeyCode();
			if (keyCode == KeyEvent.VK_ESCAPE) {
				if (previewThread != null) { 
					previewThread.kill(); 
					previewThread = null; 
				}
				progressBar.setVisible(false);
				hidePreview();
			}
		}
		@Override
		public void keyReleased(KeyEvent ke) {  }
		@Override
		public void keyTyped(KeyEvent ke) {  }
	};

	public static List<Component> getAllComponents(final Container c) {
		Component[] comps = c.getComponents();
		List<Component> compList = new ArrayList<Component>();
		for (Component comp : comps) {
			compList.add(comp);
			if (comp instanceof Container) {
				compList.addAll(getAllComponents((Container) comp));
			}
		}
		return compList;
	}
	
	private void set_VK_ESCAPE_KeyListener() {
		// Set VK_ESCAPE KeyEvent listeners
		List<Component> comps = BaselineCorrectorSetupDialog.getAllComponents(BaselineCorrectorSetupDialog.this);
		for (Component c : comps) {
			c.addKeyListener(BaselineCorrectorSetupDialog.this.keyListener);
		}
	}
	private void unset_VK_ESCAPE_KeyListener() {
		// Remove VK_ESCAPE KeyEvent listeners
		List<Component> comps = BaselineCorrectorSetupDialog.getAllComponents(BaselineCorrectorSetupDialog.this);
		for (Component c : comps) {
			c.removeKeyListener(BaselineCorrectorSetupDialog.this.keyListener);
		}
	}

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


	class PreviewThread extends Thread {

		private TICPlot ticPlot;
		private RawDataFile dataFile;
		private	Range rtRange;
		private Range mzRange;
		private boolean crashed;

		public PreviewThread(TICPlot ticPlot, RawDataFile dataFile, Range rtRange, Range mzRange) {
			this.ticPlot = ticPlot;
			this.dataFile = dataFile;
			this.rtRange = rtRange;
			this.mzRange = mzRange;
			this.crashed = false;
		}

		@Override
		public void run() {

			// Set VK_ESCAPE KeyEvent listeners
			set_VK_ESCAPE_KeyListener();

			ticPlot.removeAllTICDataSets();
			ticPlot.setPlotType(getPlotType());

			// Add the original raw data file
			int scanNumbers[] = dataFile.getScanNumbers(1, rtRange);
			TICDataSet ticDataset = new TICDataSet(dataFile, scanNumbers, mzRange, null, getPlotType());
			ticPlot.addTICDataset(ticDataset);

			this.crashed = true;
			try {
				// Create a new corrected raw data file
				RawDataFile newDataFile = baselineCorrector.correctDatafile(dataFile, correctorParameters);

				// If successful, add the new data file
				if (newDataFile != null) {
					int newScanNumbers[] = newDataFile.getScanNumbers(1, rtRange);
					TICDataSet newDataset = new TICDataSet(newDataFile, newScanNumbers, mzRange, null, getPlotType());
					ticPlot.addTICDataset(newDataset);

					// Show the trend line as well
					XYDataset tlDataset = createBaselineDataset(dataFile, newDataFile, getPlotType());
					ticPlot.addTICDataset(tlDataset);

					this.crashed = false;
				}
			} catch (IOException e) {				// Writing error
				e.printStackTrace();
				this.crashed = true;
				//baselineCorrector.initProgress(dataFile);
			} catch (IllegalStateException e) {		// R computing error
				e.printStackTrace();
				this.crashed = true;
				//baselineCorrector.initProgress(dataFile);
			}

			// Handle post-processing
			if (!this.crashed) {
				// If processing went fine: Restore "parametersChanged" listeners
				unset_VK_ESCAPE_KeyListener();
				// And remove temporary file
				dataFile = null;
			} else {
				// If processing went wrong: Stop all & prepare for next attempt
				baselineCorrector.setAbortProcessing(dataFile, true);
				baselineCorrector.initProgress(dataFile);
			}

		}

		public boolean getCrashed() {
			return this.crashed;
		}

		public void kill() {
			RawDataFile dataFile = getPreviewDataFile();
			if (baselineCorrector != null && dataFile != null) { 
				// Abort current process
				baselineCorrector.setAbortProcessing(dataFile, true); 
				unset_VK_ESCAPE_KeyListener();
			}
		}

	}

	class ProgressThread extends Thread {

		private RawDataFile dataFile;

		public ProgressThread(RawDataFile dataFile) {
			this.dataFile = dataFile;
		}

		@Override
		public void run() {

			progressBar.setVisible(true);
			int val = 0;
			while (val < 100) 
			{
				if (previewThread != null && progressBar.isVisible()) {
					int progressMax = baselineCorrector.getProgressMax(dataFile);
					int progress = baselineCorrector.getProgress(dataFile);
					val = (int) Math.round((progressMax == 0 ? 0.0 : 100.0 * (double) progress / (double) progressMax));
					progressBar.setValue(val);
				}
			}
			this.kill();
		}

		public void kill() {
			// Kill preview processing
			if (previewThread != null) { 
				unset_VK_ESCAPE_KeyListener();
				previewThread = null; 
			}
			progressBar.setVisible(false);
		}
	}


	/**
	 * This function add all the additional components for this dialog over the
	 * original ParameterSetupDialogWithChromatogramPreview.
	 */
	@Override
	protected void addDialogComponents() {

		super.addDialogComponents();

		progressBar = new JProgressBar();
		progressBar.setValue(25);
		progressBar.setStringPainted(true);
		Border border = BorderFactory.createTitledBorder("Processing...     <Press \"ESC\" to cancel>    ");
		progressBar.setBorder(border);
		this.add(progressBar, BorderLayout.NORTH);
		progressBar.setVisible(false);
	}

	/**
	 * This function sets all the information into the plot chart
	 */
	@Override
	protected void loadPreview(TICPlot ticPlot, RawDataFile dataFile, Range rtRange, Range mzRange) {

		baselineCorrector.initProgress(dataFile);

		// Run process
		if (previewThread == null || previewThread.getCrashed()) {
			previewThread = new PreviewThread(ticPlot, dataFile, rtRange, mzRange);
			previewThread.start();
			
			// Run progress
			progressThread = new ProgressThread(dataFile);
			progressThread.start();
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

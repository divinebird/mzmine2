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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.tic.PlotType;
import net.sf.mzmine.modules.visualization.tic.TICDataSet;
import net.sf.mzmine.modules.visualization.tic.TICPlot;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialogWithChromatogramPreview;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskEvent;
import net.sf.mzmine.taskcontrol.TaskListener;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.RSession;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.RSession.RengineType;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;



/**
 * @description This class extends ParameterSetupDialogWithChromatogramPreview class. 
 * This is used to preview how the selected baseline correction method and its parameters 
 * works over the raw data file.
 * 
 * @author Gauthier Boaglio
 * @date Nov 6, 2014
 */
public class BaselineCorrectorSetupDialog extends ParameterSetupDialogWithChromatogramPreview
implements TaskListener {

	// Logger.
	private static final Logger LOG = Logger.getLogger(
			ParameterSetupDialogWithChromatogramPreview.class.getName());

	private ParameterSet correctorParameters;
	private BaselineCorrector baselineCorrector;

	private PreviewTask previewTask = null;
	private Thread previewThread = null;


	// Listen to VK_ESCAPE KeyEvent and allow to abort preview computing 
	// if input parameters gave a computation task that is about to take forever...
	KeyListener keyListener = new KeyListener() {
		@Override
		public void keyPressed(KeyEvent ke) {
						
			int keyCode = ke.getKeyCode();
			if (keyCode == KeyEvent.VK_ESCAPE) {

				LOG.info("<ESC> Presssed.");				
				previewTask.kill();
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


	class PreviewTask extends AbstractTask {

		private TICPlot ticPlot;
		private RawDataFile dataFile;
		private	Range rtRange;
		private Range mzRange;
		private BaselineCorrectorSetupDialog dialog;
		private ProgressThread progressThread;

		private RSession rSession;
		
		private boolean userCanceled;


		public PreviewTask(BaselineCorrectorSetupDialog dialog, TICPlot ticPlot, RawDataFile dataFile, Range rtRange, Range mzRange) {
			
			this.dialog = dialog;
			this.ticPlot = ticPlot;
			this.dataFile = dataFile;
			this.rtRange = rtRange;
			this.mzRange = mzRange;
			
			this.userCanceled = false;
			
			this.addTaskListener(dialog);
		}

		public RawDataFile getDataFile() {
			return this.dataFile;
		}
		public RengineType getRengineType() {
			return this.rSession.getRengineType();
		}

		@Override
		public void run() {

			// Update the status of this task
			setStatus(TaskStatus.PROCESSING);

			// Check R availability, by trying to open the connection
			try {
				String[] reqPackages = baselineCorrector.getRequiredRPackages();
				this.rSession = new RSession(baselineCorrector.getRengineType(), reqPackages);
				this.rSession.open();
			}
			catch (Throwable t) {
				String msg = t.getMessage();
				LOG.log(Level.SEVERE, "Baseline correction error", msg);
				errorMessage = msg;
				setStatus(TaskStatus.ERROR);
				this.rSession = null;
				return;
			}

			// Check & load required R packages
			String missingPackage = null;
			missingPackage = this.rSession.loadRequiredPackages();
			if (missingPackage != null) {
				String msg = "The \"" + baselineCorrector.getName() + "\" requires " +
						"the \"" + missingPackage + "\" R package, which couldn't be loaded - is it installed in R?";
				LOG.log(Level.SEVERE, "Baseline correction error", msg);
				errorMessage = msg;
				setStatus(TaskStatus.ERROR);
				return;
			}


			// Set VK_ESCAPE KeyEvent listeners
			set_VK_ESCAPE_KeyListener();

			// Reset TIC plot
			ticPlot.removeAllTICDataSets();
			ticPlot.setPlotType(getPlotType());

			// Add the original raw data file
			int scanNumbers[] = dataFile.getScanNumbers(1, rtRange);
			TICDataSet ticDataset = new TICDataSet(dataFile, scanNumbers, mzRange, null, getPlotType());
			ticPlot.addTICDataset(ticDataset);

			try {

				// Start progress bar
				baselineCorrector.initProgress(dataFile);
				progressThread = new ProgressThread(this.dialog, this, dataFile);
				progressThread.start();

				// Create a new corrected raw data file
				RawDataFile newDataFile = baselineCorrector.correctDatafile(this.rSession, dataFile, correctorParameters);

				// If successful, add the new data file
				if (newDataFile != null) {
					int newScanNumbers[] = newDataFile.getScanNumbers(1, rtRange);
					TICDataSet newDataset = new TICDataSet(newDataFile, newScanNumbers, mzRange, null, getPlotType());
					ticPlot.addTICDataset(newDataset);

					// Show the trend line as well
					XYDataset tlDataset = createBaselineDataset(dataFile, newDataFile, getPlotType());
					ticPlot.addTICDataset(tlDataset);
				}
			} catch (IOException e) {				// Writing error
				e.printStackTrace();
				//baselineCorrector.initProgress(dataFile);
			} catch (IllegalStateException e) {		// R computing error
				if (!this.userCanceled)
					e.printStackTrace();
			}

			// Task is over: Restore "parametersChanged" listeners
			unset_VK_ESCAPE_KeyListener();

			// We're done!
			setStatus(TaskStatus.FINISHED);
		}

		public void kill() {

			RawDataFile dataFile = getPreviewDataFile();
			if (baselineCorrector != null && dataFile != null) { 

					this.userCanceled = true;
				
					// Turn off R instance
					this.rSession.close(true);
				
					// Cancel task
					this.cancel();
					// Release "ESC" listener
					unset_VK_ESCAPE_KeyListener();
					// Abort current processing thread
					baselineCorrector.setAbortProcessing(dataFile, true); 
					
					LOG.info("Preview task canceled!");
			}

		}

		/* (non-Javadoc)
		 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
		 */
		@Override
		public String getTaskDescription() {
			return baselineCorrector.getName() + ": preview for file " + dataFile.getName();
		}

		/* (non-Javadoc)
		 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
		 */
		@Override
		public double getFinishedPercentage() {
			return baselineCorrector.getFinishedPercentage(dataFile);
		}


	}

	class ProgressThread extends Thread {

		private RawDataFile dataFile;
		private PreviewTask previewTask;
		private BaselineCorrectorSetupDialog dialog;
		private JProgressBar progressBar;

		public ProgressThread(BaselineCorrectorSetupDialog dialog, PreviewTask previewTask, RawDataFile dataFile) {
			this.previewTask = previewTask;
			this.dataFile = dataFile;
			this.dialog = dialog;
		}

		@Override
		public void run() {

			addProgessBar();
			while ((this.previewTask != null && this.previewTask.getStatus() == TaskStatus.PROCESSING)) 
			{

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						progressBar.setValue( (int) Math.round(100.0 * baselineCorrector.getFinishedPercentage(dataFile)) );
					}
				});
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// Clear GUI stuffs
			removeProgessBar();
			unset_VK_ESCAPE_KeyListener();
		}


		private void addProgessBar() {
			// Add progress bar
			progressBar = new JProgressBar();
			progressBar.setValue(25);
			progressBar.setStringPainted(true);
			Border border = BorderFactory.createTitledBorder("Processing...     <Press \"ESC\" to cancel>    ");
			progressBar.setBorder(border);
			this.dialog.add(progressBar, BorderLayout.NORTH);
			//			this.dialog.repaint();
			progressBar.setVisible(true);
			this.dialog.pack();
		}
		private void removeProgessBar() {
			// Remove progress bar
			progressBar.setVisible(false);
			this.dialog.remove(progressBar);
			this.dialog.pack();
		}

	}


	/**
	 * This function sets all the information into the plot chart
	 */
	@Override
	protected void loadPreview(TICPlot ticPlot, RawDataFile dataFile, Range rtRange, Range mzRange) {

		boolean ready = true;
		// Abort previous preview task. 
		if (previewTask != null && previewTask.getStatus() == TaskStatus.PROCESSING) {
			ready = false;
			previewTask.kill();
			try {
				previewThread.join();
				ready = true;
			} catch (InterruptedException e) {
				ready = false;
			}
		}

		//**ready = (ready || previewTask.getRengineType() == RengineType.RCaller);
		// Start processing new preview task.
		if (ready && (previewTask == null || previewTask.getStatus() != TaskStatus.PROCESSING)) {

			baselineCorrector.initProgress(dataFile);
			previewTask = new PreviewTask(this, ticPlot, dataFile, rtRange, mzRange);
			previewThread = new Thread(previewTask);
			LOG.info("Launch preview task.");
			previewThread.start();
		}
	}


	/**
	 * Quick way to recover the baseline plot (by subtracting the corrected file to the original one).
	 * @param dataFile      original datafile
	 * @param newDataFile   corrected datafile
	 * @param plotType      expected plot type
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


	/* (non-Javadoc)
	 * @see net.sf.mzmine.taskcontrol.TaskListener#statusChanged(net.sf.mzmine.taskcontrol.TaskEvent)
	 */
	@Override
	public void statusChanged(TaskEvent e) {
		if (e.getStatus() == TaskStatus.ERROR) {
			MZmineCore.getDesktop().displayErrorMessage( "Error of preview task ", e.getSource().getErrorMessage());
			hidePreview();
		} 
	}

}




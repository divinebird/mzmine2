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
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.rosuda.JRI.Rengine;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.RawDataFileWriter;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleScan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.RUtilities;
import net.sf.mzmine.util.Range;

/**
 * @description Abstract corrector class for baseline correction. Has to be specialized via the
 * implementation of a "BaselineProvider".
 * 
 * @author Gauthier Boaglio
 * @date Nov 6, 2014
 */
public abstract class BaselineCorrector implements BaselineProvider, MZmineModule {

	// Logger.
	private static final Logger LOG = Logger.getLogger(BaselineCorrectionTask.class.getName());

	// Used to give ability to stop processing when the referring task is canceled 
	boolean isAborted;

	// Original data file and newly created baseline corrected file.
	private RawDataFile origDataFile;
	private RawDataFile correctedDataFile;

	// Progress counters (Used to give progress feedback to the referring task).
	private int progress;
	private int progressMax;

	// Filename suffix.
	private String suffix;

	// General parameters (common to all baseline correction methods).
	private double binWidth;
	private boolean useBins;
	private int msLevel;
	private ChromatogramType chromatogramType;
	
	
	/**
	 * Initialization
	 */
	public BaselineCorrector() {

		isAborted = false;

		// Check for R requirements
		String missingPackage = checkRPackages(getRequiredRPackages());
		if (missingPackage != null) {
			throw new IllegalStateException("The \"" + missingPackage + "\" R package couldn't be loaded - is it installed in R?");
		}

	}
	
	/**
	 * Getting original DataFile and general parameters 
	 * @param dataFile
	 * @param generalParameters The parameters common to all methods (inherited from "BaselineCorrectionParameters")
	 */
	public void setGeneralParameters(RawDataFile dataFile, ParameterSet generalParameters) {
		
		// Working DataFile
		origDataFile = dataFile;
		
		// Init.
		correctedDataFile = null;
		progressMax = 0;
		progress = 0;

		// Get common parameters.
		suffix = generalParameters.getParameter(BaselineCorrectionParameters.SUFFIX).getValue();
		binWidth = generalParameters.getParameter(BaselineCorrectionParameters.MZ_BIN_WIDTH).getValue();
		useBins = generalParameters.getParameter(BaselineCorrectionParameters.USE_MZ_BINS).getValue();
		msLevel = generalParameters.getParameter(BaselineCorrectionParameters.MS_LEVEL).getValue();
		chromatogramType = generalParameters.getParameter(BaselineCorrectionParameters.CHROMOTAGRAM_TYPE).getValue();
	}
	
	/**
	 * Try loading packages required by this instance of baseline corrector.
	 * Gives a name if anything went wrong.
	 * @param packages List of required packages.
	 * @return The name of the first failing one, or null if everything went right.
	 */
	public String checkRPackages(String[] packages) {
		// Get R engine.
		try {
			final Rengine rEngine = RUtilities.getREngine();
			// Check required packages.
			for (int i=0; i < packages.length; ++i)
				if (rEngine.eval("require(" + packages[i] + ")").asBool().isFALSE()) return packages[i];
			return null;
		}
		catch (Throwable t) {
			throw new IllegalStateException(
					"Baseline correction requires R but it couldn't be loaded (" + t.getMessage() + ')');
		}
	}

	public RawDataFile correctDatafile(RawDataFile dataFile, ParameterSet parameters) throws IOException
	{
		// Get very last information from root module setup
		this.setGeneralParameters(dataFile, MZmineCore.getConfiguration().getModuleParameters(BaselineCorrectionModule.class));
		
		try {
			// Create a new temporary file to write in.
			RawDataFileWriter rawDataFileWriter =
					MZmineCore.createNewFile(origDataFile.getName() + ' ' + suffix);

			// Determine number of bins.
			int numBins = useBins ? (int) Math.ceil(origDataFile.getDataMZRange().getSize() / binWidth) : 1;

			// Get MS levels.
			int[] levels = origDataFile.getMSLevels();

			// Measure progress and find MS-level.
			boolean foundLevel = msLevel == 0;
			progressMax = 0;
			for (int level : levels) {
				boolean isMSLevel = msLevel == level;
				int numScans = origDataFile.getScanNumbers(level).length;
				foundLevel |= isMSLevel;
				progressMax += isMSLevel || msLevel == 0 ? 2 * numScans + numBins : numScans;
			}

			// Is the specified MS-level present?
			if (!foundLevel) {
				throw new IllegalArgumentException("The data file doesn't contain data for MS-level " + msLevel + '.');
			}

			// Which chromatogram type.
			boolean useTIC = (chromatogramType == ChromatogramType.TIC);

			// Process each MS level.
			for (int level : levels) {

				if (!isAborted) {
					if (level == msLevel || msLevel == 0) {

						// Correct baseline for this MS-level.
						if (useTIC) {
							correctTICBaselines(rawDataFileWriter, level, numBins, parameters);
						} else {
							correctBasePeakBaselines(rawDataFileWriter, level, numBins, parameters);
						}
					} else {

						// Copy scans for this MS-level.
						copyScansToWriter(rawDataFileWriter, level);
					}
				}
			}

			// If the referring task was canceled, stop processing.
			if (!isAborted) {
				// Finalize writing.
				correctedDataFile = rawDataFileWriter.finishWriting();
			}
		}
		catch (Throwable t) {

			LOG.log(Level.SEVERE, "Baseline correction error", t);
			//LOG.log(Level.SEVERE, "Baseline correction error", ExceptionUtils.getStackTrace(t));
		}
		return correctedDataFile;
	}
	
	/**
	 * Copy scans to RawDataFileWriter.
	 *
	 * @param writer writer to copy scans to.
	 * @param level  MS-level of scans to copy.
	 * @throws IOException if there are i/o problems.
	 */
	private void copyScansToWriter(final RawDataFileWriter writer,
			final int level)
					throws IOException {

		LOG.finest("Copy scans");

		// Get scan numbers for MS-level.
		final int[] scanNumbers = origDataFile.getScanNumbers(level);
		final int numScans = scanNumbers.length;

		// Create copy of scans.
		for (int scanIndex = 0; !isAborted && scanIndex < numScans; scanIndex++) {

			// Get original scan.
			final Scan origScan = origDataFile.getScan(scanNumbers[scanIndex]);

			// Get data points (m/z and intensity pairs) of the original scan
			final DataPoint[] origDataPoints = origScan.getDataPoints();
			final DataPoint[] newDataPoints = new DataPoint[origDataPoints.length];

			// Copy original data points.
			int i = 0;
			for (final DataPoint dp : origDataPoints) {
				newDataPoints[i++] = new SimpleDataPoint(dp);
			}

			// Create new copied scan.
			final SimpleScan newScan = new SimpleScan(origScan);
			newScan.setDataPoints(newDataPoints);
			writer.addScan(newScan);
			progress++;
		}
	}

	/**
	 * Correct the baselines (using base peak chromatograms).
	 *
	 * @param writer  data file writer.
	 * @param level   the MS level.
	 * @param numBins number of m/z bins.
	 * @param parameters parameters specific to the actual method for baseline computing.
	 * @throws IOException if there are i/o problems.
	 */
	private void correctBasePeakBaselines(final RawDataFileWriter writer, final int level, final int numBins, final ParameterSet parameters)
			throws IOException {

		// Get scan numbers from original file.
		final int[] scanNumbers = origDataFile.getScanNumbers(level);
		final int numScans = scanNumbers.length;

		// Build chromatograms.
		LOG.finest("Building base peak chromatograms.");
		final double[][] baseChrom = buildBasePeakChromatograms(level, numBins);

		// Calculate baselines: done in-place, i.e. overwrite chromatograms to save memory.
		LOG.finest("Calculating baselines.");
		for (int binIndex = 0; !isAborted && binIndex < numBins; binIndex++) {
			baseChrom[binIndex] = computeBaseline(baseChrom[binIndex], parameters);
			progress++;
		}

		// Subtract baselines.
		LOG.finest("Subtracting baselines.");
		for (int scanIndex = 0; !isAborted && scanIndex < numScans; scanIndex++) {

			// Get original scan.
			final Scan origScan = origDataFile.getScan(scanNumbers[scanIndex]);

			// Get data points (m/z and intensity pairs) of the original scan
			final DataPoint[] origDataPoints = origScan.getDataPoints();

			// Create and write new corrected scan.
			final SimpleScan newScan = new SimpleScan(origScan);
			newScan.setDataPoints(subtractBasePeakBaselines(origDataPoints, baseChrom, numBins, scanIndex));
			writer.addScan(newScan);
			progress++;
		}
	}

	/**
	 * Correct the baselines (using TIC chromatograms).
	 *
	 * @param writer     data file writer.
	 * @param level      the MS level.
	 * @param numBins    number of m/z bins.
	 * @param parameters parameters specific to the actual method for baseline computing.
	 * @throws IOException if there are i/o problems.
	 */
	private void /*double[]*/ correctTICBaselines(final RawDataFileWriter writer, final int level, final int numBins, final ParameterSet parameters)
			throws IOException {

		// Get scan numbers from original file.
		final int[] scanNumbers = origDataFile.getScanNumbers(level);
		final int numScans = scanNumbers.length;

		// Build chromatograms.
		LOG.finest("Building TIC chromatograms.");
		final double[][] baseChrom = buildTICChromatograms(level, numBins);

		// Calculate baselines: done in-place, i.e. overwrite chromatograms to save memory.
		LOG.finest("Calculating baselines.");
		for (int binIndex = 0; !isAborted && binIndex < numBins; binIndex++) {

			// Calculate baseline.
			//final double[] baseline = asymBaseline(baseChrom[binIndex]);
			final double[] baseline = computeBaseline(baseChrom[binIndex], parameters);


			// Normalize the baseline w.r.t. chromatogram (TIC).
			for (int scanIndex = 0; scanIndex < numScans; scanIndex++) {
				final double bc = baseChrom[binIndex][scanIndex];
				if (bc != 0.0) {
					baseChrom[binIndex][scanIndex] = baseline[scanIndex] / bc;
				}
			}
			progress++;
		}

		// Subtract baselines.
		LOG.finest("Subtracting baselines.");
		for (int scanIndex = 0; !isAborted && scanIndex < numScans; scanIndex++) {

			// Get original scan.
			final Scan origScan = origDataFile.getScan(scanNumbers[scanIndex]);

			// Get data points (m/z and intensity pairs) of the original scan
			final DataPoint[] origDataPoints = origScan.getDataPoints();

			// Create and write new corrected scan.
			final SimpleScan newScan = new SimpleScan(origScan);
			newScan.setDataPoints(subtractTICBaselines(origDataPoints, baseChrom, numBins, scanIndex));
			writer.addScan(newScan);
			progress++;
		}
		
	}

	/**
	 * Constructs base peak (max) chromatograms - one for each m/z bin.
	 *
	 * @param level   the MS level.
	 * @param numBins number of m/z bins.
	 * @return the chromatograms as double[number of bins][number of scans].
	 */
	private double[][] buildBasePeakChromatograms(final int level, final int numBins) {

		// Get scan numbers from original file.
		final int[] scanNumbers = origDataFile.getScanNumbers(level);
		final int numScans = scanNumbers.length;

		// Determine MZ range.
		final Range mzRange = origDataFile.getDataMZRange();

		// Create chromatograms.
		final double[][] chromatograms = new double[numBins][numScans];

		for (int scanIndex = 0; !isAborted && scanIndex < numScans; scanIndex++) {

			// Get original scan.
			final Scan scan = origDataFile.getScan(scanNumbers[scanIndex]);

			// Process data points.
			for (final DataPoint dataPoint : scan.getDataPoints()) {

				final int bin = mzRange.binNumber(numBins, dataPoint.getMZ());
				final double value = chromatograms[bin][scanIndex];
				chromatograms[bin][scanIndex] = Math.max(value, dataPoint.getIntensity());
			}
			progress++;
		}

		return chromatograms;
	}

	/**
	 * Constructs TIC (sum) chromatograms - one for each m/z bin.
	 *
	 * @param level   the MS level.
	 * @param numBins number of m/z bins.
	 * @return the chromatograms as double[number of bins][number of scans].
	 */
	private double[][] buildTICChromatograms(final int level, final int numBins) {

		// Get scan numbers from original file.
		final int[] scanNumbers = origDataFile.getScanNumbers(level);
		final int numScans = scanNumbers.length;

		// Determine MZ range.
		final Range mzRange = origDataFile.getDataMZRange();

		// Create chromatograms.
		final double[][] chromatograms = new double[numBins][numScans];

		for (int scanIndex = 0; !isAborted && scanIndex < numScans; scanIndex++) {

			// Get original scan.
			final Scan scan = origDataFile.getScan(scanNumbers[scanIndex]);

			// Process data points.
			for (final DataPoint dataPoint : scan.getDataPoints()) {

				chromatograms[mzRange.binNumber(numBins, dataPoint.getMZ())][scanIndex] += dataPoint.getIntensity();
			}
			progress++;
		}

		return chromatograms;
	}

	/**
	 * Perform baseline correction in bins (base peak).
	 *
	 * @param dataPoints input data points to correct.
	 * @param baselines  the baselines - one per m/z bin.
	 * @param numBins    the number of m/z bins.
	 * @param scanIndex  the current scan index that these data points come from.
	 * @return the corrected data points.
	 */
	private DataPoint[] subtractBasePeakBaselines(final DataPoint[] dataPoints,
			final double[][] baselines,
			final int numBins,
			final int scanIndex) {

		// Create an ArrayList for new data points.
		final DataPoint[] newDataPoints = new DataPoint[dataPoints.length];

		// Determine MZ range.
		final Range mzRange = origDataFile.getDataMZRange();

		// Loop through all original data points.
		int i = 0;
		for (final DataPoint dp : dataPoints) {

			// Subtract baseline.
			final double mz = dp.getMZ();
			final int bin = mzRange.binNumber(numBins, mz);
			final double baselineIntenstity = baselines[bin][scanIndex];
			newDataPoints[i++] = baselineIntenstity <= 0.0 ?
					new SimpleDataPoint(dp) :
						new SimpleDataPoint(mz, Math.max(0.0, dp.getIntensity() - baselineIntenstity));
		}

		// Return the new data points.
		return newDataPoints;
	}

	/**
	 * Perform baseline correction in bins (TIC).
	 *
	 * @param dataPoints input data points to correct.
	 * @param baselines  the baselines - one per m/z bin.
	 * @param numBins    the number of m/z bins.
	 * @param scanIndex  the current scan index that these data points come from.
	 * @return the corrected data points.
	 */
	private DataPoint[] subtractTICBaselines(final DataPoint[] dataPoints,
			final double[][] baselines,
			final int numBins,
			final int scanIndex) {

		// Create an ArrayList for new data points.
		final DataPoint[] newDataPoints = new DataPoint[dataPoints.length];

		// Determine MZ range.
		final Range mzRange = origDataFile.getDataMZRange();

		// Loop through all original data points.
		int i = 0;
		for (final DataPoint dp : dataPoints) {

			// Subtract baseline.
			final double mz = dp.getMZ();
			final int bin = mzRange.binNumber(numBins, mz);
			final double baselineIntenstity = baselines[bin][scanIndex];
			newDataPoints[i++] = baselineIntenstity <= 0.0 ?
					new SimpleDataPoint(dp) :
						new SimpleDataPoint(mz, Math.max(0.0, dp.getIntensity() * (1.0 - baselineIntenstity)));
		}

		// Return the new data points.
		return newDataPoints;
	}


	// Correction progress getters
	public int getProgress() {
		return progress;
	}
	public int getProgressMax() {
		return progressMax;
	}
	// Correction chromatogram type
	public ChromatogramType getChromatogramType() {
		return MZmineCore.getConfiguration().getModuleParameters(BaselineCorrectionModule.class)
				.getParameter(BaselineCorrectionParameters.CHROMOTAGRAM_TYPE).getValue();
	}

	/**
	 * Switch to abort processing (used from task mode)
	 * @param abort If we shall abort
	 */
	void setAbortProcessing(boolean abort) {
		this.isAborted = abort;
	}

}

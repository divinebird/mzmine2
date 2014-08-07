/*
 * @Author Gauthier Boaglio
 */

package net.sf.mzmine.modules.peaklistmethods.merging.rt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.RawDataFileWriter;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.datamodel.impl.SimpleScan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakSorter;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.ScanUtils;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

import org.apache.commons.lang.ArrayUtils;

/**
 * 
 */
class PeakMergerTask extends AbstractTask {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	// RDF
	RawDataFile dataFile, ancestorDataFile, workingDataFile;

	// peaks lists
	private PeakList peakList, mergedPeakList;

	// peaks counter
	private int processedPeaks, totalPeaks;

	// parameter values
	private String suffix;
	private MZTolerance mzTolerance;
	private RTTolerance rtTolerance;
	private boolean useOldestRDFAncestor;
	private double detectedMZSearchWidth;
	private boolean useOnlyDetectedPeaks;
	private boolean cumulativeComputing;
	private boolean removeOriginal;
	private ParameterSet parameters;

	//private static final String ancestor_suffix = "#Ancestor";
	private static final String unpastableSep = MZmineCore.getUnpastableSep();
	private static final String autogenPrefix = MZmineCore.getAutogenPrefix();
	private static final double doublePrecision = 0.000001;


	/**
	 * @param rawDataFile
	 * @param parameters
	 */
	PeakMergerTask(PeakList peakList, ParameterSet parameters) {

		this.peakList = peakList;
		this.parameters = parameters;

		// Get parameter values for easier use
		suffix = parameters.getParameter(PeakMergerParameters.suffix)
				.getValue();
		mzTolerance = parameters.getParameter(
				PeakMergerParameters.mzTolerance).getValue();
		rtTolerance = parameters.getParameter(
				PeakMergerParameters.rtTolerance).getValue();

		this.useOldestRDFAncestor = parameters.getParameter(
				PeakMergerParameters.useOldestRDFAncestor).getValue();
		this.detectedMZSearchWidth = parameters.getParameter(
				PeakMergerParameters.detectedMZSearchWidth).getValue();
		this.useOnlyDetectedPeaks = parameters.getParameter(
				PeakMergerParameters.useOnlyDetectedPeaks).getValue();
		this.cumulativeComputing = parameters.getParameter(
				PeakMergerParameters.cumulativeComputing).getValue();

		removeOriginal = parameters.getParameter(
				PeakMergerParameters.autoRemove).getValue();

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "RT peaks merger on " + peakList;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		if (totalPeaks == 0)
			return 0.0f;
		return (double) processedPeaks / (double) totalPeaks;
	}

	/**
	 * @see Runnable#run()
	 */
	public void run() {

		setStatus(TaskStatus.PROCESSING);
		logger.info("Running RT peaks merger on " + peakList);

		////////////////////////////////////////////////////////////////////////////////////
		// We assume source peakList contains one single data file
		this.dataFile = peakList.getRawDataFile(0); 
		List<Feature> sortedPeaks = Arrays.asList(peakList.getPeaks(this.dataFile));

		this.ancestorDataFile = this.dataFile.getAncestorDataFile(true);

		this.workingDataFile = this.getWorkingDataFile(sortedPeaks);

		logger.log(Level.INFO, "Base raw data file: " + this.dataFile.getName());
		logger.log(Level.INFO, "Working raw data file: " + this.workingDataFile.getName());
		////////////////////////////////////////////////////////////////////////////////////


		// Create a new RT merged peakList
		//mergedPeakList = new SimplePeakList(peakList + " " + suffix, peakList.getRawDataFiles());
		this.initMergedPeakList();



		// Loop through all peaks
		totalPeaks = sortedPeaks.size();
		int nb_empty_peaks = 0;

		for (int ind = 0; ind < totalPeaks; ind++) {

			if (isCanceled())
				return;

			Feature aPeak = sortedPeaks.get(ind);

			// Check if peak was already deleted
			if (aPeak == null) {
				processedPeaks++;
				continue;
			}


			// Build RT group
			ArrayList<Feature> groupedPeaks = this.getPeaksGroupByRT(aPeak, sortedPeaks);
			// Sort by intensity (descending)
			Collections.sort(groupedPeaks, new PeakSorter(SortingProperty.Height, SortingDirection.Descending));

			// TODO: debug stuffs here !!!! 


			// Start from most intense peak
			Feature oldPeak = groupedPeaks.get(0);
			// Get scan numbers of interest
			List<Integer> scan_nums = Arrays.asList(ArrayUtils.toObject(oldPeak.getScanNumbers()));


			////double maxHeight = oldPeak.getHeight();
			int originScanNumber = oldPeak.getRepresentativeScanNumber();
			MergedPeak newPeak = new MergedPeak(this.workingDataFile);
			int totalScanNumber = this.workingDataFile.getNumOfScans();
			Range mzRange = this.workingDataFile.getDataMZRange(1);		// No MZ requirement (take the full dataFile MZ Range)
			Scan scan;
			DataPoint dataPoint;

			// Look for dataPoint related to this peak to the left
			int scanNumber = originScanNumber;
			scanNumber--;
			while (scanNumber > 0 && scanNumber >= scan_nums.get(0))
			{
				scan = this.workingDataFile.getScan(scanNumber);

				if (scan == null) {
					scanNumber--;
					continue;
				}

				if (scan.getMSLevel() != 1) {
					scanNumber--;
					continue;
				}


				// Switch accordingly to option "Only DETECTED peaks"
				dataPoint = getMergedDataPointFromPeakGroup(scan, groupedPeaks, mzRange);


				if (dataPoint != null /* || dataPoint.getIntensity() < minimumHeight */)
				{
					newPeak.addMzPeak(scanNumber, dataPoint);
					////if (dataPoint.getIntensity() > maxHeight) { maxHeight = dataPoint.getIntensity(); }
					//break;
				}

				scanNumber--;
			}


			// Add original DataPoint
			//newPeak.addMzPeak(originScanNumber, oldPeak.getDataPoint(originScanNumber));
			scanNumber = originScanNumber;
			scan = this.workingDataFile.getScan(originScanNumber);
			// Switch accordingly to option "Only DETECTED peaks"
			dataPoint = getMergedDataPointFromPeakGroup(scan, groupedPeaks, mzRange);
			//
			if (dataPoint == null && this.useOnlyDetectedPeaks) {
				this.useOnlyDetectedPeaks = false;
				dataPoint = getMergedDataPointFromPeakGroup(scan, groupedPeaks, mzRange);
				this.useOnlyDetectedPeaks = true;
				//
				logger.log(Level.WARNING, "DETECTED Middle / Main peak (DataPoint) not found for scan: #" + scanNumber 
						+ ". Using \"Base Peak Intensity\" instead (May lead to inaccurate results).");
				//break;
			}
			//
			if (dataPoint != null /* || dataPoint.getIntensity() < minimumHeight */)
			{
				newPeak.addMzPeak(scanNumber, dataPoint);
				////if (dataPoint.getIntensity() > maxHeight) { maxHeight = dataPoint.getIntensity(); }
			}


			// Look to the right
			//scanNumber = originScanNumber;
			scanNumber++;
			while (scanNumber <= totalScanNumber && scanNumber <= scan_nums.get(scan_nums.size()-1)) 
			{
				scan = this.workingDataFile.getScan(scanNumber);

				if (scan == null) {
					scanNumber++;
					continue;
				}

				if (scan.getMSLevel() != 1) {
					scanNumber++;
					continue;
				}


				// Switch accordingly to option "Only DETECTED peaks"
				dataPoint = getMergedDataPointFromPeakGroup(scan, groupedPeaks, mzRange);


				if (dataPoint != null /* || dataPoint.getIntensity() < minimumHeight */)
				{
					newPeak.addMzPeak(scanNumber, dataPoint);
					////if (dataPoint.getIntensity() > maxHeight) { maxHeight = dataPoint.getIntensity(); }
					//break;
				}

				scanNumber++;
			}

			// Finishing the merged peak
			newPeak.finishMergedPeak();

			//			newPeak.setMostIntenseFragmentScanNumber(oldPeak
			//					.getMostIntenseFragmentScanNumber());


			// Create new peak list row for this merged peak 
			PeakListRow oldRow = peakList.getPeakRow(oldPeak);

			// TODO: Might be VIOLENT (quick and dirty => to be verified)
			if (newPeak.getScanNumbers().length == 0) {
				++nb_empty_peaks;
				//#continue;
				logger.log(Level.WARNING, "0 scans peak found !");
				break;
			}

			this.updateMergedPeakList(oldRow, newPeak);


			// Clear already treated peaks
			for (int g_i=0; g_i < groupedPeaks.size(); g_i++) {
				sortedPeaks.set(sortedPeaks.indexOf(groupedPeaks.get(g_i)), null);
			}


			// Update completion rate
			processedPeaks++;

		}

		if (nb_empty_peaks > 0)
			logger.log(Level.WARNING, "Skipped \"" + nb_empty_peaks + "\" empty peaks !");

		// Setup and add new peakList to the project
		this.finishMergedPeakList();

		// Remove the original peakList if requested
		if (removeOriginal)
			MZmineCore.getCurrentProject().removePeakList(peakList);

		logger.info("Finished RT peaks merger on " + peakList);
		setStatus(TaskStatus.FINISHED);

	}

	static final public Double getDoublePrecision() {
		return doublePrecision;
	}

	private DataPoint getMergedDataPointFromPeakGroup(Scan refScan, ArrayList<Feature> peaksGroup, Range mzRange)
	{
		DataPoint dataPoint = null, newDp;

		int scanNumber = refScan.getScanNumber();

		// Switch accordingly to option "Only DETECTED peaks"
		/** Now compatible with "cumulativeComputing" !! 
		 *		=> (See "getWorkingDataFile()#(// Cumulate only DETECTED peaks by hand)")
		 */
		if (this.useOnlyDetectedPeaks /*&& !this.cumulativeComputing*/) {
			List<DataPoint> l_data_pts = new ArrayList<DataPoint>();
			double mzHalfSearch = this.detectedMZSearchWidth / 2.0;
			for (int i=0; i < peaksGroup.size(); i++) {
				DataPoint dp0 = peaksGroup.get(i).getDataPoint(scanNumber);
				if (dp0 != null)
				{
					double mz = dp0.getMZ();
					DataPoint dp = null;

					// If we are working on a different file than the one used to detect the peaks in the first place,
					// or its "TICed" version (in which cases this is a non-sense to look for the exact m/z for this DETECTED peak):
					// we extend the search!                  /*to the user-specified tolerance window*/
					////if (this.dataFile != this.ancestorDataFile && this.workingDataFile == this.ancestorDataFile) 
					if (this.dataFile != this.workingDataFile) 
					{
						// Get the only one data point available in the scan (no matter the m/z) - Infinite window
						// (Remember that the "Cumulative / TIC" RDF contains only one DataPoint per scan !!)
						if (this.cumulativeComputing) {
							dp = ScanUtils.findBasePeak(refScan, mzRange);
						} 
						// Get the top data point in the user-specified window
						else {
							DataPoint[] dp_arr = refScan.getDataPointsByMass(new Range(mz - mzHalfSearch, mz + mzHalfSearch));
							dp =  ScanUtils.findTopDataPoint(dp_arr);
						}
					}
					// Otherwise, just find out the correct corresponding data point - Exact m/z
					else
					{
						Range mz_range = new Range(mz - doublePrecision, mz + doublePrecision);
						if (refScan.getDataPointsByMass(mz_range).length > 1)
							logger.log(Level.SEVERE, "Number of returned DataPoints should be '1' !!");
						if (refScan.getDataPointsByMass(mz_range).length > 0) {
							dp = refScan.getDataPointsByMass(mz_range)[0];
						}
					}
					if (dp != null)
						l_data_pts.add(dp);
				}
			}

			if (l_data_pts.size() == 0) {
				logger.log(Level.WARNING, "No DataPoint found for current group at scan #" + scanNumber 
						+ " ! Maybe try with a larger \"DETECTED m/z search window\", "
						+ "or simply uncheck the \"Use DETECTED peaks only\" option...");
				newDp = null;
			} else {
				Object[] lst2arr = l_data_pts.toArray();
				DataPoint[] data_pts = Arrays.copyOf(lst2arr, lst2arr.length, DataPoint[].class);
				newDp = ScanUtils.findTopDataPoint(data_pts);
			}

		} else {
			newDp = ScanUtils.findBasePeak(refScan, mzRange);
		}

		dataPoint = (newDp == null) ? newDp : new SimpleDataPoint(newDp);

		return dataPoint;
	}


	private ArrayList<Feature> getPeaksGroupByRT(Feature mainPeak, List<Feature> sortedPeaks) 
	{
		HashSet<Feature> groupedPeaks = new HashSet<Feature>();

		double mainMZ   = mainPeak.getMZ();
		double mainRT   = mainPeak.getRT();
		int mainScanNum = mainPeak.getRepresentativeScanNumber();

		//groupedPeaks.add(mainPeak);

		//		boolean followingPeakFound;
		//		do {
		//
		//			// Assume we don't find match for n:th peak in the given RT range (which
		//			// will end the loop)
		//			followingPeakFound = false;

		// Loop through following peaks, and collect candidates for the n:th peak
		// in the RT range
		Vector<Feature> goodCandidates = new Vector<Feature>();
		for (int ind = 0; ind < sortedPeaks.size(); ind++) {

			Feature candidatePeak = sortedPeaks.get(ind);

			if (candidatePeak == null)
				continue;

			// Get properties of the candidate peak
			double candidatePeakMZ = candidatePeak.getMZ();
			double candidatePeakRT = candidatePeak.getRT();


			// Check if current peak is in RT range
			if (rtTolerance.checkWithinTolerance(candidatePeakRT, mainRT)
					&& mzTolerance.checkWithinTolerance(candidatePeakMZ, mainMZ)
					&& (!groupedPeaks.contains(candidatePeak))
					) 
			{
				goodCandidates.add(candidatePeak);
				//int idx = sortedPeaks.indexOf(candidatePeak);
				//logger.log(Level.INFO, "Peak '" + idx + "' is good candidate because: " + candidatePeakRT + " | " + mainRT);
			}

		}			

		// Add all good candidates to the group
		if (!goodCandidates.isEmpty()) {
			groupedPeaks.addAll(goodCandidates);
			//				followingPeakFound = true;
		}

		//
		//		} while (followingPeakFound);


		// Detect and remove "False goodCandidates" !
		// Merge scans: Use HashSet to preserve unicity
		HashSet<Integer> hs = new HashSet<Integer>();
		//for (int g_i=1; g_i < groupedPeaks.size(); g_i++) {
		for (Feature p : groupedPeaks) {
			hs.addAll(Arrays.asList(ArrayUtils.toObject(p.getScanNumbers())));
		}
		// Sort HashSet of scans (TreeSet) by ascending order
		List<Integer> scan_nums = new ArrayList<Integer>(new TreeSet<Integer>(hs)); 
		// Check for gaps
		if (scan_nums.get(scan_nums.size()-1) - scan_nums.get(0) != scan_nums.size()-1)
		{
			// Gaps exist, extract valid scans sequence (left connected scans)
			// The correct sequence MUST contain the "MainPeak"
			logger.log(Level.INFO, "Gaps in sequence: " + scan_nums.toString());
			List<Integer> scan_nums_ok = new ArrayList<Integer>();				
			//			int it = 0;
			//			scan_nums_ok.add(scan_nums.get(it));
			//			do {
			//				++it;
			//				scan_nums_ok.add(scan_nums.get(it));
			//			} while ((it + 1 - scan_nums.size() != 0) && (scan_nums.get(it+1) == scan_nums.get(it) + 1));
			int it = scan_nums.indexOf(mainScanNum);
			scan_nums_ok.add(scan_nums.get(it));
			// Get left side of the sequence 
			do {
				--it;
				scan_nums_ok.add(0, scan_nums.get(it));
			} while ((it != 0) && (scan_nums.get(it-1) == scan_nums.get(it) - 1));
			// Get right side of the sequence
			it = scan_nums.indexOf(mainScanNum);
			do {
				++it;
				scan_nums_ok.add(scan_nums.get(it));
			} while ((it + 1 - scan_nums.size() != 0) && (scan_nums.get(it+1) == scan_nums.get(it) + 1));
			logger.log(Level.INFO, "Valid sequence is: " + scan_nums_ok.toString());

			double rt_min = this.workingDataFile.getScan(scan_nums_ok.get(0)).getRetentionTime();
			double rt_max = this.workingDataFile.getScan(scan_nums_ok.get(scan_nums_ok.size()-1)).getRetentionTime();
			Range rt_range_ok = new Range(rt_min - doublePrecision, rt_max + doublePrecision);

			// List bad candidates
			Vector<Feature> badCandidates = new Vector<Feature>();
			//for (int i=0; i < groupedPeaks.size(); ++i) {
			for (Feature p : groupedPeaks) {
				if (!rt_range_ok.containsRange(p.getRawDataPointsRTRange())) {
					badCandidates.add(p);
					logger.log(Level.INFO, "Bad candidate found at: " + p.getRepresentativeScanNumber());
					logger.log(Level.INFO, "rt_range_ok :" + rt_range_ok.toString());
					logger.log(Level.INFO, "rt_badCandidates: " + p.getRawDataPointsRTRange().toString());
				}
			}

			//			if (groupedPeaks.size() == badCandidates.size()) {
			//				logger.log(Level.INFO, "HURKKKKK: " + groupedPeaks.size());
			//				for (int i=0; i < badCandidates.size(); ++i) { 
			//					ChromatographicPeak p = badCandidates.get(i);
			//					double rt_min_p = this.workingDataFile.getScan(p.getScanNumbers()[0]).getRetentionTime();
			//					double rt_max_p = this.workingDataFile.getScan(p.getScanNumbers()[p.getScanNumbers().length-1]).getRetentionTime();
			//					Range rt_range_p = new Range(rt_min_p, rt_max_p);
			//					logger.log(Level.INFO, "HURKKKKK_" + i + " : " + badCandidates.get(i).toString()); 
			//					logger.log(Level.INFO, "rt_range_ok" + rt_range_ok.toString());
			//					logger.log(Level.INFO, "rt_badCandidates" + badCandidates.get(i).getRawDataPointsRTRange().toString());
			//					logger.log(Level.INFO, "rt_badCandidates" + rt_range_p.toString());
			//				}
			//			}

			// Remove bad candidates
			groupedPeaks.removeAll(badCandidates);

		}

		//return groupedPeaks;
		return new ArrayList<Feature>(groupedPeaks);
	}


	RawDataFile getWorkingDataFile(List<Feature> detectedPeaks) 
	{
		RawDataFile working_rdf = null;

		// Choose the RDF to work with
		if (this.useOldestRDFAncestor) {
			//working_rdf = peakList.getRawDataFile(0).getAncestorDataFile(true);
			//this.oldestAncestorDataFile = working_rdf;
			if (this.ancestorDataFile != null) {
				////this.ancestorDataFile.setName(this.ancestorDataFile.getName() + ancestor_suffix);
				working_rdf = this.ancestorDataFile;
			}
			//			else {
			//				working_rdf = this.dataFile;
			//			}
		}
		//		else {
		//			//this.oldestAncestorDataFile = null;
		//			working_rdf = this.dataFile;
		//		}

		// If none of the above was reachable, use the regular RawDataFile
		if (working_rdf == null) working_rdf = this.dataFile;

		// Convert (on the fly) "working_rdf" into its cumulative version from either "dataFile" or "oldestAncestorDataFile"
		if (this.cumulativeComputing && working_rdf != null)	// The latter should always be true
		{
			try {
				//				working_rdf = new MutableRawDataFile(working_rdf.getName() + cum_suffix, (RawDataFileImpl) working_rdf);

				//				// WARNING: Quite quick and dirty...
				//				// "BRUTE" force Base Peak Intensity to Total Ion Count RDF chromatogram
				//				// (aka: cumulated intensity over the whole mzRange of the scan) 
				//				for (int i=0; i < working_rdf.getNumOfScans(); ++i) {
				//					int scan_num = working_rdf.getScanNumbers(1)[i];
				//					StorableScan scan = (StorableScan) working_rdf.getScan(scan_num);
				//					// Create a "cumulative" DataPoint
				//					logger.log(Level.FINER, "Scan '" + scan_num + " | " + scan.getStorageID() + "' OK !!!!!!!! => " + scan);
				//					DataPoint old_base_peak = ScanUtils.findBasePeak(scan, scan.getMZRange());
				//					DataPoint new_base_peak = new SimpleDataPoint(old_base_peak.getMZ(), ScanUtils.calculateTIC(scan, scan.getMZRange()));
				//					// Reduce scan DataPoints to a single one, the one resulting from cumulating process
				//					// See "ScanUtils.calculateTIC()"...
				//					SimpleScan s = new SimpleScan(scan);
				//					s.setDataPoints( new DataPoint[] { new_base_peak } );
				//					// Replacement of the scan by its "reduced" version
				//					StorableScan new_scan = new StorableScan(s, (RawDataFileImpl) scan.getDataFile(), scan.getNumberOfDataPoints(), scan.getStorageID(), true);
				//					((MutableRawDataFile) working_rdf).setScan(scan_num, new_scan);
				//				}

				//---
				// Prepare for writing a new temporary RawDataFile
				RawDataFileWriter rawDataFileWriter = MZmineCore.createNewFile(autogenPrefix + this.suffix + unpastableSep + working_rdf.getName()
						+ unpastableSep + String.valueOf(java.util.UUID.randomUUID())); //cum_suffix + ".tmp");

				// Loop over all Scans
				int[] scanNumbers = working_rdf.getScanNumbers(1);
				int totalScans = scanNumbers.length;
				for (int i = 0; i < totalScans; ++i) {
					// Get source Scan
					Scan scan = working_rdf.getScan(scanNumbers[i]);

					// Deep copy (not clone !!!!!!!!! ) the source Scan
					SimpleScan scanCopy = new SimpleScan(scan, null);
					// Create a "cumulative" DataPoint
					DataPoint old_base_peak = ScanUtils.findBasePeak(scan, scan.getMZRange());
					DataPoint new_base_peak = null;
					// Old base peak was found
					if (old_base_peak != null)
					{
						// Cumulate only DETECTED peaks by hand 
						if (this.useOnlyDetectedPeaks)
						{
							double tic = 0.0;
							//ArrayList<ChromatographicPeak> TwinPeaks = new ArrayList<ChromatographicPeak>();
							for (final Feature p : detectedPeaks) {
								//ArrayList<Integer> peakScanNums = new ArrayList<Integer>();
								//								for (int s=0; s < p.getScanNumbers().length; ++s) {
								////									peakScanNums.add(p.getScanNumbers()[s]);
								//									if (s == scan.getScanNumber()) tic += p.getDataPoint(s)
								//								}
								//								if (peakScanNums.contains(new Integer(scan.getScanNumber()))) {
								//									//TwinPeaks.add(p);
								//								}

								DataPoint dp = p.getDataPoint(scan.getScanNumber());
								if (dp != null) tic += dp.getIntensity();
							}

							//							for (ChromatographicPeak p : TwinPeaks) {
							//								
							//							}

							//					        for (final DataPoint dataPoint : scan.getDataPointsByMass(scanCopy.getMZRange())) {
							//					        	if ()
							//					        		tic += dataPoint.getIntensity();
							//					        }
							new_base_peak = new SimpleDataPoint(old_base_peak.getMZ(), tic);
						}
						else
						{
							new_base_peak = new SimpleDataPoint(old_base_peak.getMZ(), ScanUtils.calculateTIC(scanCopy, scanCopy.getMZRange()));
						}
						// Reduce (erase) the Scan DataPoints to a single one, the one resulting from cumulating process
						// See "ScanUtils.calculateTIC()"...
						if (new_base_peak != null)
						{
							scanCopy.setDataPoints( new DataPoint[] { new_base_peak } );
							// Add new Scan to file writer
							rawDataFileWriter.addScan(scanCopy);
						}
					}

				}

				// Write down the data file
				RawDataFile tmpRawDataFile = rawDataFileWriter.finishWriting();
				MZmineCore.getCurrentProject().addFile(tmpRawDataFile);

				working_rdf = tmpRawDataFile;

			} catch (IOException e) {
				logger.log(Level.SEVERE, e.getMessage());
				e.printStackTrace();
			}		
		}

		// Such that never NULL is returned...
		return working_rdf;
	}

	// To be overridden in "CumulativePeakMergerTask"
	void initMergedPeakList() {
		// Populate with reference RDFs 
		// (just for display purpose)
		ArrayList<RawDataFile> l_rdfs = new ArrayList<RawDataFile>();
		l_rdfs.add(this.dataFile);
		if (this.ancestorDataFile != null && this.ancestorDataFile != this.dataFile) {
			l_rdfs.add(this.ancestorDataFile);
		}
		if (this.cumulativeComputing) {
			l_rdfs.add(this.workingDataFile); 
		}
		// RDFs list to array
		Object[] lst2arr = l_rdfs.toArray();
		RawDataFile[] rdfs = Arrays.copyOf(lst2arr, lst2arr.length, RawDataFile[].class);

		// Create PL
		this.mergedPeakList = new SimplePeakList(peakList + " " + suffix, rdfs);

		//		this.mergedPeakList = new SimplePeakList(peakList + " " + suffix, this.workingDataFile);
	}

	void updateMergedPeakList(PeakListRow oldRow, MergedPeak newPeak) {
		// Keep old ID
		int oldID = oldRow.getID();
		SimplePeakListRow newRow = new SimplePeakListRow(oldID);
		PeakUtils.copyPeakListRowProperties(oldRow, newRow);


		//		logger.log(Level.INFO, "oldRow: " + oldRow);
		//		logger.log(Level.INFO, "newRow: " + newRow);
		//		logger.log(Level.INFO, "newPeak: " + newPeak);
		//		logger.log(Level.INFO, "newPeak.getScanNumbers().length: " + newPeak.getScanNumbers().length);
		//		logger.log(Level.INFO, "newPeak.getRawDataPointsIntensityRange(): " + newPeak.getRawDataPointsIntensityRange());

		// Add peak to PLRow
		newRow.addPeak(this.workingDataFile, newPeak);
		// Add row to PL
		mergedPeakList.addRow(newRow);
	}

	void finishMergedPeakList() {
		// Add new peakList to the project
		MZmineProject currentProject = MZmineCore.getCurrentProject();
		currentProject.addPeakList(mergedPeakList);

		// Load previous applied methods
		for (PeakListAppliedMethod proc : peakList.getAppliedMethods()) {
			mergedPeakList.addDescriptionOfAppliedTask(proc);
		}

		// Add task description to peakList
		mergedPeakList.addDescriptionOfAppliedTask(
				new SimplePeakListAppliedMethod("RT peaks merger", parameters)
				);
	}


	public Object[] getCreatedObjects() {
		// Do we need to add the newly created RDF to the batch processing workflow ? 
		// (in my opinion, the answer should be: !No! ...)
		//		if (this.cumulativeComputing)
		//			return new Object[] { mergedPeakList, this.workingDataFile };
		//		else
		return new Object[] { mergedPeakList };
	}

}

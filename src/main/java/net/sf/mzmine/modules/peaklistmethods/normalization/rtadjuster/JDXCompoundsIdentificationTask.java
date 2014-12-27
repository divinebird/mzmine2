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

package net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.FormulaUtils;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;


// Dot
import org.apache.commons.math.linear.ArrayRealVector;
//Pearson
import org.apache.commons.math.stat.correlation.PearsonsCorrelation;



public class JDXCompoundsIdentificationTask extends AbstractTask {

	// Logger.
	private static final Logger LOG = Logger
			.getLogger(JDXCompoundsIdentificationTask.class.getName());

	// Minimum abundance.
	private static final double MIN_ABUNDANCE = 0.001;

	// Counters.
	private int finishedItems;
	private int numItems;

//	private final MZmineProcessingStep<OnlineDatabase> db;
//	private final MZTolerance mzTolerance;
//	private final int numOfResults;
	private final PeakList peakList;
//	private final boolean isotopeFilter;
//	private final ParameterSet isotopeFilterParameters;
//	private final IonizationType ionType;
//	private DBGateway gateway;

	private File jdxFileC1, jdxFileC2;
	private JDXCompound jdxComp1, jdxComp2;
	private Range rtSearchRangeC1, rtSearchRangeC2;
	private SimilarityMethodType simMethodType;
	private double mixFactor;
	
//	private HashMap<JDXCompound, Double> compoundsRowScores = new HashMap<JDXCompound, Double>();
	
	private PeakListRow currentRow;
	

	/**
	 * Create the identification task.
	 * 
	 * @param parameters
	 *            task parameters.
	 * @param list
	 *            peak list to operate on.
	 */
	@SuppressWarnings("unchecked")
	JDXCompoundsIdentificationTask(final ParameterSet parameters,
			final PeakList list) {

		peakList = list;
		numItems = 0;
		finishedItems = 0;
//		gateway = null;
		currentRow = null;

//		db = parameters
//				.getParameter(SingleRowIdentificationParameters.DATABASE)
//				.getValue();
//		mzTolerance = parameters.getParameter(
//				SingleRowIdentificationParameters.MZ_TOLERANCE).getValue();
//		numOfResults = parameters.getParameter(
//				SingleRowIdentificationParameters.MAX_RESULTS).getValue();
//		isotopeFilter = parameters.getParameter(
//				SingleRowIdentificationParameters.ISOTOPE_FILTER).getValue();
//		isotopeFilterParameters = parameters.getParameter(
//				SingleRowIdentificationParameters.ISOTOPE_FILTER)
//				.getEmbeddedParameters();
//		ionType = parameters.getParameter(
//				PeakListIdentificationParameters.ionizationType).getValue();
		
		jdxFileC1 = parameters.getParameter(JDXCompoundsIdentificationParameters.JDX_FILE_C1).getValue();
		jdxFileC2 = parameters.getParameter(JDXCompoundsIdentificationParameters.JDX_FILE_C2).getValue();
		rtSearchRangeC1 = parameters.getParameter(JDXCompoundsIdentificationParameters.RT_SEARCH_WINDOW_C1).getValue();
		rtSearchRangeC2 = parameters.getParameter(JDXCompoundsIdentificationParameters.RT_SEARCH_WINDOW_C2).getValue();
		simMethodType = parameters.getParameter(JDXCompoundsIdentificationParameters.SIMILARITY_METHOD).getValue();
		mixFactor = parameters.getParameter(JDXCompoundsIdentificationParameters.MIX_FACTOR).getValue();
		
		jdxComp1 = JDXCompound.parseJDXfile(jdxFileC1);
		jdxComp2 = JDXCompound.parseJDXfile(jdxFileC2);
		
		
		LOG.info("JDX parsing: " + jdxComp1);
	}

	@Override
	public double getFinishedPercentage() {

		return numItems == 0 ? 0.0 : (double) finishedItems / (double) numItems;
	}

	@Override
	public Object[] getCreatedObjects() {

		return null;
	}

	@Override
	public String getTaskDescription() {

		return "Identification of standard peaks in "
				+ peakList
				+ " using " + " standards '" + jdxComp1.getName() + "' and '" + jdxComp2.getName() + "'";
	}

	@Override
	public void run() {

		if (!isCanceled()) {
			try {

				setStatus(TaskStatus.PROCESSING);

//				// Create database gateway.
//				gateway = db.getModule().getGatewayClass().newInstance();

				// Identify the peak list rows starting from the biggest peaks.
				final PeakListRow[] rows = peakList.getRows();
				Arrays.sort(rows, new PeakListRowSorter(SortingProperty.Area, SortingDirection.Descending));

				// Initialize counters.
				numItems = rows.length;

				// Keep score for each std component for each peak/row of the current list.
				Double defaultScore = 0.0; //Double.MIN_VALUE;
				HashMap<JDXCompound, Double> compoundsRowScores; // = new HashMap<JDXCompound, Double>();
//				compoundsRowScores.put((JDXCompound)jdxComp1.clone(), defaultScore);
//				compoundsRowScores.put((JDXCompound)jdxComp2.clone(), defaultScore);
//				HashMap<PeakListRow, JDXCompound[]> mapRowScores = new HashMap<PeakListRow, JDXCompound[]>();
				HashMap<PeakListRow, HashMap<JDXCompound, Double>> mapRowScores = new HashMap<PeakListRow, HashMap<JDXCompound, Double>>();
				
				// Process rows.
				for (finishedItems = 0; !isCanceled() && finishedItems < numItems; finishedItems++) {

					PeakListRow a_row = rows[finishedItems];
					
//					compoundsRowScores = new HashMap<JDXCompound, Double>();
//					compoundsRowScores.put((JDXCompound)jdxComp1.clone(), defaultScore);
//					compoundsRowScores.put((JDXCompound)jdxComp2.clone(), defaultScore);
//					// Retrieve results for each row.
//					mapRowScores.put(a_row, compoundsRowScores);
//					for (JDXCompound stdComp : compoundsRowScores.keySet()) {
//						LOG.info("Looking for compound '" + stdComp.getName() + "' in row '" + a_row.getID() + "'.");
//						//retrieveIdentification(a_row);
//						compoundsRowScores.put(stdComp, computeCompoundRowScore(a_row, stdComp));
//					}
					final JDXCompound[] findCompounds = { (JDXCompound) jdxComp1.clone(), (JDXCompound) jdxComp2.clone() };

					// Retrieve results for each row.
					mapRowScores.put(a_row, computeCompoundsRowScore(a_row, findCompounds));
				}

				if (!isCanceled()) {
					
					// Build temp scores table.
					
					
					// Show up table.
					
					setStatus(TaskStatus.FINISHED);
				}
			} catch (Throwable t) {

				final String msg = "Could not search standard compounds for list '" + peakList.getName() + "'.";
				LOG.log(Level.WARNING, msg, t);
				setStatus(TaskStatus.ERROR);
				errorMessage = msg + ": " + ExceptionUtils.exceptionToString(t);
			}
		}
	}

	private HashMap<JDXCompound, Double> computeCompoundsRowScore(final PeakListRow row, JDXCompound[] findCompounds)
			throws IOException {

		HashMap<JDXCompound, Double> scoresMap = new HashMap<JDXCompound, Double>();
		
		currentRow = row;

		// Process each one of the result ID's.
//		final String[] findCompounds = gateway.findCompounds(massValue,
//		mzTolerance, numOfResults, db.getParameterSet());
		for (int i = 0; !isCanceled() && i < findCompounds.length; i++) {

//			final DBCompound compound = gateway.getCompound(findCompounds[i],
//			db.getParameterSet());
			
			final JDXCompound compound = findCompounds[i];
			
			// Get scan of interest.
			Scan apexScan = row.getBestPeak().getDataFile().getScan(row.getBestPeak().getRepresentativeScanNumber());
			// Get scan m/z vector.
	        double[] vec1 = new double[JDXCompound.MAX_MZ];
	        Arrays.fill(vec1, 0.0);
			DataPoint[] dataPoints = apexScan.getDataPoints();
	        for (int j=0; j < dataPoints.length; ++j) {
	        	DataPoint dp = dataPoints[j];
	        	vec1[(int) Math.round(dp.getMZ())] = dp.getIntensity();
	        }
			// Get std compound vector
			double[] vec2 = compound.getSpectrum();
			// Get similarity score.
			scoresMap.put(compound, computeSimilarityScore(vec1, vec2));
			
//			final String formula = compound
//					.getPropertyValue(PeakIdentity.PROPERTY_FORMULA);
//
//			// If required, check isotope score.
//			if (isotopeFilter && rowIsotopePattern != null && formula != null) {
//
//				// First modify the formula according to ionization.
//				final String adjustedFormula = FormulaUtils.ionizeFormula(
//						formula, ionType, charge);
//
//				LOG.finest("Calculating isotope pattern for compound formula "
//						+ formula + " adjusted to " + adjustedFormula);
//
//				// Generate IsotopePattern for this compound
//				final IsotopePattern compoundIsotopePattern = IsotopePatternCalculator
//						.calculateIsotopePattern(adjustedFormula,
//								MIN_ABUNDANCE, charge, ionType.getPolarity());
//
//				// Check isotope pattern match
//				boolean check = IsotopePatternScoreCalculator.checkMatch(
//						rowIsotopePattern, compoundIsotopePattern,
//						isotopeFilterParameters);
//
//				if (!check)
//					continue;
//			}

			// Add the retrieved identity to the peak list row
			row.addPeakIdentity(compound, false);

			// Notify the GUI about the change in the project
			MZmineCore.getCurrentProject().notifyObjectChanged(row, false);
			MZmineCore.getDesktop().getMainWindow().repaint();
		}
		
		return scoresMap;
	}

	private double computeSimilarityScore(double[] vec1, double[] vec2) {
		
		double simScore = 0.0;
		
		try {
			if (this.simMethodType == SimilarityMethodType.DOT) {
				simScore = new ArrayRealVector(vec1).dotProduct(vec2);
			} else if (this.simMethodType == SimilarityMethodType.PEARSON) {
				simScore = new PearsonsCorrelation().correlation(vec1, vec2);
			}
		} catch (IllegalArgumentException e ) {
			LOG.severe("Failed to compute similarity score for vec1.length=" + vec1.length + " and vec2.length=" + vec2.length);
		} 
		
		return simScore;
	}
	
	
	/**
	 * Search the database for the peak's identity.
	 * 
	 * @param row
	 *            the peak list row.
	 * @throws IOException
	 *             if there are i/o problems.
	 */
	private void retrieveIdentification(final PeakListRow row)
			throws IOException {

		currentRow = row;

//		// Determine peak charge.
//		final Feature bestPeak = row.getBestPeak();
//		int charge = bestPeak.getCharge();
//		if (charge <= 0) {
//			charge = 1;
//		}
//
//		// Calculate mass value.
//		final double massValue = (row.getAverageMZ() - ionType.getAddedMass())
//				* (double) charge;
//
//		// Isotope pattern.
//		final IsotopePattern rowIsotopePattern = bestPeak.getIsotopePattern();

		// Process each one of the result ID's.
//		final String[] findCompounds = gateway.findCompounds(massValue,
//		mzTolerance, numOfResults, db.getParameterSet());
		final JDXCompound[] findCompounds = { jdxComp1, jdxComp2 };
		for (int i = 0; !isCanceled() && i < findCompounds.length; i++) {

//			final DBCompound compound = gateway.getCompound(findCompounds[i],
//			db.getParameterSet());
			final JDXCompound compound = findCompounds[i];
			
//			final String formula = compound
//					.getPropertyValue(PeakIdentity.PROPERTY_FORMULA);
//
//			// If required, check isotope score.
//			if (isotopeFilter && rowIsotopePattern != null && formula != null) {
//
//				// First modify the formula according to ionization.
//				final String adjustedFormula = FormulaUtils.ionizeFormula(
//						formula, ionType, charge);
//
//				LOG.finest("Calculating isotope pattern for compound formula "
//						+ formula + " adjusted to " + adjustedFormula);
//
//				// Generate IsotopePattern for this compound
//				final IsotopePattern compoundIsotopePattern = IsotopePatternCalculator
//						.calculateIsotopePattern(adjustedFormula,
//								MIN_ABUNDANCE, charge, ionType.getPolarity());
//
//				// Check isotope pattern match
//				boolean check = IsotopePatternScoreCalculator.checkMatch(
//						rowIsotopePattern, compoundIsotopePattern,
//						isotopeFilterParameters);
//
//				if (!check)
//					continue;
//			}

			// Add the retrieved identity to the peak list row
			row.addPeakIdentity(compound, false);

			// Notify the GUI about the change in the project
			MZmineCore.getCurrentProject().notifyObjectChanged(row, false);
			MZmineCore.getDesktop().getMainWindow().repaint();
		}
	}
}

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultCellEditor;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.modules.peaklistmethods.identification.dbsearch.ResultWindow;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.CollectionUtils;
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



public class JDXCompoundsIdentificationSingleTask extends AbstractTask {

	// Logger.
	private static final Logger LOG = Logger
			.getLogger(JDXCompoundsIdentificationSingleTask.class.getName());

	// Minimum abundance.
	private static final double MIN_ABUNDANCE = 0.001;

	// Counters.
	private int finishedItemsTotal;
	private int finishedItems;
	private int numItems;

//	private final MZmineProcessingStep<OnlineDatabase> db;
//	private final MZTolerance mzTolerance;
//	private final int numOfResults;
	private final PeakList[] peakLists;
//	private final boolean isotopeFilter;
//	private final ParameterSet isotopeFilterParameters;
//	private final IonizationType ionType;
//	private DBGateway gateway;

	private File jdxFileC1, jdxFileC2;
	private JDXCompound jdxComp1, jdxComp2;
	private Range rtSearchRangeC1, rtSearchRangeC2;
	private SimilarityMethodType simMethodType;
	private double areaMixFactor;
	
//	private HashMap<JDXCompound, Double> compoundsRowScores = new HashMap<JDXCompound, Double>();
	
	private PeakListRow currentRow;
	private PeakList currentPeakList;
	

	/**
	 * Create the identification task.
	 * 
	 * @param parameters
	 *            task parameters.
	 * @param list
	 *            peak list to operate on.
	 */
	@SuppressWarnings("unchecked")
	JDXCompoundsIdentificationSingleTask(final ParameterSet parameters, final PeakList[] lists) {

		peakLists = lists;
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
		areaMixFactor = parameters.getParameter(JDXCompoundsIdentificationParameters.MIX_FACTOR).getValue();
		
		jdxComp1 = JDXCompound.parseJDXfile(jdxFileC1);
		jdxComp2 = JDXCompound.parseJDXfile(jdxFileC2);
		
		
		LOG.info("JDX parsing: " + jdxComp1);
	}

	@Override
	public double getFinishedPercentage() {

		//return numItems == 0 ? 0.0 : (double) finishedItems / (double) numItems;
		return numItems == 0 ? 0.0 : (double) finishedItemsTotal / (double) (finishedItems * numItems * peakLists.length);
	}

	@Override
	public Object[] getCreatedObjects() {

		return null;
	}

	@Override
	public String getTaskDescription() {

		return "Identification of standard peaks in "
				+ "selected peak lists"
				+ " using " + " standards '" + jdxComp1.getName() + "' and '" + jdxComp2.getName() + "'";
	}

	@Override
	public void run() {

		if (!isCanceled()) {
			PeakList curPeakList = null;
			RawDataFile curRefRDF = null;
			try {

				setStatus(TaskStatus.PROCESSING);
				finishedItemsTotal = 0;

				final JDXCompound[] findCompounds = { (JDXCompound) jdxComp1/*.clone()*/, (JDXCompound) jdxComp2/*.clone()*/ };
				final Range[] findRTranges = { rtSearchRangeC1, rtSearchRangeC2 };
			    String[] columnNames = { "Data file", jdxComp1.getName(), jdxComp1.getName() + " score", jdxComp2.getName(), jdxComp2.getName() + " score" };
			    ScoresResultWindow window = new ScoresResultWindow(columnNames, this);
			    //window.setColumnNames(columnNames);
			    window.setVisible(true);
			    
			    
//				// Create database gateway.
//				gateway = db.getModule().getGatewayClass().newInstance();
				
//				ArrayList<LinkedHashMap<PeakListRow, ArrayList<JDXCompound>>> piafsRowScores = 
//						new ArrayList<LinkedHashMap<PeakListRow, ArrayList<JDXCompound>>>();
								
				for (final PeakList peakList : peakLists) {
					
			        // Update window title.
					window.setTitle(peakList.getName() + ": Searching for compounds '" + jdxComp1.getName() + "' and '" + jdxComp2.getName() + "'");

			        curPeakList = peakList;
			        curRefRDF = curPeakList.getRawDataFile(0);
					
					// Identify the peak list rows starting from the biggest peaks.
					final PeakListRow[] rows = peakList.getRows();
					//**Arrays.sort(rows, new PeakListRowSorter(SortingProperty.Area, SortingDirection.Descending));
					Arrays.sort(rows, new PeakListRowSorter(SortingProperty.ID, SortingDirection.Ascending));
	
					// Initialize counters.
					numItems = rows.length;
	
//					// Keep score for each std component for each peak/row of the current list.
//					Double defaultScore = 0.0; //Double.MIN_VALUE;
//					LinkedHashMap<JDXCompound, Double> compoundsRowScores; // = new HashMap<JDXCompound, Double>();
//	//				compoundsRowScores.put((JDXCompound)jdxComp1.clone(), defaultScore);
//	//				compoundsRowScores.put((JDXCompound)jdxComp2.clone(), defaultScore);
//	//				HashMap<PeakListRow, JDXCompound[]> mapRowScores = new HashMap<PeakListRow, JDXCompound[]>();
//					LinkedHashMap<PeakListRow, ArrayList<JDXCompound>> mapRowScores = 
//							new LinkedHashMap<PeakListRow, ArrayList<JDXCompound>>();
//					
//					// Process rows.
//					for (finishedItems = 0; !isCanceled() && finishedItems < numItems; finishedItems++) {
//	
//						PeakListRow a_row = rows[finishedItems];
//						
//	//					compoundsRowScores = new HashMap<JDXCompound, Double>();
//	//					compoundsRowScores.put((JDXCompound)jdxComp1.clone(), defaultScore);
//	//					compoundsRowScores.put((JDXCompound)jdxComp2.clone(), defaultScore);
//	//					// Retrieve results for each row.
//	//					mapRowScores.put(a_row, compoundsRowScores);
//	//					for (JDXCompound stdComp : compoundsRowScores.keySet()) {
//	//						LOG.info("Looking for compound '" + stdComp.getName() + "' in row '" + a_row.getID() + "'.");
//	//						//retrieveIdentification(a_row);
//	//						compoundsRowScores.put(stdComp, computeCompoundRowScore(a_row, stdComp));
//	//					}
//						// TODO: make 'clone()' work...
//						final JDXCompound[] findCompounds = { (JDXCompound) jdxComp1/*.clone()*/, (JDXCompound) jdxComp2/*.clone()*/ };
//	
//						// Retrieve results for each row.
//						mapRowScores.put(a_row, computeCompoundsRowScore(a_row, findCompounds));
//						
//						finishedItemsTotal++;
//					}
//					piafsRowScores.add(mapRowScores);
//					
//	                // Add piaf to the list and display it in window of results.
//					LOG.info("Add table row: " + ((PeakListRow)piafsRowScores.get(piafsRowScores.size()-1).keySet().toArray()[0]).getBestPeak().getDataFile().getName());
//	                window.addNewListItem(mapRowScores);

					///---------------------------------------------------------------------------------------
					
					// Keep score for each std component for each peak/row of the current list.
//					Double defaultScore = 0.0; //Double.MIN_VALUE;
//					LinkedHashMap<PeakListRow, ArrayList<JDXCompound>> mapRowScores = 
//							new LinkedHashMap<PeakListRow, ArrayList<JDXCompound>>();
					Double[][] scoreMatrix = new Double[numItems][1+findCompounds.length]; // +1: Store row id first
					
					// Process rows.
					for (finishedItems = 0; !isCanceled() && finishedItems < numItems; finishedItems++) {
	
						PeakListRow a_row = rows[finishedItems];
	
						// Retrieve results for each row.
						//mapRowScores.put(a_row, computeCompoundsRowScore(a_row, findCompounds));
						scoreMatrix[finishedItems][0] = (double) finishedItems;
						for (int i = 0; !isCanceled() && i < findCompounds.length; i++) {
							if (findRTranges[i].contains(a_row.getBestPeak().getRT()))
								scoreMatrix[finishedItems][i+1] = computeCompoundRowScore(curRefRDF.getAncestorDataFile(false), curPeakList, a_row, findCompounds[i]);
							else
								// Out of range.
								scoreMatrix[finishedItems][i+1] = 0.0;
						}
						
						finishedItemsTotal++;
					}
//					piafsRowScores.add(mapRowScores);
					
	                // Add piaf to the list and display it in window of results.
					//LOG.info("Add table row: " + ((PeakListRow)piafsRowScores.get(piafsRowScores.size()-1).keySet().toArray()[0]).getBestPeak().getDataFile().getName());
	                //window.addNewListItem(mapRowScores);
	                window.addNewListItem(peakList, findCompounds, scoreMatrix);
	                
//                	printMatrixToFile(scoreMatrix, "scores_" + (simMethodType == SimilarityMethodType.DOT ? "dot" : "pearson") + "_" + peakList.getName() + ".txt");
//	                for (int i = 0; !isCanceled() && i < findCompounds.length; i++) {
//	                	Double[][] mtx_sorted = new Double[scoreMatrix.length][scoreMatrix[0].length]; //scoreMatrix;
//	    				arrayCopy(scoreMatrix, mtx_sorted);
//	                	Arrays.sort(mtx_sorted, new ScoresResultTableModel.ArrayComparator(i+1, false));
//	                	printMatrixToFile(mtx_sorted, "scores_" + (simMethodType == SimilarityMethodType.DOT ? "dot" : "pearson") + "_" + i + "_" + peakList.getName() + ".txt");
//	                }
				}
				
				if (!isCanceled()) {
					
					// Build temp scores table.
//					ScoresResultWindow srw = new ScoresResultWindow(piafsRowScores, null, 0.0, this);
//					for (int i=0; i < piafsRowScores.size(); i++) {
//						srw.addNewListItem(piafsRowScores.get(i));						
//					}
//					// Show up table.
//					srw.setVisible(true);
					
					setStatus(TaskStatus.FINISHED);
				}
			} catch (Throwable t) {
				t.printStackTrace();
				final String msg = "Could not search standard compounds for list '" + curPeakList.getName() + "'.";
				LOG.log(Level.WARNING, msg, t);
				setStatus(TaskStatus.ERROR);
				errorMessage = msg + ": " + ExceptionUtils.exceptionToString(t);
			}
		}
	}

	private double computeCompoundRowScore(final RawDataFile refRDF, final PeakList curPeakList, final PeakListRow row, final JDXCompound compound)
			throws IOException {

		//ArrayList<JDXCompound> scoresList = new ArrayList<JDXCompound>();
		double score = 0.0;
		
		currentRow = row;
		currentPeakList = curPeakList;

		

		// Process each one of the result ID's.
//		final String[] findCompounds = gateway.findCompounds(massValue,
//		mzTolerance, numOfResults, db.getParameterSet());
//		for (int i = 0; !isCanceled() && i < findCompounds.length; i++) {

//			final DBCompound compound = gateway.getCompound(findCompounds[i],
//			db.getParameterSet());
			
//			final JDXCompound compound = findCompounds[i];
			
			// Get scan of interest.
			Scan apexScan = refRDF.getScan(row.getBestPeak().getRepresentativeScanNumber());
			LOG.info(refRDF + " | Computing score for row of id: " + row.getID());
			LOG.info("Scan: " + apexScan);
			
			int minMZ, maxMZ;
			minMZ = 38;
			maxMZ = compound.getMaxMZ();
			
			// Get scan m/z vector.
	        double[] vec1 = new double[JDXCompound.MAX_MZ];
	        Arrays.fill(vec1, 0.0);
	        //LOG.info("DPs MZ Range: " + apexScan.getMZRange());
			DataPoint[] dataPoints = apexScan.getDataPoints();
	        for (int j=0; j < dataPoints.length; ++j) {
	        	DataPoint dp = dataPoints[j];
	        	vec1[(int) Math.round(dp.getMZ())] = dp.getIntensity();
	        }
			// Get std compound vector
			double[] vec2 = compound.getSpectrum();
			// Get similarity score.
			JDXCompound newComp = (JDXCompound) compound.clone();
			
//			int compoundPertinentSignalLength = compound.getMaxMZ() - compound.getMinMZ() + 1 - minMZ + compound.getMinMZ();
//			double[] vec1_crop = new double[compoundPertinentSignalLength];
//			double[] vec2_crop = new double[compoundPertinentSignalLength];
//			LOG.info("Before crop1: " + vec1.length + " | " + Arrays.toString(vec1));
//			LOG.info("Before crop2: " + vec2.length + " | " + Arrays.toString(vec2));
//			System.arraycopy(vec1, minMZ, vec1_crop, 0, compoundPertinentSignalLength);//new double[vec1.length];
//			System.arraycopy(vec2, minMZ, vec2_crop, 0, compoundPertinentSignalLength);//new double[vec1.length];
//			LOG.info("After crop1: " + vec1_crop.length + " | " + Arrays.toString(vec1_crop));
//			LOG.info("After crop2: " + vec2_crop.length + " | " + Arrays.toString(vec2_crop));
//			score = computeSimilarityScore(vec1_crop, vec2_crop);	
			score = computeSimilarityScore(vec1, vec2);	

			// Adjust taking area in account (or not: areaMixFactor = 0.0).
			// TODO: Check if the following is pertinent with Pearson's correlation method
			//			(where scores are not normalized and can be negative).
			if (areaMixFactor > 0.0) {
				double maxArea = Double.MIN_VALUE;
				for (PeakListRow plr : currentPeakList.getRows()) {
					if (plr.getBestPeak().getArea() > maxArea) { maxArea = plr.getBestPeak().getArea(); }
				}
				score = (1.0 - areaMixFactor) * score + (areaMixFactor) * row.getBestPeak().getArea() / maxArea;
			}

			
			LOG.info("Score: " + score);
			
			newComp.setBestScore(score);
//			scoresList.add(newComp);
			
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

//			// Add the retrieved identity to the peak list row, if applicable.
//			if (!CollectionUtils.arrayContains(row.getPeakIdentities(), newComp)) {
//				row.addPeakIdentity(newComp, false);
//			}
			

//			// Notify the GUI about the change in the project
//			MZmineCore.getCurrentProject().notifyObjectChanged(row, false);
//			MZmineCore.getDesktop().getMainWindow().repaint();
//		}
		
		return score;
	}

	private double computeSimilarityScore(double[] vec1, double[] vec2) {
		
		double simScore = 0.0;
		
		try {
			
			if (this.simMethodType == SimilarityMethodType.DOT) {
				double[] vec1_norm = new double[vec1.length];
				double[] vec2_norm = new double[vec2.length];
				
				double div1 = 0.0, div2 = 0.0;
				for (int i=0; i < vec1.length; ++i) {
					div1 += vec1[i] * vec1[i];
					div2 += vec2[i] * vec2[i];
				}
				for (int i=0; i < vec1.length; ++i) {
					vec1_norm[i] = vec1[i] / Math.sqrt(div1);
					vec2_norm[i] = vec2[i] / Math.sqrt(div2);
				}
				simScore = (new ArrayRealVector(vec1_norm)).dotProduct(vec2_norm);
			} else if (this.simMethodType == SimilarityMethodType.PEARSON) {
				simScore = new PearsonsCorrelation().correlation(vec1, vec2);
			}
		} catch (IllegalArgumentException e ) {
			LOG.severe("Failed to compute similarity score for vec1.length=" + vec1.length + " and vec2.length=" + vec2.length);
		} 
		
		return simScore;
	}
	
	private void printMatrixToFile(Object[][] mtx, String filename) {
		PrintWriter writer;
		try {
			writer = new PrintWriter(filename, "UTF-8");
			for (int i=0; i < mtx.length; ++i) {
				for (int j=0; j < mtx[i].length; ++j) {
					writer.print(mtx[i][j] + " ");
				}
				writer.println();
			}
			writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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

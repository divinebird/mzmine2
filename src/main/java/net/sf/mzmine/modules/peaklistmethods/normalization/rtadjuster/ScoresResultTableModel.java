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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.ScoresResultTableModel.ComboboxPeak;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.FormulaUtils;
import net.sf.mzmine.util.PeakListRowSorter;

public class ScoresResultTableModel extends DefaultTableModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private boolean DEBUG = true;
	// Logger.
	private static final Logger LOG = Logger.getLogger(ScoresResultTableModel.class.getName());

	//	static class MyComboBoxRenderer extends JComboBox implements TableCellRenderer {
	//		public MyComboBoxRenderer(String[] items) {
	//			super(items);
	//		}
	//
	//		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
	//				boolean hasFocus, int row, int column) {
	//			if (isSelected) {
	//				setForeground(table.getSelectionForeground());
	//				super.setBackground(table.getSelectionBackground());
	//			} else {
	//				setForeground(table.getForeground());
	//				setBackground(table.getBackground());
	//			}
	//			setSelectedItem(value);
	//			return this;
	//		}
	//	}
	//
	//	static class MyComboBoxEditor extends DefaultCellEditor {
	//		public MyComboBoxEditor(String[] items) {
	//			super(new JComboBox(items));
	//		}
	//	}


	//
	private static String[] columnNames = null; //{ "Piaf", "Compound 1", "Score C1", "Compound 2", "Score C2" };
	//
	//	private double searchedMass;
	//	//private Vector<JDXCompound> compounds = new Vector<JDXCompound>();
	//	private ArrayList<LinkedHashMap<PeakListRow, ArrayList<JDXCompound>>> piafsRowScores = 
	//			new ArrayList<LinkedHashMap<PeakListRow, ArrayList<JDXCompound>>>();
//	private ArrayList<Piaf> piafsList = new ArrayList<Piaf>();
	//**private ArrayList<Vector<Object>> data = new ArrayList<Vector<Object>>();

	//
	//	final NumberFormat percentFormat = NumberFormat.getPercentInstance();
	//	final NumberFormat massFormat = MZmineCore.getConfiguration().getMZFormat();

	ScoresResultTableModel(String[] columnNames, int rowCount) {
		super(columnNames, rowCount);
		ScoresResultTableModel.columnNames = columnNames;
	}


	//	ScoresResultTableModel(String[] columnNames, DefaultCellEditor[] cellEditors) {
	//		
	//		if (columnNames.length != cellEditors.length)
	//			throw new IllegalStateException("columnNames.length != cellEditors.length...");
	//		
	//		for (int i=0; i < columnNames.length; ++i) {
	//			
	//		}
	//		
	//		//this.piafsRowScores = piafsRowScores;
	//	}

	//	public void setColumnName(int i, String[] names) {
	//	    columnNames[i] = name;
	//	    fireTableStructureChanged();
	//	}

	public String getColumnName(int col) {
		return columnNames[col];
	}

	public void setColumnNames(String[] colNames) {
		columnNames = colNames;
		fireTableStructureChanged();
	}

	//	public int getRowCount() {
	//		//return compounds.size();
	//		//return piafsRowScores.size();
	//		return piafsList.size();
	//	}

	public int getColumnCount() {
		return columnNames.length;
	}

	//	public Object getValueAt(int row, int col) {
	//
	//		Object value = null;
	//		//JDXCompound comp = compounds.get(row);
	//		//PeakListRow plr = (PeakListRow)piafsRowScores.get(row).keySet().toArray()[0];
	//
	//		switch (col) {
	//		case (0):
	//			//value = plr.getBestPeak().getDataFile().getName();
	//			value = piafsList.get(row).peakList.getRawDataFiles()[piafsList.get(row).peakList.getRawDataFiles().length-1].getName();
	//		break;
	//		//		case (1):
	//		//			
	//		//			
	//		//			
	//		//			value = comp.getName();
	//		//		break;
	//		//		case (2):
	//		//			value = comp.getPropertyValue(PeakIdentity.PROPERTY_FORMULA);
	//		//		break;
	//		//		case (3):
	//		//			String compFormula = comp
	//		//			.getPropertyValue(PeakIdentity.PROPERTY_FORMULA);
	//		//		if (compFormula != null) {
	//		//			double compMass = FormulaUtils.calculateExactMass(compFormula);
	//		//			double massDifference = Math.abs(searchedMass - compMass);
	//		//			value = massFormat.format(massDifference);
	//		//		}
	//		//		break;
	//		//		case (4):
	//		//			Double score = comp.getBestScore();
	//		//		if (score != null)
	//		//			value = percentFormat.format(score);
	//		//		break;
	//		default:
	//			value = "Snowboarding";
	//			break;
	//		}
	//
	//		LOG.info("Set value at: " + value);
	//		return value;
	//	}
	public Object getValueAt(int row, int col)
	{
		if (getDataVector().size() == 0) return "Empty";

		if (col==0)
			//return piafsList.get(row).peakList.getRawDataFiles()[piafsList.get(row).peakList.getRawDataFiles().length-1].getName();
			return ((PeakList)super.getValueAt(row,col)).getRawDataFile(0).getAncestorDataFile(false).getName();
		
		if (super.getValueAt(row,col) instanceof JComboBox)
			return ((JComboBox<?>) super.getValueAt(row,col)).getSelectedItem();

		return super.getValueAt(row,col);
	}

	/*
	 * Don't need to implement this method unless your table's
	 * data can change.
	 */
	public void setValueAt(Object value, int row, int col) {
		
		//if (row >= data.size()) return;
		
		if (col == 0) return;
		
		LOG.info("setValueAt: " + value + " | " + row + "," + col);
//		if (value instanceof Vector) {
//			data.set(row, (Vector<Object>) value);
//		}
//		if (value instanceof Double) {
//			data.get(row).set(col, (Object) value);
//		}
//		//fireTableCellUpdated(row, col);
		
//		if (value instanceof ComboboxPeak) {
//			super.setValueAt(value, row, col);
//			super.setValueAt(((ComboboxPeak) value).getScore(), row, col+1);
//		}
		if (value instanceof ComboboxPeak) {
			//super.setValueAt(value, row, col);
			super.setValueAt(((ComboboxPeak) value).getScore(), row, col+1);
		}
		
//		else {
//			super.setValueAt(value, row, col+1);
//		}
		//fireTableCellUpdated(row, col+1);
	}

	public Vector getData() {
		return getDataVector();
	}

	/*
	 * JTable uses this method to determine the default renderer/
	 * editor for each cell.  If we didn't implement this method,
	 * then the last column would contain text ("true"/"false"),
	 * rather than a check box.
	 */
	public Class getColumnClass(int c) {
		return getValueAt(0, c).getClass();
	}


	public JDXCompound getCompoundAt(int row) {
		return null; //compounds.get(row);
	}

	public boolean isCellEditable(int row, int col) {
		if (col == 1 || col == 3) return true;
		else return false;
//		return true;
	}

	//	public void addElement(JDXCompound compound) {
	//		compounds.add(compound);
	//		fireTableRowsInserted(compounds.size() - 1, compounds.size() - 1);
	//	}

	//	enum PeakListRowComparator implements Comparator<PeakListRow> {
	//	    ID_SORT {
	//	        public int compare(PeakListRow o1, PeakListRow o2) {
	//	            return Integer.valueOf(o1.getId()).compareTo(o2.getId());
	//	        }},
	//	    NAME_SORT {
	//	        public int compare(PeakListRow o1, PeakListRow o2) {
	//	            return o1.getFullName().compareTo(o2.getFullName());
	//	        }};
	//
	//	    public static Comparator<Person> decending(final Comparator<Person> other) {
	//	        return new Comparator<Person>() {
	//	            public int compare(Person o1, Person o2) {
	//	                return -1 * other.compare(o1, o2);
	//	            }
	//	        };
	//	    }
	//
	//	    public static Comparator<Person> getComparator(final PersonComparator... multipleOptions) {
	//	        return new Comparator<Person>() {
	//	            public int compare(Person o1, Person o2) {
	//	                for (PersonComparator option : multipleOptions) {
	//	                    int result = option.compare(o1, o2);
	//	                    if (result != 0) {
	//	                        return result;
	//	                    }
	//	                }
	//	                return 0;
	//	            }
	//	        };
	//	    }
	//	}

	//	class PeakListRowComparator2 implements Comparator<PeakListRow> {
	//
	//		int compoundId = 0;
	//		JDXCompound compound;
	//		
	//		public PeakListRowComparator2(JDXCompound compound) {
	//			//this.compoundId = compoundId;
	//			this.compound = compound;
	//		}
	//		
	//		@Override
	//		public int compare(PeakListRow r1, PeakListRow r2) {
	//			double score1 = ;
	//			return Double.valueOf(o1.getId()).compareTo(o2.getId());
	//		}
	//	}

	static class ArrayComparator implements Comparator<Double[]> {
		private final int columnToSort;
		private final boolean ascending;

		public ArrayComparator(int columnToSort, boolean ascending) {
			this.columnToSort = columnToSort;
			this.ascending = ascending;
		}

		@Override
		public int compare(Double[] d1, Double[] d2) {
			int cmp = d1[columnToSort].compareTo(d2[columnToSort]);
			return ascending ? cmp : -cmp;
		}
	}

//	public class Piaf {
//
//		public final PeakList peakList;
//		public final JDXCompound[] findCompounds;
//		public final Double[][] scoreMatrix;
//
//		public Piaf(final PeakList peakList, final JDXCompound[] findCompounds, final Double[][] scoreMatrix) {
//			this.peakList = peakList;
//			this.findCompounds = findCompounds;
//			this.scoreMatrix = scoreMatrix;
//		}
//	}

	public class ComboboxPeak extends SimpleFeature {

		//private Feature peak;
		private PeakList peakList;
		private PeakListRow peakListRow;
		private JDXCompound jdxComp;
		private double score;

		public ComboboxPeak(Feature peak, PeakList peakList, PeakListRow peakListRow, JDXCompound jdxComp, double score) {
			super(peak);
			//this.peak = peak;
			this.peakList = peakList;
			this.peakListRow = peakListRow;
			this.jdxComp = jdxComp;
			this.score = score;
			
			//**this.peakListRow.setPreferredPeakIdentity(jdxComp);
		}

		public PeakList getPeakList() {
			return peakList;
		}
		public PeakListRow getPeakListRow() {
			return peakListRow;
		}
		public JDXCompound getJDXCompound() {
			return jdxComp;
		}
		public double getScore() {
			return score;
		}
		public int getRowID() {
			return peakListRow.getID();
		}
		
		@Override
		public String toString() {
			return "Peak " + this.peakListRow.getID() + " @" + this.getRT() + " #" + this.score;
		}

	}

	
	public void addElement(final JTableXY piafsScoresTable, final PeakList peakList, final JDXCompound[] findCompounds, final Double[][] scoreMatrix) {

		if (columnNames == null) return;

//		//piafsRowScores.add(element);
//		Piaf piaf = new Piaf(peakList, findCompounds, scoreMatrix);
//		piafsList.add(piaf);
//
//		int row = piafsList.size() - 1;
		final int row = getDataVector().size();//**data.size();

		//		SortedSet<Map.Entry<String, Double>> sortedset = new TreeSet<Map.Entry<String, Double>>(
		//				new Comparator<Map.Entry<String, Double>>() {
		//					@Override
		//					public int compare(Map.Entry<String, Double> e1,
		//							Map.Entry<String, Double> e2) {
		//						return e1.getValue().compareTo(e2.getValue());
		//					}
		//				});
		//		sortedset.addAll(myMap.entrySet());

		// Set up the editor for the combo cells.
		// Get compounds
		//ArrayList<JDXCompound> compounds = element.entrySet().iterator().next().getValue();
		//int step = 1;
		// Sort PeakListRows by score for compound i
		Vector<Object> objects = new Vector<Object>(/*columnNames.length*/);
		//objects.setElementAt(piafsList.get(row).peakList.getRawDataFiles()[piafsList.get(row).peakList.getRawDataFiles().length-1].getName(), 0);
		//objects.add(piafsList.get(row).peakList.getRawDataFiles()[piafsList.get(row).peakList.getRawDataFiles().length-1].getName());
//		objects.add(piafsList.get(row).peakList.getName());
		objects.add(peakList/*.getName()*/);
		for (int i=0; i < findCompounds.length; ++i) {

			final int col = objects.size();

			// Sort matrix for compound i
			Double[][] mtx = new Double[scoreMatrix.length][scoreMatrix[0].length]; //scoreMatrix;
			CollectionUtils.matrixCopy(scoreMatrix, mtx);
			Arrays.sort(mtx, new ArrayComparator(i+1, false)); // +1: skip first column (row number)

			// Build related Combobox
			JComboBox<ComboboxPeak> comboBox = new JComboBox<ComboboxPeak>();
			for (int j=0; j < mtx.length; ++j) {
				if (j < 20) LOG.info("Row mtx at " + (i+1) + ": " + mtx[j][0] + ", " + mtx[j][1] + ", " + mtx[j][2]);
				PeakListRow bestRow = peakList.getRow((int) Math.round(mtx[j][0]));
				// Keep only scoring rows.
				//if (mtx[j][i+1] > 0.0)
				comboBox.addItem(new ComboboxPeak(bestRow.getBestPeak(), peakList, bestRow, findCompounds[i], mtx[j][i+1]));
			}
			//piafsScoresTable.getColumnModel().getColumn(i+step).setCellEditor(new DefaultCellEditor(comboBox));
			CellEditorModel cm = piafsScoresTable.getCellEditorModel();
			DefaultCellEditor ed = new DefaultCellEditor(comboBox);
			cm.addEditorForCell(row, col, ed);
			//			objects.setElementAt(comboBox, i+step);
			//			objects.setElementAt(data[0][i+1], i+1+step);
			LOG.info("Added combobox at: tcol=" + objects.size() + " | cb size= " + comboBox.getItemCount() + " | mtx size= " + mtx.length);
			
//			comboBox.addActionListener(
//	                new ActionListener(){
//	                    public void actionPerformed(ActionEvent e){
//	                        JComboBox<?> combo = (JComboBox<?>)e.getSource();
//	                        setValueAt(((ComboboxPeak)combo.getSelectedItem()).getScore(), row, col+1);
//	                    }
//	                }            
//	        );

			objects.add(comboBox);
			objects.add(mtx[0][i+1]);
			//step += 2;
			//comboBox.setSelectedIndex(0);
			//((ComboboxPeak) comboBox.getItemAt(0)).getPeakListRow().setPreferredPeakIdentity(identity)
			
//			// Update peaks identity.
//			peakList.getRow(row).setPeakIdentity(identity)
		}

		//
		//		comboBox.addItem("Snowboarding");
		//		comboBox.addItem("Rowing");
		//		comboBox.addItem("Knitting");
		//		comboBox.addItem("Speed reading");
		//		comboBox.addItem("Pool");
		//		comboBox.addItem("None of the above");
		//		sportColumn.setCellEditor(new DefaultCellEditor(comboBox));
		//
		//		//Set up tool tips for the sport cells.
		//		DefaultTableCellRenderer renderer =
		//				new DefaultTableCellRenderer();
		//		renderer.setToolTipText("Click for combo box");
		//		sportColumn.setCellRenderer(renderer);

		LOG.info("Fired: " + peakList.getRawDataFile(0).getAncestorDataFile(false).getName());
		//**data.add(objects);
		super.addRow(objects);
		//**fireTableRowsInserted(row, row);
		LOG.info("Affected row: " + row);
		
//		// Set widgets.
//		//for (Object obj : objects) {
//		for (int i=0; i < objects.size(); ++i) {
//			if (objects.get(i) instanceof JComboBox) {
//				((JComboBox<?>) objects.get(i)).setSelectedIndex(0);
//				fireTableCellUpdated(row, i);
//			}
//		}
	}


	//	//		data[0][0] = ((PeakListRow)element.keySet().toArray()[0]).getBestPeak().getDataFile().getName();
	//}

	//	public void setValueAt(Object value, int row, int col) {
	//		fireTableCellUpdated(row, col);
	//	}




	//
	//	private String[] columnNames = {"First Name",
	//			"Last Name",
	//			"Sport",
	//			"# of Years",
	//	"Vegetarian"};
	//	private Object[][] data = {
	//			{"Kathy", "Smith",
	//				"Snowboarding", new Integer(5), new Boolean(false)},
	//				{"John", "Doe",
	//					"Rowing", new Integer(3), new Boolean(true)},
	//					{"Sue", "Black",
	//						"Knitting", new Integer(2), new Boolean(false)},
	//						{"Jane", "White",
	//							"Speed reading", new Integer(20), new Boolean(true)},
	//							{"Joe", "Brown",
	//								"Pool", new Integer(10), new Boolean(false)}
	//	};
	//
	//	public final Object[] longValues = {"Jane", "Kathy",
	//			"None of the above",
	//			new Integer(20), Boolean.TRUE};
	//
	//	public int getColumnCount() {
	//		return columnNames.length;
	//	}
	//
	//	public int getRowCount() {
	//		return data.length;
	//	}
	//
	//	public String getColumnName(int col) {
	//		return columnNames[col];
	//	}
	//
	//	public Object getValueAt(int row, int col) {
	//		return data[row][col];
	//	}
	//
	//	/*
	//	 * JTable uses this method to determine the default renderer/
	//	 * editor for each cell.  If we didn't implement this method,
	//	 * then the last column would contain text ("true"/"false"),
	//	 * rather than a check box.
	//	 */
	//	public Class getColumnClass(int c) {
	//		return getValueAt(0, c).getClass();
	//	}

	//	/*
	//	 * Don't need to implement this method unless your table's
	//	 * editable.
	//	 */
	//	public boolean isCellEditable(int row, int col) {
	//		//Note that the data/cell address is constant,
	//		//no matter where the cell appears onscreen.
	//		if (col == 1 || col == 3) {
	//			return true;
	//		} else {
	//			return false;
	//		}
	//	}

	//	/*
	//	 * Don't need to implement this method unless your table's
	//	 * data can change.
	//	 */
	//	public void setValueAt(Object value, int row, int col) {
	//		if (DEBUG) {
	//			System.out.println("Setting value at " + row + "," + col
	//					+ " to " + value
	//					+ " (an instance of "
	//					+ value.getClass() + ")");
	//		}
	//
	//		data[row][col] = value;
	//		fireTableCellUpdated(row, col);
	//
	//		if (DEBUG) {
	//			System.out.println("New value of data:");
	//			printDebugData();
	//		}
	//	}
	//
	//	private void printDebugData() {
	//		int numRows = getRowCount();
	//		int numCols = getColumnCount();
	//
	//		for (int i=0; i < numRows; i++) {
	//			System.out.print("    row " + i + ":");
	//			for (int j=0; j < numCols; j++) {
	//				System.out.print("  " + data[i][j]);
	//			}
	//			System.out.println();
	//		}
	//		System.out.println("--------------------------");
	//	}

}

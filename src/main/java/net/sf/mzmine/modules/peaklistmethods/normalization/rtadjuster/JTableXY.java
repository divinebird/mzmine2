package net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster;

import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;

public class JTableXY extends JTable
{
    protected CellEditorModel cm;

    public JTableXY()
     {
         super();
         cm = null;
     }

     public JTableXY(TableModel tm)
     {
         super(tm);
         cm = null;
     }

     public JTableXY(TableModel tm, TableColumnModel cm)
     {
         super(tm,cm);
         cm = null;
     }

     public JTableXY(TableModel tm, TableColumnModel cm, ListSelectionModel sm)
     {
         super(tm,cm,sm);
         cm = null;
     }

     public JTableXY(int rows, int cols)
     {
         super(rows,cols);
         cm = null;
     }

     public JTableXY(final Vector rowData, final Vector columnNames)
     {
         super(rowData, columnNames);
         cm = null;
     }

     public JTableXY(final Object[][] rowData, final Object[] colNames)
     {
         super(rowData, colNames);
         cm = null;
     }

     // new constructor
     public JTableXY(TableModel tm, CellEditorModel rm)
     {
         super(tm,null,null);
         this.cm = rm;
     }

     public void setCellEditorModel(CellEditorModel rm)
     {
         this.cm = rm;
     }

     public CellEditorModel getCellEditorModel()
     {
         return cm;
     }

     public TableCellEditor getCellEditor(int row, int col)
     {
         TableCellEditor tmpEditor = null;
         if (cm!=null)
             tmpEditor = cm.getEditor(row, col);
         if (tmpEditor!=null)
             return tmpEditor;
         return super.getCellEditor(row, col);
     }
 }
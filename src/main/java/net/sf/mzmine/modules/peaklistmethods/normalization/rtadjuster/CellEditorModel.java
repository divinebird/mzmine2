package net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster;

import javax.swing.table.*;


import java.util.*;
public class CellEditorModel
{
	
	public class Pair<A, B> {
	    private A first;
	    private B second;

	    public Pair(A first, B second) {
	    	super();
	    	this.first = first;
	    	this.second = second;
	    }

	    public int hashCode() {
	    	int hashFirst = first != null ? first.hashCode() : 0;
	    	int hashSecond = second != null ? second.hashCode() : 0;

	    	return (hashFirst + hashSecond) * hashSecond + hashFirst;
	    }

	    public boolean equals(Object other) {
	    	if (other instanceof Pair) {
	    		Pair<?, ?> otherPair = (Pair<?, ?>) other;
	    		return 
	    		((  this.first == otherPair.first ||
	    			( this.first != null && otherPair.first != null &&
	    			  this.first.equals(otherPair.first))) &&
	    		 (	this.second == otherPair.second ||
	    			( this.second != null && otherPair.second != null &&
	    			  this.second.equals(otherPair.second))) );
	    	}

	    	return false;
	    }

	    public String toString()
	    { 
	           return "(" + first + ", " + second + ")"; 
	    }

	    public A getFirst() {
	    	return first;
	    }

	    public void setFirst(A first) {
	    	this.first = first;
	    }

	    public B getSecond() {
	    	return second;
	    }

	    public void setSecond(B second) {
	    	this.second = second;
	    }
	}
	
     private Hashtable<Pair<Integer, Integer>, TableCellEditor> data;
     
     public CellEditorModel()
     {
         data = new Hashtable<Pair<Integer, Integer>, TableCellEditor>();
     }
     public void addEditorForCell(int row, int col, TableCellEditor e )
     {
         data.put(new Pair<Integer, Integer>(row, col), e);
     }
     public void removeEditorForCell(int row, int col)
     {
         data.remove(new Pair<Integer, Integer>(row, col));
     }
     public TableCellEditor getEditor(int row, int col)
     {
         return (TableCellEditor)data.get(new Pair<Integer, Integer>(row, col));
     }
}
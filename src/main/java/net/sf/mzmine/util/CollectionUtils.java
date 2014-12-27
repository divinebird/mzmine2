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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * Collection API related utilities
 */
public class CollectionUtils {

	/**
	 * Returns an array of ints consisting of the elements of the specified
	 * collection.
	 * 
	 * @param collection
	 *            Collection of Integers
	 * @return Array of ints
	 */
	public static int[] toIntArray(Collection<Integer> collection) {
		int array[] = new int[collection.size()];
		int index = 0;
		Iterator<Integer> it = collection.iterator();
		while (it.hasNext()) {
			array[index++] = it.next();
		}
		return array;
	}

	/**
	 * Converts an array of ints to array of Integers
	 */
	public static Integer[] toIntegerArray(int array[]) {
		Integer newArray[] = new Integer[array.length];
		for (int i = 0; i < array.length; i++)
			newArray[i] = Integer.valueOf(array[i]);
		return newArray;
	}

	/**
	 * Change the type of array of Objects to an array of objects of type
	 * newClass.
	 * 
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] changeArrayType(Object[] array, Class<T> newClass) {

		ArrayList<T> newArray = new ArrayList<T>();

		for (int i = 0; i < array.length; i++) {
			// Only add those objects that can be cast to the new class
			if (newClass.isInstance(array[i])) {
				newArray.add(newClass.cast(array[i]));
			}
		}

		return newArray.toArray((T[]) Array.newInstance(newClass, 0));
	}

	/**
	 * Returns an array of doubles consisting of the elements of the specified
	 * collection.
	 * 
	 * @param collection
	 *            Collection of Doubles
	 * @return Array of doubles
	 */
	public static double[] toDoubleArray(Collection<Double> collection) {
		double array[] = new double[collection.size()];
		int index = 0;
		Iterator<Double> it = collection.iterator();
		while (it.hasNext()) {
			array[index++] = it.next();
		}
		return array;
	}

	/**
	 * Checks if the haystack array contains all elements of needles array
	 * 
	 * @param haystack
	 *            array of ints
	 * @param needles
	 *            array of ints
	 * @return true if haystack contains all elements of needles
	 */
	public static boolean isSubset(int haystack[], int needles[]) {
		needleTraversal: for (int i = 0; i < needles.length; i++) {
			for (int j = 0; j < haystack.length; j++) {
				if (needles[i] == haystack[j])
					continue needleTraversal;
			}
			return false;
		}
		return true;
	}

	/**
	 * Checks if the haystack array contains a specified element
	 * 
	 * @param haystack
	 *            array of objects
	 * @param needle
	 *            object
	 * @return true if haystack contains needle
	 */
	public static <T> boolean arrayContains(T haystack[], T needle) {
		for (T test : haystack) {
			if (needle.equals(test))
				return true;
		}
		return false;
	}

	/**
	 * Concatenate two arrays
	 * 
	 * @param first
	 *            array of objects
	 * @param second
	 *            array of objects
	 * @return both array of objects
	 */
	public static <T> T[] concat(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}
	
	public static <T> void matrixCopy(T[][] aSource, T[][] aDestination) {
	    for (int i = 0; i < aSource.length; i++) {
	        System.arraycopy(aSource[i], 0, aDestination[i], 0, aSource[i].length);
	    }
	}


}

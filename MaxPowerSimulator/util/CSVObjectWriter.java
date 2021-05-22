/*
 * uk.ac.macaulay.util: CSVObjectWriter.java
 * 
 * Copyright (C) 2011 Macaulay Institute
 * 
 * This file is part of utils.
 * 
 * utils is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * utils is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with utils. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Author: Gary Polhill
 */

package util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;


/**
 * <!-- CSVObjectWriter -->
 * 
 * Class to write a CSV file from collections of objects that follow
 * {@link CSVObject}.
 */
public class CSVObjectWriter {

	/**
	 * File pointer to print the data to
	 */
	private PrintWriter fp;

	/**
	 * String of headings from the first line. All data written to the file must
	 * have the same data.
	 */
	private String headings;

	/**
	 * Filename the data are to be saved to
	 */
	private final String filename;

	/**
	 * Boolean stating whether or not the file can be written to
	 */
	private boolean writeable;

	/**
	 * Constructor for incremental usage.
	 * 
	 * @param filename
	 * @throws IOException
	 */
	public CSVObjectWriter(String filename) throws IOException {
		fp = FileOpener.write(filename);
		headings = null;
		this.filename = filename;
		writeable = true;
	}

	/**
	 * Convenience static method to write all the data in the dataset
	 * 
	 * @param filename
	 * @param data
	 * @throws IOException
	 */
	public static void write(String filename, CSVObject data[]) throws IOException {
		CSVObjectWriter fp = new CSVObjectWriter(filename);
		fp.write(data);
		fp.close();
	}

	/**
	 * Convenience static method to write all the data in the dataset
	 * 
	 * @param filename
	 * @param data
	 * @throws IOException
	 */
	public static void write(String filename, Collection<? extends CSVObject> data) throws IOException {
		CSVObjectWriter fp = new CSVObjectWriter(filename);
		fp.write(data);
		fp.close();
	}

	/**
	 * Write an array of data to the file. All the data must return the same
	 * heading line.
	 * 
	 * @param data
	 * @throws IOException
	 */
	public void write(CSVObject data[]) throws IOException {
		for(CSVObject obj: data) {
			write(obj);
		}
	}

	/**
	 * Write a collection of data to the file. All the data must return the same
	 * heading line.
	 * 
	 * @param data
	 * @throws IOException
	 */
	public void write(Collection<? extends CSVObject> data) throws IOException {
		for(CSVObject obj: data) {
			write(obj);
		}
	}

	/**
	 * Write a single line of data to the file. If the first line written, it will
	 * determine the heading line; otherwise, this line must return the same
	 * heading line as the data for the first line did.
	 * 
	 * @param data
	 * @throws IOException
	 *           if attempt made to write data to close file or to write data
	 *           with different heading than the first line
	 */
	public void write(CSVObject data) throws IOException {
		if(!writeable) {
			throw new IOException("Attempt to write data to closed file \"" + filename + "\"");
		}
		if(headings == null) {
			headings = data.getCSVheadings();
			fp.println(headings);
		}
		else {
			if(!headings.equals(data.getCSVheadings())) {
				throw new IOException("Attempt to write data with different headings (" + data.getCSVheadings()
						+ ") than those of the first data written to the file \"" + filename + "\" (" + headings + ")");
			}
		}
		fp.println(data.getCSVdata());
	}

	/**
	 * @param data
	 * @return <code>true</code> if the data are compatible with the current
	 *         headings, <i>or</i> if no headings have been written (which can be
	 *         checked separately using {@link #headingsWritten()}).
	 */
	public boolean compatible(CSVObject data) {
		if(headings == null) return true;
		else return headings.equals(data.getCSVheadings());
	}

	/**
	 * @param data
	 * @return <code>true<code> if there is at least one member of <code>data</code>
	 *         <i>and</i> <i>either</i> a heading line has already been written
	 *         and all the data in the array are compatible with it,
	 *         <i>or</i> a heading line has not already been written and all the
	 *         data in the array return the same heading as the first member. Note
	 *         that the latter case will not guarantee safe writing to the file --
	 *         e.g. if an incompatible object is written before the contents of
	 *         the array.
	 */
	public boolean allCompatible(CSVObject data[]) {
		if(headings == null && data.length > 0) {
			String first = data[0].getCSVheadings();
			for(int i = 1; i < data.length; i++) {
				if(!first.equals(data[i].getCSVheadings())) return false;
			}
			return true;
		}
		else if(data.length > 0) {
			for(CSVObject obj: data) {
				if(!compatible(obj)) return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * @return <code>true</code> if the headings have been written to the file
	 */
	public boolean headingsWritten() {
		return headings != null;
	}

	/**
	 * @return <code>true</code> if the file pointer has not been closed yet
	 */
	public boolean isWriteable() {
		return writeable;
	}

	/**
	 * Close the file and make the object unwritable
	 */
	public void close() {
		fp.close();
		writeable = false;
	}

	/**
	 * @see Object#finalize()
	 * 
	 *      Call {@link #close()} if the object has no references
	 */
	protected void finalize() {
		close();
	}
}

/*
 * util: CSVObject.java
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


/**
 * <!-- CSVObject -->
 *
 * Interface for classes to follow if they want to use CSVObjectWriter
 */
public interface CSVObject {

	/**
	 * @return A line of CSV data (without the end of line marker)
	 */
	public String getCSVdata();

	/**
	 * @return A line of headings for the CSV data returned by
	 *         {@link #getCSVdata()}
	 */
	public String getCSVheadings();

	/**
	 * Tools to help with creating CSV data, if required
	 */
	public class Tools {
		public static final String ID_COLUMN_NAME = "id";

		private Tools() {
			// Prevent construction
		}

		/**
		 * @param value
		 * @return A formatted entry, surrounded by quotes if needed because the
		 *         string contains quotes, or a comma
		 */
		public static String getEntry(String value) {
			String doubleQuoted = value.replaceAll("\"", "\"\"");
			if(!doubleQuoted.equals(value) || value.contains(",")) {
				return "\"" + doubleQuoted + "\"";
			}
			else {
				return value;
			}
		}

		/**
		 * @param value
		 * @return Quoted double value (if required by locale)
		 */
		public static String getEntry(double value) {
			return getEntry(Double.toString(value));
		}

		/**
		 * @param value
		 * @return Quoted float value (if required by locale)
		 */
		public static String getEntry(float value) {
			return getEntry(Float.toString(value));
		}

		/**
		 * @param value
		 * @return Quoted object value
		 */
		public static String getEntry(Object value) {
			return getEntry(value.toString());
		}

		/**
		 * Allow two datasets to be merged into the same CSV file (assuming they
		 * have the same number of rows)
		 *
		 * @param obj1
		 * @param obj2
		 * @return A merged CSVObject
		 */
		public static CSVObject merge(final CSVObject obj1, final CSVObject obj2) {
			return new CSVObject() {

				@Override
				public String getCSVdata() {
					return obj1.getCSVdata() + "," + obj2.getCSVdata();
				}

				@Override
				public String getCSVheadings() {
					return obj1.getCSVheadings() + "," + obj2.getCSVheadings();
				}

			};
		}

		/**
		 * Allow a dataset to be saved with an ID column
		 *
		 * @param id
		 * @param obj
		 * @return A CSVObject with an assigned ID
		 */
		public static CSVObject id(final String id, final CSVObject obj) {
			return new CSVObject() {

				@Override
				public String getCSVdata() {
					return id + "," + obj.getCSVdata();
				}

				@Override
				public String getCSVheadings() {
					return ID_COLUMN_NAME + "," + obj.getCSVheadings();
				}

			};
		}

		/**
		 * Convenience method allowing integer assigned IDs
		 *
		 * @param id
		 * @param obj
		 * @return A CSVObject with an assigned integer ID
		 */
		public static CSVObject id(final int id, final CSVObject obj) {
			return id(Integer.toString(id), obj);
		}
	}
}
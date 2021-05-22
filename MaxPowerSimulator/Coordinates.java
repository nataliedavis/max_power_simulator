/*
This file is part of MaxPowerSimulator.

MaxPowerSimulator is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

MaxPowerSimulator is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with MaxPowerSimulator.  If not, see <https://www.gnu.org/licenses/>.

Author: Natalie Davis, Gary Polhill
*/

import util.CSVObject;
import util.Interval;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * <!-- Coordinates -->
 * 
 * A data structure to store the location of an consumer or branch point.
 * 	Comprised of coordinates and ranges for each coordinate (not currently
 * 	used).
 * 
 * @author Natalie Davis, Gary Polhill
 */
public class Coordinates implements CSVObject {

	/**
	 * <!-- coords -->
	 * A list of the coordinates
	 */
	private final List<Double> coords;

	/**
	 * <!-- ranges -->
	 * A list of ranges for each coordinate
	 */
	private final List<Interval<Double>> ranges;

	/**
	 * <!-- Coordinates constructor -->
	 * Constructors with array input
	 * @param ranges an array of the minimum and maximum coordinates in each dimension
	 * @param coords an array of the coordinates
	 */
	public Coordinates(Interval<Double>[] ranges, Double... coords) {
		this(Arrays.asList(ranges), Arrays.asList(coords));
	}

	/**
	 * <!-- Coordinates constructor -->
	 * Convenience constructor with list input
	 * @param ranges a list of the minimum and maximum coordinates in each dimension
	 * @param coords an array of the coordinates
	 */
	public Coordinates(List<Interval<Double>> ranges, double[] coords) {
		for(Interval<Double> range: ranges) {
			if(range == null) throw new NullPointerException();
		}
		for(Double coord: coords) {
			if(coord == null) throw new NullPointerException();
		}

		// Store coordinates as list
		List<Double> coordsList = new LinkedList<>();
		for (double c : coords) {
			coordsList.add(c);
		}

		this.ranges = ranges;
		this.coords = coordsList;
	}

	/**
	 * <!-- Coordinates constructor -->
	 * Main constructor
	 * 
	 * @param ranges the permitted range for coordinates, defines spatial topology
	 * @param coords the coordinates defined in space
	 */
	public Coordinates(List<Interval<Double>> ranges, List<Double> coords) {
		for(Interval<Double> range: ranges) {
			if(range == null) throw new NullPointerException();
		}
		for(Double coord: coords) {
			if(coord == null) throw new NullPointerException();
		}
		if(ranges.size() != coords.size()) {
			throw new IllegalArgumentException("Number of ranges (" + ranges.size()
					+ ") is different from the number of co-ordinates (" + coords.size() + ")");
		}
		if(coords.size() == 0) {
			throw new IllegalArgumentException("Cannot have zero-length co-ordinates");
		}
		this.coords = coords;
		this.ranges = ranges;
	}

	/**
	 * <!-- size -->
	 * @return the size of the coordinates - same as the dimensionality of the space
	 */
	public long size() {
		return coords.size();
	}

	/**
	 * <!-- getCoords -->
	 * @return the coordinates as a double[] array
	 */
	public double[] getCoords() {
		Double[] ary = coords.toArray(new Double[0]);

		// Convert Double[] to double[]
		double[] ret = new double[ary.length];
		for(int i = 0; i < ary.length; i++) {
			ret[i] = ary[i];
		}

		return ret;
	}

	/** 
	 * <!-- getCSVdata -->
	 * @see util.CSVObject#getCSVdata()
	 * @return the coordinates as Cartesian in a comma-delimited string
	 * 	for insertion into a CSV
	 */
	@Override
	public String getCSVdata() {
		StringBuffer buff = new StringBuffer();
		
		double[] cartesian = MaxPowerParam.getTopology().asCartesianParam(getCoords());
		
		for(Double coord: cartesian) {
			if(buff.length() > 0) buff.append(",");
			buff.append(CSVObject.Tools.getEntry(Double.toString(coord)));
		}
		return buff.toString();
	}

	/** 
	 * <!-- getCSVheadings -->
	 * @see util.CSVObject#getCSVheadings()
	 * @return comma-delimited headings for the coordinates CSV - returns x, xy, xyz,
	 * 	or x1...xN depending on dimensionality
	 */
	@Override
	public String getCSVheadings() {
		
		double[] cartesian = MaxPowerParam.getTopology().asCartesianParam(getCoords());

		// One-dimensional space)
		if(cartesian.length == 1) {
			return "x";
		}
		// Two-dimensional space
		else if(cartesian.length == 2) {
			return "x,y";
		}
		// Three-dimensional space
		else if(cartesian.length == 3) {
			return "x,y,z";
		}
		// N-dimensional space
		else {
			StringBuilder buff = new StringBuilder();
			
			for(int i = 1; i <= cartesian.length; i++) {
				if(i > 1) buff.append(",");
				buff.append("x");
				buff.append(i);
			}
			
			return buff.toString();
		}
	}

}

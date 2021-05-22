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

Authors: Natalie Davis, Gary Polhill
*/

import util.FileOpener;
import util.Interval;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * <!-- Topology -->
 * 
 * <p>
 * An enumeration of topologies for the space, for the purposes of computing
 * distances between co-ordinates
 * </p>
 * 
 * <p>
 * The following options are provided:
 * </p>
 * 
 * <ul>
 * <li><b>PLANE</b>: The co-ordinates lie in an n-Dimensional Euclidean space.
 * The numbers supplied to {@link #distance(double[], double[])} are cartesian
 * co-ordinates.</li>
 * <li><b>SPHERE</b>: The co-ordinates lie inside a sphere. The first number is
 * the radius showing the distance from the centre of the sphere, the remaining
 * numbers are thetas. The parameterised distance function
 * {@link #distance(double[], double[], double[])} must be used, the third
 * argument specifying in element 0 the radius of the sphere.</li>
 * <li><b>SPHERE_SURFACE</b>: The co-ordinates lie on the surface of a
 * sphere. The first coordinate is the radial coordinate (same for all points,
 * defines size of sphere), the other two co-ordinates are polar co-ordinates
 * specifying the thetas.</li>
 * </ul>
 * 
 * @author Gary Polhill, Natalie Davis
 */
public enum Topology {

	// Possible topologies
	PLANE, SPHERE_SURFACE, SPHERE;

	/**
	 * <!-- isCartesian -->
	 * 
	 * @return <code>true</code> if the co-ordinates interpreted by the distance
	 *         functions are cartesian
	 */
	public boolean isCartesian() {
		switch(this) {
		case PLANE:
			return true;
		case SPHERE_SURFACE:
		case SPHERE:
			return false;
		default:
			throw new IllegalArgumentException("That is not a recognised topology");
		}
	}

	/**
	 * <!-- asCartesian -->
	 * 
	 * Converts coordinates to Cartesian. Note that for parameterised topologies, this method does not check
	 * parameters.
	 * 
	 * @param coords The coordinates to convert to Cartesian
	 * @return Cartesian equivalents of the coordinates. Only makes any
	 *         difference for SPHERE and SPHERE_SURFACE.
	 */
	public double[] asCartesian(double[] coords) {
		switch(this) {
		case PLANE:
			return Arrays.copyOf(coords, coords.length);
		case SPHERE_SURFACE:
		case SPHERE: {
			double[] carts = new double[coords.length];

			double pisin = 1.0;
			for(int i = 1; i < coords.length; i++) {
				carts[i - 1] = coords[0] * Math.cos(coords[i]) * pisin;
				pisin *= Math.sin(coords[i]);
			}
			carts[coords.length - 1] = coords[0] * pisin;

			return carts;
		}
		default:
			throw new IllegalArgumentException("That is not a recognised topology");
		}
	}

	/**
	 * <!-- asCartesian -->
	 * 
	 * Return cartesian coordinates using the parameters. For <code>PLANE</code>,
	 * the <code>param</code> array is ignored.
	 * For <code>SPHERE_SURFACE</code>, the <code>param</code> is expected to be
	 * an array of length 1, containing <i>r</i>, the radius of the sphere.
	 * In <code>SPHERE</code>, element 0 of <code>coords</code> represents the
	 * radius component of the polar co-ordinates, and is 'wrapped' so that it
	 * falls within range of the appropriate topology.
	 * 
	 * @param coords the coordinates to transform to cartesian
	 * @param param the parameterisation required to transform the coordinates to cartesian
	 * @return Cartesian equivalent to coordinates
	 * @throws IllegalArgumentException
	 *           if the lengths of <code>coords</code> and <code>param</code> are
	 *           different for <code>TORUS_SURFACE</code>, <code>param</code> has
	 *           non-unit length for <code>SPHERE</code>, or <code>param</code> is
	 *           not of length 2 with the second element greater than the first
	 *           for <code>HOLLOW_SPHERE</code>.
	 */
	public double[] asCartesian(double[] coords, double[] param) {
		switch(this) {
		case PLANE:
			return asCartesian(coords);
		case SPHERE:
		case SPHERE_SURFACE:
			if(param.length != 1) {
				throw new IllegalArgumentException();
			}

			if(coords[0] <= param[0]) {
				return asCartesian(coords);
			} else {
				double[] newCoords = Arrays.copyOf(coords, coords.length);
				newCoords[0] = wrap(coords[0], param[0]);
				return asCartesian(newCoords);
			}
		default:
			throw new IllegalArgumentException("That is not a recognised topology");
		}
	}

	/**
	 * <!-- asCartesianParam -->
	 * 
	 * Converts coordinates to Cartesian. Refers to the parameter object to get the
	 * parameters to pass in to the full implementation of this method
	 * 
	 * @param coords the coordinates to convert to Cartesian
	 * @return cartesian co-ordinate array
	 */
	public double[] asCartesianParam(double[] coords) {
		return asCartesian(coords, MaxPowerParam.getTopologyParamArray(this));
	}

	/**
	 * <!-- fromCartesian -->
	 * Converts Cartesian coordinates to polar coordinates
	 * @param coords the coordinates to convert to polar
	 * @return Polar coordinate array converted from Cartesian
	 */
	public double[] fromCartesian(double[] coords) {
		if (isCartesian())  {
			// Meant to be Cartesian - do not convert
			return Arrays.copyOf(coords, coords.length);
		} else {
			double[] polar = new double[coords.length];
			// Radius
			polar[0] = MaxPowerParam.getSphereR();
			// Longitude (azimuth)
			polar[1] = Math.atan2(coords[1], coords[0]);
			// Latitude (from polar)
			polar[2] = Math.acos(coords[2]/polar[0]) - Math.PI/2;
			return polar;
		}
	}

	/**
	 * <!-- distance -->
	 * 
	 * This method computes the distance between two points using the topology. It
	 * uses the Euclidean distance for <code>PLANE</code> and <code>SPHERE</code>,
	 * and computes the arc length between the two angles for <code>SPHERE_SURFACE</code>.
	 * 
	 * @param x1 The coordinates of the first set of points
	 * @param x2 The coordinates of the second set of points
	 * @return The distance between <code>x1</code> and <code>x2</code>
	 * @throws IllegalArgumentException
	 *           if <code>x1</code> and <code>x2</code> have different lengths,
	 *           the topology is <code>TORUS_SURFACE</code> or
	 *           <code>HOLLOW_SPHERE</code>, or the topology is <code>SPHERE_SURFACE</code>
	 *           or <code>SPHERE_SURFACE</code> and the length of <code>x1</code> is more than 2.
	 */
	public double distance(double[] x1, double[] x2) {
		if(x1.length != x2.length) {
			throw new IllegalArgumentException();
		}
		switch(this) {
		case PLANE: {
			// Use the more accurate computation for 2D
			return Math.hypot(x1[0] - x2[0], x1[1] - x2[1]);
		}
		case SPHERE: {
			return PLANE.distance(asCartesian(x1), asCartesian(x2));
		}
		case SPHERE_SURFACE: {
			// Let's just keep this in 3D, shall we?
			if (x1.length != 3) {
				throw new IllegalArgumentException("Extendable sphere surfaces require three coordinates: " +
						"distance from radius and two angles");
			}

			double theta_diff = Math.abs(x1[1] - x2[1]);
			return MaxPowerParam.getSphereR() * Math.atan2(
					Math.sqrt(Math.pow(Math.cos(x2[2]) * Math.sin(theta_diff), 2.0)
							+ Math.pow(
							(Math.cos(x1[2]) * Math.sin(x2[2]))
									- (Math.sin(x1[2]) * Math.cos(x2[2]) * Math.cos(theta_diff)),
							2.0)),
					(Math.sin(x1[2]) * Math.sin(x2[2]))
							+ (Math.cos(x1[2]) * Math.cos(x2[2]) * Math.cos(theta_diff)));
		}
		default:
			throw new IllegalArgumentException("That is not a recognised topology");
		}
	}

	/**
	 * <!-- distance -->
	 * 
	 * <p>
	 * Compute the distance between two co-ordinates <code>x1</code> and
	 * <code>x2</code> with parameters <code>param</code>. See
	 * {@link #asCartesian(double[], double[])} for an explanation of the
	 * <code>param</code> array.
	 * </p>
	 * 
	 * <p>
	 * For <code>PLANE</code> and <code>SPHERE_SURFACE</code>,
	 * the <code>param</code> array is ignored, and the result is the same as calling
	 * {@link #distance(double[], double[])}.
	 * </p>
	 * 
	 * <p>
	 * For <code>SPHERE</code>, the co-ordinates <code>x1</code> and
	 * <code>x2</code> are wrapped such that their <i>r</i> components are in the
	 * range [0, <code>param[0]</code>], and the distance is then the Euclidean
	 * distance between the two points.
	 * </p>
	 * 
	 * @param x1 The coordinates of the first set of points
	 * @param x2 The coordinates of the second set of points
	 * @param param The parameter array for that topology
	 * @return The distance between <code>x1</code> and <code>x2</code> according
	 *         to the topology
	 * @throws IllegalArgumentException
	 *           if <code>x1</code> and <code>x2</code> have different lengths; an
	 *           arc distance has to be computed on the surface of an
	 *           <i>n</i>-sphere where <i>n</i> > 3 (the lengths of
	 *           <code>x1</code> and <code>x2</code> are > 2 for
	 *           <code>SPHERE_SURFACE</code> and > 3 for
	 *           <code>HOLLOW_SPHERE</code>); or <code>param</code> is incorrectly
	 *           configured for those topologies using this array.
	 */
	public double distance(double[] x1, double[] x2, double[] param) {
		if(x1.length != x2.length) {
			throw new IllegalArgumentException();
		}
		switch(this) {
		case PLANE:
		case SPHERE_SURFACE:
			return distance(x1, x2);
		case SPHERE: {
			if(param.length != 1) {
				throw new IllegalArgumentException();
			}

			if(x1[0] <= param[0] && x2[0] <= param[0]) {
				return distance(x1, x2);
			}
			else if(x1[0] <= param[0]) {
				return distance(x1, wrapPolar(x2, param[0]));
			}
			else if(x2[0] <= param[0]) {
				return distance(wrapPolar(x1, param[0]), x2);
			}
			else {
				return distance(wrapPolar(x1, param[0]), wrapPolar(x2, param[0]));
			}
		}
		default:
			throw new IllegalArgumentException("That is not a recognised topology");
		}
	}

	/**
	 * <!-- distanceParam -->
	 * 
	 * Return the distance as implemented by the parameterised version of the
	 * topology
	 * 
	 * @param x1 The coordinates of the first set of points
	 * @param x2 The coordinates of the second set of points
	 * @return the distance between x1 and x2
	 */
	public double distanceParam(double[] x1, double[] x2) {
		return distance(x1, x2, MaxPowerParam.getTopologyParamArray(this));
	}

	/**
	 * <!-- getOrdinateRangesParam -->
	 * 
	 * @return A list of the parameters for this topology from the
	 *         {@link MaxPowerParam} class
	 */
	public List<Interval<Double>> getOrdinateRangesParam() {
		List<Interval<Double>> rangeList = new LinkedList<>();
		switch(this) {
		case PLANE:
			double[] pLimits = MaxPowerParam.getPlaneMaxCoords();
			for(double maxCoord : pLimits) {
				rangeList.add(new Interval<>(0.0, maxCoord));
			}
			break;
		case SPHERE:
			rangeList.add(new Interval<>(0.0, MaxPowerParam.getSphereR()));
			rangeList.add(new Interval<>(0.0, Math.PI));
			rangeList.add(new Interval<>(0.0, Math.PI * 2.0));
			break;
		case SPHERE_SURFACE:
			// All coordinates should have the same radial coordinate - the radius
			rangeList.add(new Interval<>(MaxPowerParam.getSphereR(), MaxPowerParam.getSphereR()));
			rangeList.add(new Interval<>(0.0, Math.PI));
			rangeList.add(new Interval<>(0.0, Math.PI * 2.0));
			break;
		default:
			throw new IllegalArgumentException("That is not a recognised topology");
		}

		return rangeList;
	}

	/**
	 * <!--getRandomCoordinates-->
	 *
	 * Creates and returns a list of random coordinates that is made by sampling the uniform dist.
	 * from the ordinate range for this topology. The length of list is the number of coordinates needed
	 * to define a location in that topology (number of dimensions).
	 *
	 * @return list of random coordinates
	 */
	public List<Double> getRandomCoordinates() {
		int len;
		switch(this) {
			// Three-dimensional topologies
			case SPHERE:
			case SPHERE_SURFACE:
				len = 3;
				break;
			// Two-dimensional topology
			case PLANE:
				len = 2;
				break;
			default:
				throw new IllegalArgumentException("That is not a recognised topology");
		}
		List<Double> coordList = new LinkedList<>();
		for (int i = 0; i < len; i++) {
			coordList.add(getOrdinateRangesParam().get(i).getDoubleUniformSample());
		}

		return coordList;
	}

	/**
	 * <!-- readCoords -->
	 * Reads in a file storing coordinates, such as those specifying locations of multiple, fixed consumers
	 * or branch points
	 * @param coordfile The CSV file storing the coordinates
	 * @param numCoords The number of coordinates to expect (ex. number of consumers or branch points)
	 * @return list of coordinates
	 * @throws IOException if file is empty
	 * @throws IllegalArgumentException if the number of coordinates is not as specified
	 */
	public List<double[]> readCoords(String coordfile, int numCoords) throws IOException,
			IllegalArgumentException {

		List<double[]> coordList = new LinkedList<>();
		BufferedReader fp = FileOpener.read(coordfile);

		String headingline = fp.readLine();
		if(headingline == null) {
			throw new IOException("No text in Coordinates file " + coordfile);
		}

		// Determine dimensionality of coordinates
		int coordLength = headingline.split(",").length;

		String line;
		while((line = fp.readLine()) != null) {
			String[] cells = line.split(",");

			// Check that all coordinates are the same length
			if (cells.length != coordLength) {
				throw new IllegalArgumentException("The coordinates are expected to be specified in " +
						coordLength + " dimensions, but some coordinates are specified in " + cells.length +
						" dimensions");
			}

			double[] location = new double[cells.length];
			for(int i = 0; i < cells.length; i++) {
				location[i] = Double.parseDouble(cells[i]);
			}

			// Convert the coordinates from Cartesian to polar (if applicable)
			double[] loc = fromCartesian(location);
			coordList.add(loc);
		}
		fp.close();

		// Check that number of coordinates is what was specified in parameter file
		if (coordList.size() != numCoords) {
			throw new IllegalArgumentException("Number of nodes is " + coordList.size() +
					", but is expected to be " + numCoords);
		}

		return coordList;
	}

	/**
	 * <!-- wrap -->
	 * 
	 * Wrapping involves taking a number (<code>n</code>) of arbitrary range, and
	 * 'wrapping' it to the range [0, <code>limit</code>], by (if <code>n</code>
	 * is positive) subtracting <code>limit</code> from <code>n</code> until it is
	 * within range. (If <code>n</code> is positive, then <code>limit</code> is
	 * added to <code>n</code> until it is within range.)
	 * 
	 * @param n The number to wrap
	 * @param limit The maximum range for the number
	 * @return <code>n</code> wrapped to <code>limit</code>
	 * @throws IllegalArgumentException
	 *           if <code>limit</code> is not positive
	 */
	public static double wrap(double n, double limit) {
		if(limit <= 0.0) throw new IllegalArgumentException("The maximum range for a wrapped variable" +
				" must be positive");

		double wrapped = n % limit;
		if(wrapped < 0.0) {
			wrapped += limit;
		}
		return wrapped;
	}

	/**
	 * <!-- wrapPolar -->
	 * 
	 * Wrap polar coordinates <code>x</code> so that the magnitude component lies
	 * in [0, <code>r</code>], and all but the last angle lies in [0, <code>PI</code>].
	 * Last angle should be between [0,<code>2PI</code>].
	 * 
	 * @param x The polar coordinate to wrap
	 * @param r The magnitude component
	 * @return Wrapped polar coordinates
	 * @throws IllegalArgumentException
	 *           if <code>x</code> doesn't have at least two elements
	 */
	public static double[] wrapPolar(double[] x, double r) {
		if(x.length < 2) throw new IllegalArgumentException();

		double[] w = Arrays.copyOf(x, x.length);

		w[0] = wrap(w[0], r);

		for(int i = 1; i < w.length - 1; i++) {
			w[i] = wrap(w[i], Math.PI);
		}
		w[w.length - 1] = wrap(w[w.length - 1], 2* Math.PI);

		return w;
	}
}

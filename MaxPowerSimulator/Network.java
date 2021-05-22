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

import util.CSVObject;
import util.CSVObjectWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <!-- Network -->
 * <p>
 *  A custom data structure to store characteristics of the network (consumer and resource
 *  locations, branch point locations if applicable, and connections matrix), along with
 *  its own metadata (size, etc.)
 * </p>
 * @author Natalie Davis, Gary Polhill
 */

public class Network {

    /**
     * <!-- branchPoints -->
     * Array of branch points. May be <code>null</code> if there are none
     */
    private final Coordinates[] branchPoints;

    /**
     * <!-- matrix -->
     * Network of link strengths/connections - can be binary
     */
    private final Matrix matrix;

    /**
     * <!-- resources -->
     * Array of resources.
     */
    private final Resource[] resources;

    /**
     * <!-- consumers -->
     * The coordinates of the consumers
     */
    private final Coordinates[] consumers;

    /**
     * <!-- coordSize -->
     * Number of coordinates required to specify a location. Determined by dimensionality of
     * spatial topology.
     */
    private final int coordSize;

    /**
     * <!-- potentials -->
     * The potentials of the consumers - calculated in NR power flows equations
     */
    private List<Double> potentials;

    /**
     * <!-- Network constructor -->
     *
     * Build a Network chromosome from a known set of branch point and consumer coordinates,
     * matrix matrix, and resource array.
     *
     * @param branchPoints the coordinates of the branch points (if used)
     * @param matrix the matrix of node connections
     * @param resources the coordinates and potentials of the resources
     * @param consumers the coordinates of the consumers
     */
    public Network(Coordinates[] branchPoints, Matrix matrix,
                   Resource[] resources, Coordinates[] consumers) {

        // Check that the consumer coordinates are all the same size
        for (int i = 1; i < consumers.length; i++) {
            if (consumers[0].getCoords().length != consumers[i].getCoords().length) {
                throw new IllegalArgumentException("All consumer coordinates must be the same length!");
            }
        }

        // Check that the consumer coordinates are all > 1 in size
        if (consumers[0].getCoords().length == 0) {
            throw new IllegalArgumentException("Array of consumers coordinates should have non-zero size");
        }
        coordSize = consumers[0].getCoords().length;

        // Check that branch point coordinates are all the same size as consumer coordinates
        for (int i = 1; i < branchPoints.length; i++) {
            if (branchPoints[i].getCoords().length != coordSize) {
                throw new IllegalArgumentException("Branch point coordinates must all have the same size as " +
                        " consumer coordinates (consumers = " + coordSize + "; Branch point #" + i + " = " +
                        branchPoints[i].getCoords().length + ")");
            }
        }

        // Check that resource coordinates are all the same size as consumer coordinates
        for (int i = 0; i < resources.length; i++) {
            if (resources[i].coordSize() != coordSize) {
                throw new IllegalArgumentException(
                        "Resource coordinates must all have the same size as consumers coordinates (consumers = "
                                + coordSize + "; Resource #" + i + " = " + resources[i].coordSize());
            }
        }

        // Check that connections matrix has the same number of rows/columns as network has nodes
        long nCoords = branchPoints.length + consumers.length + resources.length;

        if (nCoords != matrix.nRow() || nCoords != matrix.nCol()) {
            throw new IllegalArgumentException("Network has " + matrix.nRow() + " rows and " + matrix.nCol()
                    + " columns for " + nCoords + " branch points, resources, and consumers" +
                    " -- these numbers should all be equal");
        }

        // Initialise all instance variables
        this.branchPoints = branchPoints;
        this.matrix = matrix;
        this.resources = resources;
        this.consumers = consumers;
        this.potentials = new ArrayList<>();
    }


    /**
     * Getters and setters
     */
    public Coordinates[] getBranchPoints() {
        return branchPoints;
    }

    public Matrix getMatrix() {
        return matrix;
    }

    public Resource[] getResources() {
        return resources;
    }

    public Coordinates[] getConsumers() {
        return consumers;
    }

    public int getCoordSize() { return coordSize; }

    public List<Double> getPotentials() { return potentials; }

    public void setPotentials(List<Double> potentials) { this.potentials = potentials; }

    /**
     * <!-- saveConsumers -->
     * Saves consumer coordinates to a CSV file
     * @param filename the file in which to store the consumers coordinates
     * @throws IOException
     */
    public void saveConsumers(String filename) throws IOException {
        CSVObjectWriter fp = new CSVObjectWriter(filename);
        for (int i = 0; i < consumers.length; i++) {
            fp.write(CSVObject.Tools.id(i + 1, consumers[i]));
        }
        fp.close();
    }

    /**
     * <!-- saveBranchPoints -->
     * Saves branch point coordinates to a CSV file
     * @param filename the CSV file where the coordinates will be saved
     * @throws IOException
     */
    public void saveBranchPoints(String filename) throws IOException {
        CSVObjectWriter fp = new CSVObjectWriter(filename);

        for (int i = 0; i < branchPoints.length; i++) {
            fp.write(CSVObject.Tools.id(i + 1 + consumers.length, branchPoints[i]));
        }

        fp.close();
    }

    /**
     * <!-- saveResources -->
     * Save the resources array to a CSV file.
     * @param filename the CSV file where resources will be saved
     * @throws IOException
     */
    public void saveResources(String filename) throws IOException {
        CSVObjectWriter fp = new CSVObjectWriter(filename);

        for (int i = 0; i < resources.length; i++) {
            fp.write(CSVObject.Tools.id(i + 1 + consumers.length + branchPoints.length,
                    resources[i]));
        }

        fp.close();
    }

    /**
     * <!-- saveNetwork -->
     * Saves connections matrix to a CSV file. Specifies link origin and destination node ID,
     *  as determined in saveConsumers(), saveBranchPoints(), and saveResources(), link strength
     * @param filename the CSV file where the matrix will be saved
     * @throws IOException
     */
    public void saveNetwork(String filename) throws IOException {
        CSVObjectWriter.write(filename, matrix.asCSVObjectArray(branchPoints, resources, consumers));
    }

}

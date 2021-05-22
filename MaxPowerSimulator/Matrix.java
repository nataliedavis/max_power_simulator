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
import util.FileOpener;
import util.Interval;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * <!-- Matrix -->
 * <p>
 * A data structure to store a matrix of link strengths/connections (can be binary). Members of
 * matrix stored beyond a certain point (ex. resources, stored at ends of matrix) cannot interconnect,
 * but all other nodes can connect freely.
 * </p>
 * @author Natalie Davis, Gary Polhill
 */
public class Matrix {

    /**
     * <!-- matrix -->
     * The matrix of node connections. Can be binary.
     */
    private final Map<Integer, Map<Integer, Double>> matrix;

    /**
     * <!-- size -->
     * The number of entries. This is <i>not</i> the same as <code>nRow * nCol</code>
     * since the matrix is square, so <code>nRow * nCol</code> would double-count entries
     */
    private final long size;

    /**
     * <!-- nRow -->
     * The number of rows
     */
    private final int nRow;

    /**
     * <!-- nCol -->
     * The number of columns (should equal the number of columns, but separated for readability)
     */
    private final int nCol;

    /**
    * <!-- nConnectables -->
    *  The number of nodes that can connect to/be connected to by any other type of node, in a
    *  randomly-generated network.
    *  Ex. if nConnectables = nConsumers + nBranchPoints, then consumers can connect to consumers, branch points,
    *  or resources, and branch points can connect to consumers, branch points, or resources, but resources
    *  cannot connect to each other
     */
    private final int nConnectables;

    /**
     * <!-- entryRange -->
     * A range to specify for link strengths in a randomly-generated network. Currently fixed at [1.0, 1.0]
     * in the MainController() class but this could easily be changed to explore random link strengths.
     */
    private final Interval<Double> entryRange;

    /**
     * <!-- noConnection -->
     * Placeholder value for if there is no connection between two nodes.
     */
    private final double noConnection;

    /**
     * <!-- pNoConnection -->
     * Probability of no connection, used to construct matrix for random networks.
     */
    private final double pNoConnection;

    /**
     * <!-- probabilityInterval -->
     * Used to test against pNoConnection value in generating a random network.
     */
    private static final Interval<Double> probabilityInterval = new Interval<>(
            0.0, 1.0, true, false
    );

    /**
     * <!-- Matrix constructor -->
     *
     * Constructor for a matrix data structure. Since the matrix is assumed to be
     * symmetric, then it is expected to be square, and some of the elements will
     * be ignored. The constructor will fail if the size or effective size of the
     * matrix is zero, or if any entries are <code>null</code>. For symmetric
     * matrices, it will check ij as well as ji cells before failing for a
     * <code>null</code> entry.
     *
     * @param matrix
     *          2D array containing matrix entries; all rows in the 2D array must
     *          have the same number of columns.
     * @param entryRange
     *          Range for connection (link) strength between nodes.
     * @param noConnection
     *          Placeholder value for if there is no connection between two nodes.
     * @param pNoConnection
     *          Probability of no connection, used to construct matrix for random networks.
     * @param nConnectables
     *          The number of nodes that can connect to/be connected to by any other type of node, in a
     *          randomly-generated network.
     */
    public Matrix(Double[][] matrix, Interval<Double> entryRange,
                  double noConnection, double pNoConnection, int nConnectables) {

        // Initialise state variables
        this.nRow = matrix.length;
        this.nCol = matrix[0].length;
        this.nConnectables = nConnectables;
        this.noConnection = noConnection;
        this.pNoConnection = pNoConnection;
        this.entryRange = entryRange;

        if (this.nRow != this.nCol) {
            throw new IllegalArgumentException(
                    "Matrix expects symmetric, square matrix, but provided matrix has " + this.nRow +
                            " rows, and " + this.nCol + " columns)");
        }

        // Build the matrix as a hashmap of integer to integer-double map
        long size = 0;
        Map<Integer, Map<Integer, Double>> matrix_map = new HashMap<>();
        for (int i = 0; i < matrix.length; i++) {
            // Check that each row has the same number of columns as the first row
            if (this.nCol != matrix[i].length) {
                throw new IllegalArgumentException(
                        "Matrix expects a matrix with a consistent number of columns for each row. Row 0 has "
                                + this.nCol
                                + " columns, row " + i + " has " + matrix[i].length + " columns");
            }

            for (int j = i + 1; j < matrix[i].length; j++) {
                // Check for entry at this node pair in both possible array locations
                Double entry = matrix[i][j] == null ? matrix[j][i] : matrix[i][j];
                // Create new key if current key doesn't exist
                if (! matrix_map.containsKey(i)) {
                    matrix_map.put(i, new HashMap<>());
                }
                // Put not-null entry or noConnection placeholder into matrix
                matrix_map.get(i).put(j, (entry == null ? noConnection : entry));
                size++;
            }
        }
        this.matrix = matrix_map;
        this.size = size;
    }

    /**
     * <!-- Matrix constructor -->
     *
     * Private constructor with a matrix of connections passed in as a map.
     *
     * @param matrix
     *          matrix as a map, can be sparse
     * @param nRow
     *          number of rows in the matrix
     * @param nCol
     *          number of columns in the matrix
     * @param size
     *          number of entries in the matrix
     * @param entryRange
     *          range for entries in the matrix
     */
    private Matrix(Map<Integer, Map<Integer, Double>> matrix, int nRow, int nCol, int nConnectables, long size,
                   Interval<Double> entryRange, double noConnection,
                   double pNoConnection) {

        // Initialise instance variables
        this.nRow = nRow;
        this.nCol = nCol;
        this.nConnectables = nConnectables;
        this.size = size;
        this.noConnection = noConnection;
        this.pNoConnection = pNoConnection;
        this.entryRange = entryRange;

        // Build matrix map from sparse array - doesn't assume array is stored in most efficient way
        Map<Integer, Map<Integer, Double>> matrix_map = new HashMap<>();

        for (int i = 0; i < nRow; i++) {
            for (int j = i + 1; j < nCol; j++) {
                if (! matrix_map.containsKey(i)) {
                    matrix_map.put(i, new HashMap<>());
                }
                if (matrix.get(i).containsKey(j)) {
                    matrix_map.get(i).put(j, matrix.get(i).get(j));
                // Check if ij entry exists in ji cell - move to ij
                } else if (matrix.containsKey(j)) {
                    if (matrix.get(j).containsKey(i)) { // split from else-if above to prevent null pointers
                        matrix_map.get(i).put(j, matrix.get(j).get(i));
                    }
                } else {
                    matrix_map.get(i).put(j, noConnection);
                }
            }
        }
        this.matrix = matrix_map;
    }

    /**
     * <!-- nRow -->
     * @return The number of rows in the matrix (should be same as number of columns)
     */
    public int nRow() {
        return nRow;
    }

    /**
     * <!-- nCol -->
     * @return The number of columns in the matrix (should be same as number of rows)
     */
    public int nCol() {
        return nCol;
    }

    /**
     * <!-- getEntryRange -->
     * @return the entry range (minimum and maximum link strength, currently bounded at [1.0, 1.0]
     * unless manually-specified)
     */
    public Interval<Double> getEntryRange() { return entryRange; }

    /**
     * <!-- entryAt -->
     * @param row the row number in the matrix
     * @param col the column number in the matrix
     * @return the entry at a cell.
     * @throws IllegalArgumentException if row or column is out of bounds
     */
    public double entryAt(int row, int col) {
        if (row < 0 || row >= nRow) {
            throw new IllegalArgumentException("Row " + row + " not in matrix with " + nRow
                    + " rows (indexes start at 0");
        }
        if (col < 0 || col >= nCol) {
            throw new IllegalArgumentException("Column " + col + " not in matrix with " + nCol
                    + " columns (indexes start at 0");
        }
        if (row == col) {
            throw new IllegalArgumentException("Access to row " + row + " = column " + col +
                    " in non-reflexive matrix");
        }
        if (row > col) {
            int tmp = row;
            row = col;
            col = tmp;
        }
        if (! matrix.containsKey(row)) { return noConnection; }
        return (matrix.get(row).containsKey(col) ? matrix.get(row).get(col) : noConnection);
    }

    /**
     * <!-- connected -->
     * @param row the row number in the matrix
     * @param col the column number in the matrix
     * @return <code>true</code> if the row and column nodes are directly
     *         connected
     */
    public boolean connected(int row, int col) {
        return entryAt(row, col) != noConnection;
    }

    /**
     * <!-- createRandom -->
     * @param entryRange The range of strengths at which to connect the nodes
     * @param nrow The number of rows
     * @param ncol The number of columns
     * @param nConnectables The number of "free agents" in the network (can connect to any other nodes)
     * @param noConnection The value to set the strength to if nodes are not connected
     * @param pNoConnection The probability with which two nodes are not connected
     * @return random Matrix where nodes are connected with probability 1 - pNoConnection, at a strength
     * within [0, entryRange]
     */
    public static Matrix createRandom(Interval<Double> entryRange, int nrow, int ncol, int nConnectables,
                                      double noConnection, double pNoConnection) {
        Double[][] randomised = new Double[nrow][ncol];

        for (int i = 0; i < nrow; i++) {
            for (int j = i + 1; j < ncol; j++) {
                if (i >= nConnectables && j >= nConnectables) {
                    randomised[i][j] = noConnection;
                } else {
                    if (probabilityInterval.getDoubleUniformSample() < pNoConnection) {
                        randomised[i][j] = noConnection;
                    } else {
                        randomised[i][j] = entryRange.getDoubleUniformSample();
                    }
                }
            }
        }
        return new Matrix(randomised, entryRange, noConnection, pNoConnection, nConnectables);
    }

    /**
     * <!-- specifyManualMatrix -->
     * Create new Matrix from file specifications - can be sparse. Assumes symmetric, non-reflexive matrix.
     * @param matrixFile The name of the file that contains the matrix of connections - must have headings
     *                   'from', 'to', 'strength'
     * @return a connections Matrix as specified in the file
     * @throws IOException
     */
    public static Matrix specifyManualMatrix(String matrixFile, int nConnectables, int totalnodes,
                                             Interval<Double> entryRange, double noConnection, double pNoConnection) throws IOException {

        BufferedReader fp = FileOpener.read(matrixFile);

        String headingline = fp.readLine();
        if(headingline == null) {
            throw new IOException("No text in Matrix file " + matrixFile);
        }
        String[] headings = headingline.split(",");

        if (headings.length != 3) {
            throw new IllegalArgumentException("Heading string expected to be of length 3: from, to, strength");
        }

        String line;
        Map<Integer, Map<Integer, Double>> matrix_map = new HashMap<>();
        int size = 0;
        // Read in connections stored in file
        while((line = fp.readLine()) != null) {
            String[] cells = line.split(",");

            // Store origin node as key
            matrix_map.putIfAbsent(Integer.parseInt(cells[0]), new HashMap<>());
            // Store destination node and strength
            matrix_map.get(Integer.parseInt(cells[0])).put(Integer.parseInt(cells[1]),
                    Double.parseDouble(cells[2]));
            size++;

        }
        fp.close();

        // Loop through matrix and check if empty ij pairs exist in ji cells, else fill with no-connection values
        for (int i = 0; i < totalnodes; i++) {
            matrix_map.putIfAbsent(i, new HashMap<>());
            for (int j = 0; j < totalnodes; j++) {
                if (i != j) {
                    if (! matrix_map.get(i).containsKey(j)) {
                        if (matrix_map.containsKey(j) && ! matrix_map.get(j).containsKey(i)) {
                            matrix_map.get(i).put(j, noConnection);
                        } else if (matrix_map.containsKey(j) && matrix_map.get(j).containsKey(i)) {
                            matrix_map.get(i).put(j, matrix_map.get(j).get(i));
                            size++; // only increment size here so as not to double-count
                        }
                    }
                    if (matrix_map.containsKey(j)) {
                        if (! matrix_map.get(j).containsKey(i)) {
                            if (matrix_map.containsKey(i) && ! matrix_map.get(i).containsKey(j)) {
                                matrix_map.get(j).put(i, noConnection);
                            } else if (matrix_map.containsKey(i) && matrix_map.get(i).containsKey(j)) {
                                matrix_map.get(j).put(i, matrix_map.get(i).get(j));
                            }
                        }
                    }
                }
            }
        }

        return new Matrix(matrix_map, matrix_map.keySet().size(), matrix_map.keySet().size(),
                nConnectables, size, entryRange, noConnection, pNoConnection);
    }

    /**
     * <!-- size -->
     * @return the size of the network - number of unique entries, not nRow * nCol
     */
    public long size() {
        return size;
    }

    /**
     * <!-- asCSVObjectArray -->
     * @param branchPoints the array of branch points in the connections matrix
     * @param resources the array of resources in the connections matrix
     * @param consumers the array of consumers in the connections matrix
     * @return the connections matrix stored as a CSV-writeable object
     */
    public CSVObject[] asCSVObjectArray(Coordinates[] branchPoints, Resource[] resources,
                                        Coordinates[] consumers) {
        LinkedList<CSVObject> list = new LinkedList<>();

        for (int i = 0; i < nRow; i++) {
            double[] i_loc = (i < consumers.length ? consumers[i].getCoords() :
                    (i - consumers.length < branchPoints.length ? branchPoints[i - consumers.length].getCoords() :
                    resources[i - branchPoints.length - consumers.length].getLocation()));

            for (int j = 0; j < nCol; j++) {
                if (i == j) {
                    continue;
                }

                final double value = entryAt(i, j);

                if (value != noConnection) {
                    double[] j_loc = (j < consumers.length ? consumers[j].getCoords() :
                            (j - consumers.length < branchPoints.length ? branchPoints[j - consumers.length].getCoords() :
                            resources[j - branchPoints.length - consumers.length].getLocation()));

                    final double distance = MaxPowerParam.getTopology().distanceParam(i_loc, j_loc);

                    list.add(new CSVObjectEntry(i + 1, j + 1, value, distance));
                }
            }
        }
        return list.toArray(new CSVObject[0]);
    }

    /**
     * Private CSVObjectEntry class, for creating CSV objects
     */
    private class CSVObjectEntry implements CSVObject {

        private final String data;

        public CSVObjectEntry(int from, int to, double strength, double length) {
            data = Integer.toString(from) + "," + Integer.toString(to) + "," + Double.toString(strength) + ","
                    + Double.toString(length);
        }

        public String getCSVdata() {
            return data;
        }

        public String getCSVheadings() {
            return "from,to,strength,length";
        }
    }

}

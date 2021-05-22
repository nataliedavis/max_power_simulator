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
*/
import org.ejml.factory.SingularMatrixException;
import org.ejml.simple.SimpleMatrix;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * <!-- MaxPowerSimulator -->
 * <p>
 * Class of methods to calculate flows and power consumption in a network, using a modified version of the
 * Newton Raphson method for load flow analysis in electrical networks. Iteratively calculates the consumption
 * of a network with incrementing levels of current flow specification. Also calculates total length of network
 * (sum of length of all links). All output is written to a CSV file.
 * </p>
 * @author Natalie Davis
 */

public class MaxPowerSimulator {

    private String output_csv;
    private SimpleMatrix buses;
    private final Topology topology;
    protected final double[] param;
    private boolean useStrength;
    private final double strengthExponent;
    private double totalLength;

    public MaxPowerSimulator(Topology topology, double[] param, boolean useStrength, double strengthExponent,
                             String output_csv) {
        this.topology = topology;
        this.param = param;
        this.useStrength = useStrength;
        this.strengthExponent = strengthExponent;
        this.output_csv = output_csv;
        this.totalLength = 0.0;
    }

    /**
     * <!-- simulateMaxPower -->
     * @param network the Network for which to calculate the power consumption with different quantities of current flow
     */
    protected void simulateMaxPower(Network network) {

        // Write headers to output CSV
        List<String> csv_headings = new ArrayList<>();
        csv_headings.add("current,");
        csv_headings.add("length,");
        for (int i = 0; i < network.getResources().length; i++) {
            csv_headings.add("power_at_resource_" + i + ",");
        }
        for (int i = 0; i < network.getResources().length; i++) {
            csv_headings.add("voltage_at_resource_" + i + ",");
        }
        for (int a = 0; a < network.getConsumers().length; a++) {
            csv_headings.add("power_at_consumer_" + a + ",");
        }
        for (int a = 0; a < network.getConsumers().length; a++) {
            csv_headings.add("voltage_at_consumer_" + a + ",");
        }
        csv_headings.add("total_power_consumption" + "\n"); // last entry - end line

        // Calculate total link length only once since it's fixed for duration of simulations
        this.totalLength = getTotalLinkLength(network);

        // Incrementally increase current flow through network and calculate consumption
        List<List<Number>> csv_data = new ArrayList<>();
        for (double c = 1; c <= 1000; c += 0.1) {
            double power_used = recordPowerConsumption(network, c, csv_data);
            // End loop early if overall network power goes negative
            if (power_used < 0) { break; }
        }
        try {
            // Write all output to CSV
            PrintWriter pw = util.FileOpener.write(output_csv);
            StringBuilder csv_heading_sb = new StringBuilder();
            for (String heading: csv_headings) {
                csv_heading_sb.append(heading);
            }
            pw.write(csv_heading_sb.toString());
            for (List<Number> data_row : csv_data) {
                for (int j = 0; j < data_row.size() - 1; j++) {
                    pw.write(String.valueOf(data_row.get(j)));
                    pw.write(",");
                }
                // End comma-appending loop early and write last row entry outside
                //      so as not to write a comma after the last value - add newline instead
                pw.write(String.valueOf(data_row.get(data_row.size() - 1)));
                pw.write("\n");
            }
            pw.write("\n");
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * <!-- getTotalLinkLength -->
     * @param network The network for which to calculate the link cost
     * @return a number representing the sum of all link lengths (times their strength, raised to the
     * denoted exponent, if specified) in the network
     */
    private double getTotalLinkLength(Network network) {
        Coordinates[] consumers = network.getConsumers();
        Coordinates[] branchPoints = network.getBranchPoints();
        Resource[] resources = network.getResources();
        Matrix matrix = network.getMatrix();
        double tLength = 0.0;

        for (int i = 0; i < matrix.nRow(); i++) {
            // Determine type of node in row counter and get coordinates for it
            double[] i_coord = (i < consumers.length ? consumers[i].getCoords() :
                    (i - consumers.length < branchPoints.length ? branchPoints[i - consumers.length].getCoords() :
                            resources[i - branchPoints.length - consumers.length].getLocation()));
            for (int j = i + 1; j < matrix.nRow(); j++) {
                // Loop through all columns for this row (nRow and nCol should be the same) - use i+1 to avoid double-counting
                if (matrix.connected(i, j)) {
                    // Determine type of node in column counter and get coordinates for it
                    double[] j_coord = (j < consumers.length ? consumers[j].getCoords() :
                            (j - consumers.length < branchPoints.length ? branchPoints[j - consumers.length].getCoords() :
                                    resources[j - branchPoints.length - consumers.length].getLocation()));
                    // Calculate spatial distance between nodes and add to running total
                    tLength += topology.distance(i_coord, j_coord);
                }
            }
        }
        return tLength;
    }

    /**
     * <!-- makeConductanceMatrix -->
     * @param network the network for which to make the conductance matrix
     * @param useStrength whether or not to use strength in the calculation of the conductance
     *                    (inverse of resistance) for each link
     * @return a matrix representing the conductance of the connections between each node in the network
     */
    private SimpleMatrix makeConductanceMatrix(Network network, boolean useStrength) {
        Matrix matrix = network.getMatrix();
        Resource[] resources = network.getResources();
        Coordinates[] branchPoints = network.getBranchPoints();
        Coordinates[] consumers = network.getConsumers();

        int numElem = resources.length + branchPoints.length + consumers.length;
        SimpleMatrix conductance = new SimpleMatrix(numElem, numElem);

        // Loop through all node connections to calculate conductance between them
        for (int i = 0; i < matrix.nRow(); i++) {
            // Determine type of node i
            double[] i_coord = (i < consumers.length ? consumers[i].getCoords() :
                    (i - consumers.length < branchPoints.length ? branchPoints[i - consumers.length].getCoords() :
                            resources[i - branchPoints.length - consumers.length].getLocation()));
            for (int j = 0; j < matrix.nRow(); j++) {
                if (i == j) {
                    // No connection, skip this node
                    continue;
                }
                if (matrix.connected(i, j)) {
                    // Determine type of node j
                    double[] j_coord = (j < consumers.length ? consumers[j].getCoords() :
                            (j - consumers.length < branchPoints.length ? branchPoints[j - consumers.length].getCoords() :
                                    resources[j - branchPoints.length - consumers.length].getLocation()));
                    // Calculate spatial distance between nodes i and j
                    double length = topology.distance(i_coord, j_coord, param);
                    if (useStrength) {
                        // Conductance = strength ^ exp / length
                        conductance.set(i, j, Math.pow(matrix.entryAt(i, j), strengthExponent) / length);
                        conductance.set(j, i, Math.pow(matrix.entryAt(i, j), strengthExponent) / length);
                    } else {
                        // Conductance = 1 / length
                        conductance.set(i, j, 1 / length);
                        conductance.set(j, i, 1 / length);
                    }
                }
            }
        }

        // Fill in self-conductance - negative of sum of all conductances for this node
        for (int i = 0; i < conductance.numCols(); i++) {
            conductance.set(i, i,conductance.extractVector(true, i).elementSum() * -1);
        }

        return conductance;
    }

    /**
     * <!-- modifiedNewtonRaphson -->
     *     Uses Newton Raphson method for calculating power flows to calculate voltages/currents for each
     *     node and link in the network
     * @param conductance the conductance matrix specifying node connectivity
     * @param nConsumers the number of consumers (resource users) in the conductance matrix
     * @param resources the resources array
     * @param currentSpec the (current) specification at the consumer - passed in as positive but changed to negative
     *                   since it's a demand
     */
    private void modifiedNewtonRaphson(SimpleMatrix conductance, int nConsumers, Resource[] resources,
                                    double currentSpec) {
        double tolerance = 0.001;
        int max_iter = 10; // It should converge in 2 but in case it doesn't, this keeps us from looping forever
        int nBuses = conductance.numRows();
        int nRes = resources.length;
        int nLoads = nBuses - nRes;

        // col 0: current demand (negative) or supply (positive)
        // col 1: voltage - estimate for consumers and branch points, will be adjusted
        buses = new SimpleMatrix(nBuses, 2);
        for (int i = 0; i < nLoads; i++) {
            if (i >= nConsumers) {
                // Set up buses data for branch points - no demand
                buses.set(i, 0, 0);
                buses.set(i, 1, 1);
            } else {
                // Set up buses data for consumers
                buses.set(i, 0, -1 * currentSpec);
                buses.set(i, 1, 1.0);
            }
        }
        // Set up buses data for resources
        // col 0: initial estimate for current flow, will be changed by equation solution
        // col 1: voltage, fixed for simulation
        for (int i = nLoads; i < nLoads + nRes; i++) {
            buses.set(i, 0, 100);
            buses.set(i, 1, resources[i - nLoads].getVoltage());
        }

        // jacobian - Modified Jacobian matrix - nL*nL matrix of partial derivatives of I w.r.t V
        SimpleMatrix jacobian = new SimpleMatrix(nLoads, nLoads);
        for (int i = 0; i < nLoads; i++) {
            for (int j = 0; j < nLoads; j++) {
                jacobian.set(i, j, conductance.get(i,j));
            }
        }

        // calcCurrent - nL*1 vector of voltage at each load, calculated based on voltage at source, resistance
        SimpleMatrix calcCurrent = new SimpleMatrix(nLoads, 1);

        // mismatchI - nL*1 vector of difference between I (calculated) and I (demanded)
        SimpleMatrix mismatchI = new SimpleMatrix(nLoads, 1);

        int iter = 0;
        while (iter < max_iter) {

            // Calculate current at each bus based on initial estimates
            for (int i = 0; i < nLoads; i++) {
                // First, get current for that bus - sum of conductance * voltages
                double curr = 0.0;
                for (int j = 0; j < nBuses; j++) {
                    curr += (conductance.get(i, j) * (buses.get(i, 1) - buses.get(j, 1)));
                }
                calcCurrent.set(i, curr);
            }

            // Calculate current mismatch between above and needed (given in bus data matrix)
            for (int i = 0; i < nLoads; i++) {
                mismatchI.set(i, (calcCurrent.get(i) - buses.get(i, 0)));
            }

            // Solve for deltaV - voltage change
            SimpleMatrix deltaV = jacobian.negative().solve(mismatchI);

            // Check if mismatches are below tolerance - if so, break, else update bus data with deltaVs and restart
            boolean restart = false;
            for (int i = 0; i < nLoads; i++) {
                if (Math.abs(mismatchI.get(i)) > tolerance) {
                    // Update bus data with deltaVs and restart
                    double newVolt = buses.get(i, 1) * (1 - deltaV.get(i));
                    buses.set(i, 1, newVolt);
                    restart = true;
                }
            }
            if (restart) iter++; else break;
        }
    }

    /**
     * <!-- recordPowerConsumption -->
     *
     * @param network the network for which to calculate the flow/losses
     * @param c the current specification for each consumer
     * @param csv_data the data structure to hold the data for printing as CSV output
     * @return total power used of the network
     */
    private double recordPowerConsumption(Network network, double c, List<List<Number>> csv_data) {
        Resource[] resources = network.getResources();
        SimpleMatrix conductance = makeConductanceMatrix(network, useStrength);

        // Run modified Newton Raphson to get voltage profile and current along each link
        try {
            modifiedNewtonRaphson(conductance, network.getConsumers().length, resources, c);
        } catch (SingularMatrixException e) {
            // Impossible network, return -1 to end simulation
            System.err.println(e.getMessage());
            return -1;
        }

        List<Number> csv_data_row = new ArrayList<>();
        csv_data_row.add(c);    // Store current in CSV
        csv_data_row.add(totalLength);  // Store length in CSV - already set in simulateMaxPower() above since it's
                                        // fixed for duration of simulation

        // Store power and voltage at each resource in the CSV
        int nLoads = network.getConsumers().length + network.getBranchPoints().length; // Non-resource points, or 'loads'
        // Loop through resources (stored at end of matrix) to calculate current outflow
        for (int i = nLoads; i < nLoads + resources.length; i++) {
            double curr = 0.0;
            // Only loop through resource -> load connections since resources cannot connect to one another
            for (int j = 0; j < nLoads; j++) {
                // Current = conductance * voltage difference
                curr += conductance.get(i, j) * (buses.get(i, 1) - buses.get(j, 1));
            }
            buses.set(i, 0, curr); // Store final current - used if calculating overdrawn resources later
            csv_data_row.add(curr * resources[i - nLoads].getVoltage()); // Store resource power in CSV
        }
        for (int i = nLoads; i < nLoads + resources.length; i++) {
            csv_data_row.add(resources[i - nLoads].getVoltage()); // Store resource voltage in CSV
        }

        // Calculate total network power consumption: sum of product of current (absolute value since it's stored as
        //      negative/demand) and voltage at each consumer
        double totalPowerUsed = 0.0;
        for (int i = 0; i < network.getConsumers().length; i++) {
            network.getPotentials().add(buses.get(i, 1));
            csv_data_row.add(buses.get(i, 1) * c); // Store consumer power in CSV
            totalPowerUsed += Math.abs(buses.get(i, 0)) * buses.get(i, 1);
        }

        // Store consumer voltage in CSV - separate from above loop since all consumer voltages are grouped in CSV
        for (int i = 0; i < network.getConsumers().length; i++) {
            csv_data_row.add(buses.get(i, 1)); // Store consumer voltage in CSV
        }

        // Store total power consumption in CSV, add data row from this round of the simulation to CSV data
        csv_data_row.add(totalPowerUsed);
        csv_data.add(csv_data_row);

        return totalPowerUsed;
    }
}

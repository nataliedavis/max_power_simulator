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

Author: Natalie Davis
*/

import java.io.IOException;
import util.Interval;
import java.util.LinkedList;
import java.util.List;

/**
 * <!-- MainController -->
 * <p>
 *     Initiates and controls the set-up and running of the Max Power Simulator.
 *     Requires a CSV parameter file with all network construction and topological
 *     parameters.
 * </p>
 * @author Natalie Davis
 */

public class MainController {

   public static void main(String[] args) {
       // Require parameter file for set up
        if (args.length != 1) {
            System.err.println("Usage: MainController <parameter file>");
            System.exit(0);
        }
        MaxPowerParam.readParam(args[0]);

        // Read in resources file - specified in parameter file
        Resource[] resources = null;
        try {
            resources = Resource.readResources(MaxPowerParam.getResourcesFile());
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Set up network based on specifications in parameter file
        Network network;
        if (MaxPowerParam.getManualNetwork()) {
            // Manual simulations mean that the consumer locations and all links between nodes are
            // pre-specified in CSVs that are named in the parameter file
            network = setUpManualSimulation(resources);
        } else {
            // Random simulations mean that the links, and possibly consumer locations, are randomly
            // generated based on the specified topology size and probability of connection
            network = setUpRandomSimulation(resources);
        }

        // Set up simulator and run
        MaxPowerSimulator simulator = new MaxPowerSimulator(MaxPowerParam.getTopology(),
                MaxPowerParam.getTopologyParamArray(MaxPowerParam.getTopology()), MaxPowerParam.getUseStrength(),
                MaxPowerParam.getStrengthExponent(), MaxPowerParam.getOutputCSV());

        simulator.simulateMaxPower(network);
        System.exit(0);
    }

    /**
     * <!-- setUpRandomSimulation -->
     * <p>
     *     Set up multi-consumer Network using parameters specified
     *     in the parameter file. The consumers can have specified locations, but
     *     can also be placed randomly in the topology as defined by the maximum
     *     coordinates in the parameter file. The links between nodes are all randomly
     *     assigned based on the probability of connection specified in the parameter file.
     * </p>
     * @param resources The resource array used in this simulation
     * @return A Network: customised data structure for storing node locations,
     *      connections matrix, and network metadata
     */
    private static Network setUpRandomSimulation(Resource[] resources) {
        // Number of branch points - can be 0
        int nBranchPoints = MaxPowerParam.getnBranchPoints();
        // Number of consumers to place (if random) or to read in (if specified)
        int nConsumers = MaxPowerParam.getnConsumers();
        // Currently the links are all unit-strength for random networks, but this could be
        //  easily expanded by adding a parameter for a maximum link strength, and including that
        //  as the maximum here
        Interval<Double> entryRange = new Interval<>(1.0, 1.0);

        // Set up consumers
        Coordinates[] consumers = new Coordinates[nConsumers];
        List<double[]> consumerCoords = new LinkedList<>();
        if (MaxPowerParam.getRandomConsumers()) {
            // Randomly-placed consumers
            for (int j = 0; j < nConsumers; j++) {
                consumers[j] = new Coordinates(MaxPowerParam.getTopology().getOrdinateRangesParam(),
                        MaxPowerParam.getTopology().getRandomCoordinates());
            }
        } else {
            // Manually-specified consumer locations but random connectivity
            try {
                consumerCoords = MaxPowerParam.getTopology().readCoords(MaxPowerParam.getConsumersFile(),
                        nConsumers);
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (int j = 0; j < nConsumers; j++) {
                consumers[j] = new Coordinates(MaxPowerParam.getTopology().getOrdinateRangesParam(),
                        consumerCoords.get(j));
            }
        }

        // Set up branch points - random coordinates
        Coordinates[] branchPoints = new Coordinates[nBranchPoints];
        for (int j = 0; j < nBranchPoints; j++) {
            branchPoints[j] = new Coordinates(MaxPowerParam.getTopology().getOrdinateRangesParam(),
                    MaxPowerParam.getTopology().getRandomCoordinates());
        }

        // Set up connections matrix
        // Consumers and branch points can connect to anything - resources cannot connect to each other
        //      because there would be no flow between them if they are the same force anyways
        int totalNodes = nConsumers + nBranchPoints + resources.length;
        int nConnectables = nConsumers + nBranchPoints;
        Matrix matrix = Matrix.createRandom(entryRange,
                totalNodes, totalNodes, nConnectables, MaxPowerParam.getNoConnection(),
                MaxPowerParam.getpNoConnection());

        return new Network(branchPoints, matrix, resources, consumers);
    }

    /**
     * <!-- setUpManualSimulation -->
     * <p>
     *      Set up population of NetworkChromosomes using a manually-specified
     *      network structure.
     * </p>
     * @return MultiHiveNetworkChromosome[] population
     */
    private static Network setUpManualSimulation(Resource[] resources) {
        // Read in consumers from CSV file
        int nConsumers = MaxPowerParam.getnConsumers();
        Coordinates[] consumers = new Coordinates[MaxPowerParam.getnConsumers()];
        List<double[]> consumerCoords = new LinkedList<>();
        try {
            consumerCoords = MaxPowerParam.getTopology().readCoords(MaxPowerParam.getConsumersFile(),
                    MaxPowerParam.getnConsumers());
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int j = 0; j < nConsumers; j++) {
            consumers[j] = new Coordinates(MaxPowerParam.getTopology().getOrdinateRangesParam(),
                    consumerCoords.get(j));
        }

        // Read in branch points from CSV file
        int nBranchPoints = MaxPowerParam.getnBranchPoints();
        Coordinates[] branchPoints = new Coordinates[nBranchPoints];
        List<double[]> branchPointCoords = new LinkedList<>();
        try {
            branchPointCoords = MaxPowerParam.getTopology().readCoords(MaxPowerParam.getBranchPointsFile(),
                    nBranchPoints);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int j = 0; j < nBranchPoints; j++) {
            branchPoints[j] = new Coordinates(MaxPowerParam.getTopology().getOrdinateRangesParam(),
                    branchPointCoords.get(j));
        }

        // Set up connections matrix
        Matrix matrix = null;
        try {
            // Consumers and branch points can connect to anything - resources cannot connect to each other
            //      because there would be no flow between them if they are the same voltage anyways
            Interval<Double> entryRange = new Interval<>(1.0, 1.0);
            int totalNodes = nConsumers + nBranchPoints + resources.length;
            int nConnectables = nConsumers + nBranchPoints;
            // Build matrix from CSV file specifying which nodes are connected
            matrix = Matrix.specifyManualMatrix(MaxPowerParam.getMatrixFile(),
                    nConnectables, totalNodes, entryRange, MaxPowerParam.getNoConnection(),
                    MaxPowerParam.getpNoConnection());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Network(branchPoints, matrix, resources, consumers);
    }

}

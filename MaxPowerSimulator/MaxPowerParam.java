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

import java.io.*;

/**
 * <!-- MaxPowerParam -->
 * <p>
 *      Reads in and sets parameters for running the maximum power simulator.
 *      Includes accessors/modifiers for all network construction and topological
 *      parameters.
 * </p>
 * @author Natalie Davis, Gary Polhill
 */
public final class MaxPowerParam {

    private static MaxPowerParam i = new MaxPowerParam();

    // Main set-up parameters
    private String resourcesFile;
    private String branchPointsFile;
    private String consumersFile;
    private String matrixFile;
    private boolean manualNetwork;

    // Connections matrix construction
    private int nConsumers;
    private int nBranchPoints;
    private boolean randomConsumers;
    private double pNoConnection;
    private double noConnection;

    // Topology-related parameters
    private Topology topology;
    private double[] planeMaxCoords;
    private double sphereR;

    // Used in MaxPowerSimulator
    private boolean useStrength;
    private double strengthExponent;
    private String outputCSV;

    /**
     * <!-- MaxPowerParam constructor -->
     *
     */
    public MaxPowerParam() {
        // Deliberately empty
    }

    //***** Set up *****//
    public static String getResourcesFile() {
        if(i == null) {
            throw new RuntimeException("Unidentified parameter constructor");
        }
        return i.resourcesFile;
    }
    public static void setResourcesFile(String resourcesFile) {
        i.resourcesFile = resourcesFile;
    }

    public static String getBranchPointsFile() {
        if(i == null) {
            throw new RuntimeException("Unidentified parameter constructor");
        }
        return i.branchPointsFile;
    }
    public static void setBranchPointsFile(String branchPointsFile) { i.branchPointsFile = branchPointsFile; }

    public static String getConsumersFile() {
        if(i == null) {
            throw new RuntimeException("Unidentified parameter constructor");
        }
        return i.consumersFile;
    }
    public static void setConsumersFile(String consumersFile) { i.consumersFile = consumersFile; }

    public static String getMatrixFile() {
        if(i == null) {
            throw new RuntimeException("Unidentified parameter constructor");
        }
        return i.matrixFile;
    }
    public static void setMatrixFile(String matrixFile) { i.matrixFile = matrixFile; }

    public static boolean getManualNetwork() {
        if(i == null) {
            throw new RuntimeException("Unidentified parameter constructor");
        }
        return i.manualNetwork;
    }
    public static void setManualNetwork(boolean manualNetwork) { i.manualNetwork = manualNetwork; }

    //***** Matrix construction *****//
    public static int getnConsumers() {
        if (i == null) {
            throw new RuntimeException("Unidentified parameter constructor");
        }
        return i.nConsumers;
    }
    public static void setnConsumers(int nConsumers) {
        i.nConsumers = nConsumers;
    }

    public static int getnBranchPoints() {
        if (i == null) {
            throw new RuntimeException("Unidentified parameter constructor");
        }
        return i.nBranchPoints;
    }
    public static void setnBranchPoints(int nBranchPoints) { i.nBranchPoints = nBranchPoints; }

    public static boolean getRandomConsumers() {
        if (i == null) {
            throw new RuntimeException("Unidentified parameter constructor");
        }
        return i.randomConsumers;
    }
    public static void setRandomConsumers(boolean randomConsumers) { i.randomConsumers = randomConsumers; }

    public static double getNoConnection() {
        if(i == null) {
            throw new RuntimeException("Unidentified parameter constructor");
        }
        return i.noConnection;
    }
    public static void setNoConnection(double noConnection) {
        i.noConnection = noConnection;
    }

    public static double getpNoConnection() {
        if(i == null) {
            throw new RuntimeException("Unidentified parameter constructor");
        }
        return i.pNoConnection;
    }
    public static void setpNoConnection(double pNoConnection) {
        i.pNoConnection = pNoConnection;
    }

    //***** Topology-related *****//
    public static Topology getTopology() {
        if(i == null) {
            throw new RuntimeException("Unidentified parameter constructor");
        }
        return i.topology;
    }
    public static void setTopology(Topology topology) {
        i.topology = topology;
    }

    public static double[] getTopologyParamArray(Topology topology) {
        if(i == null) {
            throw new RuntimeException("Unidentified parameter constructor");
        }
        switch(topology) {
            case PLANE:
                return new double[0];
            case SPHERE_SURFACE:
            case SPHERE:
                return new double[] { i.sphereR };
            default:
                throw new IllegalArgumentException();
        }
    }

    public static double[] getPlaneMaxCoords() {
        if (i == null) {
            throw new RuntimeException("Unidentified parameter constructor");
        }
        return i.planeMaxCoords;
    }
    public static void setPlaneMaxCoords(double [] planeMaxCoords) { i.planeMaxCoords = planeMaxCoords; }

    public static double getSphereR() {
        if(i == null) {
            throw new RuntimeException("Unidentified parameter constructor");
        }
        return i.sphereR;
    }
    public static void setSphereR(double sphereR) {
        i.sphereR = sphereR;
    }

    public static double getStrengthExponent() {
        if(i == null) {
            throw new RuntimeException("Unidentified parameter constructor");
        }
        return i.strengthExponent;
    }
    public static void setStrengthExponent(double strengthExponent) {
        i.strengthExponent = strengthExponent;
    }

    public static boolean getUseStrength() {
        if(i == null) {
            throw new RuntimeException("Unidentified parameter constructor");
        }
        return i.useStrength;
    }
    public static void setUseStrength(boolean useStrength) { i.useStrength = useStrength; }

    public static String getOutputCSV() {
        if(i == null) {
            throw new RuntimeException("Unidentified parameter constructor");
        }
        return i.outputCSV;
    }
    public static void setOutputCSV(String outputCSV) { i.outputCSV = outputCSV; }

    /**
     * <!-- readParam -->
     * Reads in the parameter file (CSV) and parses each line/sets each parameter
     * @param filename The CSV file containing the parameters
     */
    public static void readParam(String filename) {
        try {
            BufferedReader paramCSV = new BufferedReader(new FileReader(filename));
            String line;
            String[] parts;
            while ((line = paramCSV.readLine()) != null) {
                parts = line.split(",");
                String param = parts[0];
                String val = parts[1];
                switch(param) {
                    case "resourcesFile":
                        setResourcesFile(val);
                        break;
                    case "branchPointsFile":
                        setBranchPointsFile(val);
                        break;
                    case "consumersFile":
                        setConsumersFile(val);
                        break;
                    case "matrixFile":
                        setMatrixFile(val);
                        break;
                    case "manualNetwork":
                        if (val.equalsIgnoreCase("true") || val.compareTo("1") == 0) {
                            setManualNetwork(true);
                        } else {
                            setManualNetwork(false);
                        }
                        break;
                    case "nConsumers":
                        setnConsumers(Integer.parseInt(val));
                        break;
                    case "nBranchPoints":
                        setnBranchPoints(Integer.parseInt(val));
                        break;
                    case "randomConsumers":
                        if (val.equalsIgnoreCase("true") || val.compareTo("1") == 0) {
                            setRandomConsumers(true);
                        } else {
                            setRandomConsumers(false);
                        }
                        break;
                    case "pNoConnection":
                        setpNoConnection(Double.parseDouble(val));
                        break;
                    case "noConnection":
                        setNoConnection(Double.parseDouble(val));
                        break;
                    case "topology":
                        if (val.equalsIgnoreCase("PLANE")) {
                            setTopology(Topology.PLANE);
                        } else if (val.equalsIgnoreCase("SPHERE")) {
                            setTopology(Topology.SPHERE);
                        } else if (val.equalsIgnoreCase("SPHERE_SURFACE")) {
                            setTopology(Topology.SPHERE_SURFACE);
                        } else {
                            System.out.println("Not a valid topology. Please choose one of the following: PLANE, SPHERE, " +
                                    "SPHERE_SURFACE");
                            System.exit(1);
                        }
                        break;
                    case "planeMaxCoords":
                        String[] planeCoordString = val.split(";");
                        double[] planeCoords = new double[planeCoordString.length];
                        for (int i = 0; i < 2; i++) {
                            planeCoords[i] = Double.parseDouble(planeCoordString[i]);
                        }
                        setPlaneMaxCoords(planeCoords);
                        break;
                    case "sphereR":
                        setSphereR(Double.parseDouble(val));
                        break;
                    case "useStrength":
                        if (val.equalsIgnoreCase("true") || val.compareTo("1") == 0) {
                            setUseStrength(true);
                        } else {
                            setUseStrength(false);
                        }
                        break;
                    case "outputCSV":
                        setOutputCSV(val);
                        break;
                    case "strengthExponent":
                        setStrengthExponent(Double.parseDouble(val));
                        break;

                    default:
                        System.out.println("Invalid parameter: " + param);
                        break;
                }
            }
            paramCSV.close();
        } catch (IOException e) {
            System.out.println("Could not read file " + filename);
        }

        // Check that all topology information is included
        if (getTopology() == Topology.PLANE && getPlaneMaxCoords() == null) {
            System.out.println("Must specify maximum coordinates for plane topology");
            System.exit(1);
        } else if ((getTopology() == Topology.SPHERE || getTopology() == Topology.SPHERE_SURFACE) &&
                getSphereR() == 0) {
            System.out.println("Must specify radius for sphere or sphere surface");
            System.exit(1);
        }



    }
}



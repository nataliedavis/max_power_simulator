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

import util.CSVObject;
import util.FileOpener;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;

/**
 * <!-- Resource -->
 *
 * Class for Resources, which produce power. Resources have a location and a voltage,
 * and are specified in a CSV file that is referenced in the main parameter file.
 *
 * @author Natalie Davis, Gary Polhill
 */
public class Resource implements CSVObject {

    private final double[] location;
    private final double voltage;

    /**
     * Resource constructor
     * @param location the coordinate location of this instance
     * @param voltage the voltage of this instance
     */
    public Resource(double[] location, double voltage) {
        this.location = location;
        this.voltage = voltage;
    }

    /**
     * <!-- getVoltage -->
     * @return voltage for this resource
     */
    public double getVoltage() {
        return voltage;
    }

    /**
     * <!-- getLocation -->
     * @return location of this resource
     */
    public double[] getLocation() { return location; }

    /**
     * <!-- coordSize -->
     * @return the length of coordinates (determines topology dimensionality)
     */
    public int coordSize() { return location.length; }

    /**
     * <!-- getCSVdata -->
     * @return the data required to store the resources as a CSV file
     * @see util.CSVObject#getCSVdata()
     */
    @Override
    public String getCSVdata() {
        StringBuffer buff = new StringBuffer();

        double[] cartesian = MaxPowerParam.getTopology().asCartesian(location);

        for(Double coord: cartesian) {
            buff.append(CSVObject.Tools.getEntry(Double.toString(coord)));
            buff.append(",");
        }

        return buff.toString();
    }


    /**
     * <!-- getCSVheadings -->
     * @return the headings to create a CSV file to store the resources
     */
    public String getCSVheadings() {

        double[] cartesian = MaxPowerParam.getTopology().asCartesian(this.location);

        if(cartesian.length == 1) {
            return "x,potential";
        }
        else if(cartesian.length == 2) {
            return "x,y,potential";
        }
        else if(cartesian.length == 3) {
            return "x,y,z,potential";
        }
        else {
            StringBuilder buff = new StringBuilder();

            for(int i = 1; i <= cartesian.length; i++) {
                buff.append("x");
                buff.append(i);
                buff.append(",");
            }
            buff.append("voltage");

            return buff.toString();
        }
    }

    /**
     * <!-- readResources -->
     *
     * The resources file has a CSV format, in which cartesian coordinates are
     * assumed to be saved. The first n columns are the cartesian coordinates of
     * the location of each resource in n-dimensional space, and the last column is
     * a double measuring the voltage or potential of that resource
     *
     * @param filename The CSV file storing the locations and voltages of the resources
     * @return An array of resources read from the file
     * @throws IOException
     */
    public static Resource[] readResources(String filename) throws IOException {
        LinkedList<Resource> resources = new LinkedList<>();
        BufferedReader fp = FileOpener.read(filename);

        String headingline = fp.readLine();

        // Check that headings are as expected
        if (headingline == null) {
            throw new IOException("No text in Resources file " + filename);
        }
        String[] headings = headingline.split(",");

        if (headings.length < 2) {
            throw new IOException("First line of Resources file " + filename + " expected to have at least 2" +
                    " columns");
        }

        // Can call the last column 'voltage' or 'potential'
        if (! ("voltage".equalsIgnoreCase(headings[headings.length - 1]) ||
                "potential".equalsIgnoreCase(headings[headings.length - 1]))) {
            throw new IOException("Last cell of first (header) line in Resources file expected to be \"voltage\" or \"potential\""
                    + filename);
        }

        // Parse file to store location and voltage of each resource
        String line;
        while((line = fp.readLine()) != null) {
            String[] cells = line.split(",");

            double resource = Double.parseDouble(cells[headings.length - 1]);

            double[] location = new double[headings.length - 1];
            for(int i = 0; i < location.length; i++) {
                location[i] = Double.parseDouble(cells[i]);
            }

            double[] convLocation = MaxPowerParam.getTopology().fromCartesian(location);

            resources.add(new Resource(convLocation, resource));
        }
        fp.close();

        return resources.toArray(new Resource[0]);
    }

}

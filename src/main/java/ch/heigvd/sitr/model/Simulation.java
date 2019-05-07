/*
 * Filename : Simulation.java
 * Creation date : 07.04.2019
 */

package ch.heigvd.sitr.model;

import ch.heigvd.sitr.gui.simulation.Displayer;
import ch.heigvd.sitr.gui.simulation.SimulationWindow;
import ch.heigvd.sitr.map.RoadNetwork;
import ch.heigvd.sitr.map.RoadSegment;
import ch.heigvd.sitr.map.input.OpenDriveHandler;
import ch.heigvd.sitr.vehicle.ItineraryPath;
import ch.heigvd.sitr.vehicle.Vehicle;
import ch.heigvd.sitr.vehicle.VehicleController;
import lombok.Getter;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Simulation class handles all global simulation settings and values
 * The main simulation loop runs here as well
 *
 * @author Luc Wachter, Simon Walther
 */
public class Simulation {
    // The displayable component we need to repaint
    private Displayer window;
    // The scenario of the current simulation
    private ScenarioType scenario;
    // The behaviour the vehicles should have when arriving at their destination
    private VehicleBehaviourType behaviour;
    // List of vehicles generated by traffic generator
    private LinkedList<Vehicle> vehicles;

    // Rate at which the redrawing will happen in milliseconds
    private static final int UPDATE_RATE = 40;

    // Road network
    private final RoadNetwork roadNetwork;

    // The ratio px/m
    @Getter
    private double scale;

    /**
     * Simulation constructor
     *
     * @param scenario    The scenario the simulation must create
     * @param behaviour   The behaviour the vehicles must adopt when arriving at their destination
     * @param controllers The number of vehicles for each controller type
     */
    public Simulation(ScenarioType scenario, VehicleBehaviourType behaviour,
                      HashMap<VehicleControllerType, Integer> controllers) {
        this.scenario = scenario;
        this.scale = scenario.getScale();
        this.behaviour = behaviour;

        // Create a roadNetwork instance and then parse the OpenDRIVE XML file
        roadNetwork = new RoadNetwork();

        // TODO : Remove hard coded openDriveFilename
        parseOpenDriveXml(roadNetwork, "simple_road.xodr");

        // Generate vehicles from user parameters
        vehicles = generateTraffic(controllers);
    }

    /**
     * Main simulation loop, runs in a fixed rate timer loop
     */
    public void loop() {
        // Launch main window
        window = SimulationWindow.getInstance();

        // Schedule a task to run immediately, and then
        // every UPDATE_RATE per second
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // TODO (tum) WTF we shouldn't do that
                // Print the road network
                roadNetwork.draw();

                for (Vehicle vehicle : vehicles) {
                    vehicle.update(0.10);
                    vehicle.draw(scale);
                    // DEBUG
                    System.out.println(vehicle);
                }

                // Callback to paintComponent()
                window.repaint();
            }
        }, 0, UPDATE_RATE);
    }

    /**
     * Generate correct number of vehicle for each specified controller
     *
     * @param controllers The hash map containing the specified number of vehicles for each controller
     * @return a list of all vehicles in the simulation
     */
    private LinkedList<Vehicle> generateTraffic(HashMap<VehicleControllerType, Integer> controllers) {
        LinkedList<Vehicle> vehicles = new LinkedList<>();

        // TODO Manage positions and front vehicles

        LinkedList<ItineraryPath> defaultItinerary = new LinkedList<>();
        Iterator<RoadSegment> roadSegmentIterator = roadNetwork.iterator();

        while(roadSegmentIterator.hasNext()) {
            defaultItinerary.add(new ItineraryPath(roadSegmentIterator.next(), scale));
        }

        // ItineraryPath itineraryPath = new ItineraryPath(new Point2D.Double(7, 49), new Point2D.Double(75, 49));

        // Iterate through the hash map
        for (Map.Entry<VehicleControllerType, Integer> entry : controllers.entrySet()) {
            // One controller for all vehicles of a given type
            VehicleController controller = new VehicleController(entry.getKey());

            // Generate as many vehicles as asked
            for (int i = 0; i < entry.getValue(); i++) {
                // TODO: add last vehicle as front vehicle
                Vehicle v = new Vehicle("regular.xml", controller, defaultItinerary);
                // v.setFrontVehicle(wall);
                vehicles.add(v);
            }
        }

        return vehicles;
    }

    /**
     * Convert meters per second to kilometers per hour
     *
     * @param mps The amount of m/s to convert
     * @return the corresponding amount of km/h
     */
    public static double mpsToKph(double mps) {
        // m/s => km/h : x * 3.6
        return mps * 3.6;
    }

    /**
     * Convert kilometers per hour to meters per second
     *
     * @param kph The amount of km/h to convert
     * @return the corresponding
     */
    public static double kphToMps(double kph) {
        // km/h => m/s : x / 3.6
        return kph / 3.6;
    }

    /**
     * Convert m to px
     *
     * @param scale the ratio px/m
     * @param m     the number of m
     * @return the number of px
     */
    public static int mToPx(double scale, double m) {
        return (int) Math.round(m * scale);
    }

    /**
     * Convert px to m
     *
     * @param scale the ratio px/m
     * @param px    the number of px
     * @return the number of px
     */
    public static double pxToM(double scale, int px) {
        return px / scale;
    }

    /**
     * Convert m to px
     *
     * @param m the number of m
     * @return the number of px
     */
    public int mToPx(double m) {
        return Simulation.mToPx(scale, m);
    }

    /**
     * Convert px to m
     *
     * @param px the number of px
     * @return the number of px
     */
    public double pxToM(int px) {
        return Simulation.pxToM(scale, px);
    }

    /**
     * Parse the OpenDrive XML file
     * @param roadNetwork The Road network that will contains OpenDrive road network
     * @param openDriveFilename The OpenDrive filename
     */
    public void parseOpenDriveXml(RoadNetwork roadNetwork, String openDriveFilename) {
        // TODO (TUM) Add some logs here
        InputStream in = getClass().getResourceAsStream("/map/simulation/" + openDriveFilename);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        OpenDriveHandler.loadRoadNetwork(roadNetwork, new StreamSource(br));
    }
}

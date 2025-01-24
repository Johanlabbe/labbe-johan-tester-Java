package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;

public class ParkingService {

    private static final Logger logger = LogManager.getLogger("ParkingService");

    // FareCalculatorService est déjà injecté
    private final FareCalculatorService fareCalculatorService;
    
    private final InputReaderUtil inputReaderUtil;
    private final ParkingSpotDAO parkingSpotDAO;
    private final TicketDAO ticketDAO;

    public static class PriceCalculator {
        public double calculatePrice(Date inTime, Date outTime, double hourlyRate) {
            if (inTime == null || outTime == null || hourlyRate <= 0) {
                throw new IllegalArgumentException("Invalid input for price calculation");
            }

            long durationInMillis = outTime.getTime() - inTime.getTime();
            if (durationInMillis <= 0) {
                return 0;
            }

            double durationInHours = durationInMillis / (1000.0 * 60 * 60);
            return Math.ceil(durationInHours) * hourlyRate;
        }
    }
    
    public ParkingService(InputReaderUtil inputReaderUtil, ParkingSpotDAO parkingSpotDAO, TicketDAO ticketDAO, FareCalculatorService fareCalculatorService) {
        this.inputReaderUtil = inputReaderUtil;
        this.parkingSpotDAO = parkingSpotDAO;
        this.ticketDAO = ticketDAO;
        this.fareCalculatorService = fareCalculatorService;
    }

    public void processIncomingVehicle() {
        try {
            ParkingSpot parkingSpot = getNextParkingNumberIfAvailable();
            if (parkingSpot != null && parkingSpot.getId() > 0) {
                String vehicleRegNumber = getVehichleRegNumber();
                parkingSpot.setAvailable(false);
                parkingSpotDAO.updateParking(parkingSpot);
    
                Date inTime = new Date();
                Ticket ticket = new Ticket();
                ticket.setParkingSpot(parkingSpot);
                ticket.setVehicleRegNumber(vehicleRegNumber);
                ticket.setPrice(0);
                ticket.setInTime(inTime);
                ticket.setOutTime(null);
    
                logger.debug("Attempting to save ticket: {}", ticket);
                boolean saved = ticketDAO.saveTicket(ticket);
                logger.debug("Ticket save status: {}", saved);
    
                System.out.println("Generated Ticket and saved in DB");
                System.out.println("Please park your vehicle in spot number: " + parkingSpot.getId());
                System.out.println("Recorded in-time for vehicle number: " + vehicleRegNumber + " is: " + inTime);
            }
        } catch (Exception e) {
            logger.error("Unable to process incoming vehicle", e);
        }
    }

    private String getVehichleRegNumber() throws Exception {
        System.out.println("Please type the vehicle registration number and press enter key");
        return inputReaderUtil.readVehicleRegistrationNumber();
    }

    public ParkingSpot getNextParkingNumberIfAvailable(){
        int parkingNumber=0;
        ParkingSpot parkingSpot = null;
        try{
            ParkingType parkingType = getVehichleType();
            parkingNumber = parkingSpotDAO.getNextAvailableSlot(parkingType);
            if(parkingNumber > 0){
                parkingSpot = new ParkingSpot(parkingNumber,parkingType, true);
            }else{
                throw new Exception("Error fetching parking number from DB. Parking slots might be full");
            }
        }catch(IllegalArgumentException ie){
            logger.error("Error parsing user input for type of vehicle", ie);
        }catch(Exception e){
            logger.error("Error fetching next available parking slot", e);
        }
        return parkingSpot;
    }

    private ParkingType getVehichleType(){
        System.out.println("Please select vehicle type from menu");
        System.out.println("1 CAR");
        System.out.println("2 BIKE");
        int input = inputReaderUtil.readSelection();
        switch(input){
            case 1: {
                return ParkingType.CAR;
            }
            case 2: {
                return ParkingType.BIKE;
            }
            default: {
                System.out.println("Incorrect input provided");
                throw new IllegalArgumentException("Entered input is invalid");
            }
        }
    }

    public void processExitingVehicle(Date outTime) throws Exception {
        try {
            String vehicleRegNumber = inputReaderUtil.readVehicleRegistrationNumber();
            logger.info("Processing exiting vehicle: {}", vehicleRegNumber);
    
            Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);
            if (ticket == null) {
                logger.error("No ticket found for vehicle registration number: {}", vehicleRegNumber);
                return;
            }
    
            ticket.setOutTime(outTime);
    
            int nbTickets = ticketDAO.getNbTicket(vehicleRegNumber);
            boolean isRecurringUser = nbTickets > 1;
            logger.info("Vehicle {} is a recurring user: {}", vehicleRegNumber, isRecurringUser);
    
            fareCalculatorService.calculateFare(ticket, isRecurringUser);
            logger.info("Calculated fare for vehicle {}: {}", vehicleRegNumber, ticket.getPrice());
    
            if (ticket.getPrice() <= 0) {
                logger.error("Calculated fare is invalid for vehicle: {}", vehicleRegNumber);
                return;
            }
    
            logger.info("Updating ticket for vehicle: {}", vehicleRegNumber);
            if (!ticketDAO.updateTicket(ticket)) {
                logger.error("Failed to update ticket for vehicle: {}", vehicleRegNumber);
                return;
            }
            logger.info("Ticket updated successfully for vehicle: {}", vehicleRegNumber);
    
            ParkingSpot parkingSpot = ticket.getParkingSpot();
            parkingSpot.setAvailable(true);
    
            logger.info("Updating parking spot for spot ID: {}", parkingSpot.getId());
            if (!parkingSpotDAO.updateParking(parkingSpot)) {
                logger.error("Failed to update parking spot for spot ID: {}", parkingSpot.getId());
                return;
            }
    
            logger.info("Vehicle {} exited successfully. Total fare: {}", vehicleRegNumber, ticket.getPrice());
        } catch (Exception e) {
            logger.error("Unable to process exiting vehicle due to an error: {}", e.getMessage(), e);
            throw e;
        }
    }
}
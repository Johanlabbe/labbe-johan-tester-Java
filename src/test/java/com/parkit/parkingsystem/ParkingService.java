package com.parkit.parkingsystem;

import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.util.InputReaderUtil;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ParkingService {

    private static final Logger logger = LogManager.getLogger("ParkingService");

    private final FareCalculatorService fareCalculatorService;

    private final InputReaderUtil inputReaderUtil;
    private final ParkingSpotDAO parkingSpotDAO;
    private final TicketDAO ticketDAO;

    public ParkingService(InputReaderUtil inputReaderUtil, ParkingSpotDAO parkingSpotDAO, TicketDAO ticketDAO, FareCalculatorService fareCalculatorService) {
        this.inputReaderUtil = inputReaderUtil;
        this.parkingSpotDAO = parkingSpotDAO;
        this.ticketDAO = ticketDAO;
        this.fareCalculatorService = fareCalculatorService;
    }

    public void processExitingVehicle() throws Exception {
        try {
            String vehicleRegNumber = inputReaderUtil.readVehicleRegistrationNumber();
            logger.info("Processing exiting vehicle: {}", vehicleRegNumber);
    
            Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);
            if (ticket == null) {
                logger.error("No ticket found for vehicle registration number: {}", vehicleRegNumber);
                return;
            }
    
            Date outTime = new Date();
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
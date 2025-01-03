package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.util.InputReaderUtil;

import java.util.Date;

public class ParkingService {

    private final InputReaderUtil inputReaderUtil;
    private final ParkingSpotDAO parkingSpotDAO;
    private final TicketDAO ticketDAO;

    public ParkingService(InputReaderUtil inputReaderUtil, ParkingSpotDAO parkingSpotDAO, TicketDAO ticketDAO) {
        this.inputReaderUtil = inputReaderUtil;
        this.parkingSpotDAO = parkingSpotDAO;
        this.ticketDAO = ticketDAO;
    }

    public void processExitingVehicle() {
        try {
            String vehicleRegNumber = inputReaderUtil.readVehicleRegistrationNumber();

            if (vehicleRegNumber == null || vehicleRegNumber.isEmpty()) {
                System.out.println("Invalid vehicle registration number provided.");
                return;
            }

            Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);
            if (ticket == null) {
                System.out.println("No ticket found for vehicle registration number: " + vehicleRegNumber);
                return;
            }

            int nbTickets = ticketDAO.getNbTicket(vehicleRegNumber);
            if (nbTickets > 1) {
                System.out.println("Welcome back! You're eligible for a discount.");
            }

            Date outTime = new Date();
            ticket.setOutTime(outTime);

            long inTimeMillis = ticket.getInTime().getTime();
            long outTimeMillis = outTime.getTime();
            double durationHours = (double) (outTimeMillis - inTimeMillis) / (1000 * 60 * 60);

            if (durationHours <= 0) {
                System.out.println("Invalid parking duration.");
                return;
            }

            double rate = nbTickets > 1 ? 1.5 * 0.95 : 1.5; // Apply 5% discount for regular users
            ticket.setPrice(durationHours * rate);

            if (!ticketDAO.updateTicket(ticket)) {
                System.out.println("Failed to update ticket in the database.");
                return;
            }

            ParkingSpot parkingSpot = ticket.getParkingSpot();
            parkingSpot.setAvailable(true);
            if (!parkingSpotDAO.updateParking(parkingSpot)) {
                System.out.println("Failed to update parking spot.");
                return;
            }

            System.out.println("Vehicle with registration number " + vehicleRegNumber + " has exited successfully.");
        } catch (Exception e) {
            System.out.println("Unable to process exiting vehicle: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
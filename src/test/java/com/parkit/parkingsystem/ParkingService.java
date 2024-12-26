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

    // Constructeur pour injecter les dépendances
    public ParkingService(InputReaderUtil inputReaderUtil, ParkingSpotDAO parkingSpotDAO, TicketDAO ticketDAO) {
        this.inputReaderUtil = inputReaderUtil;
        this.parkingSpotDAO = parkingSpotDAO;
        this.ticketDAO = ticketDAO;
    }

    // Méthode pour traiter la sortie d'un véhicule
    public void processExitingVehicle() {
        try {
            // Lecture du numéro de plaque d'immatriculation
            String vehicleRegNumber = inputReaderUtil.readVehicleRegistrationNumber();
            if (vehicleRegNumber == null || vehicleRegNumber.isEmpty()) {
                System.out.println("Invalid vehicle registration number provided.");
                return;
            }

            // Récupération du ticket associé
            Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);
            if (ticket == null) {
                System.out.println("No ticket found for vehicle registration number: " + vehicleRegNumber);
                return;
            }

            // Mise à jour de l'heure de sortie
            Date outTime = new Date();
            ticket.setOutTime(outTime);

            // Calcul de la durée de stationnement en heures
            long inTimeMillis = ticket.getInTime().getTime();
            long outTimeMillis = outTime.getTime();
            double durationHours = (double) (outTimeMillis - inTimeMillis) / (1000 * 60 * 60);

            if (durationHours <= 0) {
                System.out.println("Invalid parking duration.");
                return;
            }

            // Calcul du tarif (exemple avec un tarif horaire de 1.5)
            ticket.setPrice(durationHours * 1.5);

            // Mise à jour du ticket dans la base de données
            if (!ticketDAO.updateTicket(ticket)) {
                System.out.println("Failed to update ticket in the database.");
                return;
            }

            // Mise à jour de la disponibilité de la place de parking
            ParkingSpot parkingSpot = ticket.getParkingSpot();
            parkingSpot.setAvailable(true);
            if (!parkingSpotDAO.updateParking(parkingSpot)) {
                System.out.println("Failed to update parking spot.");
                return;
            }

            // Confirmation de la sortie réussie
            System.out.println("Vehicle with registration number " + vehicleRegNumber + " has exited successfully.");
        } catch (Exception e) {
            // Gestion des exceptions inattendues
            System.out.println("Unable to process exiting vehicle: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

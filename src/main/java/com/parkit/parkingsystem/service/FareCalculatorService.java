package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    // Nouvelle méthode avec deux paramètres
    public void calculateFare(Ticket ticket, boolean discount) {
        if (ticket.getOutTime() == null || ticket.getInTime() == null) {
            throw new IllegalArgumentException("In time or Out time is null");
        }

        if (ticket.getOutTime().before(ticket.getInTime())) {
            throw new IllegalArgumentException("Out time provided is incorrect: " + ticket.getOutTime());
        }

        // Calcul de la durée en millisecondes
        long inTimeMillis = ticket.getInTime().getTime();
        long outTimeMillis = ticket.getOutTime().getTime();
        long durationMillis = outTimeMillis - inTimeMillis;

        // Conversion en heures
        double durationHours = (double) durationMillis / (1000 * 60 * 60);

        // Gratuité pour une durée inférieure à 30 minutes
        if (durationHours < 0.5) { // 30 minutes = 0.5 heures
            ticket.setPrice(0.0);
            return;
        }

        // Calcul du tarif normal
        double price = 0.0;
        switch (ticket.getParkingSpot().getParkingType()) {
            case CAR:
                price = durationHours * Fare.CAR_RATE_PER_HOUR;
                break;
            case BIKE:
                price = durationHours * Fare.BIKE_RATE_PER_HOUR;
                break;
            default:
                throw new IllegalArgumentException("Unknown Parking Type");
        }

        // Appliquer la réduction si discount est true
        if (discount) {
            price *= 0.95; // Réduction de 5%
        }

        ticket.setPrice(price);
    }

    // Ancienne méthode qui appelle la nouvelle avec discount = false
    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }
}

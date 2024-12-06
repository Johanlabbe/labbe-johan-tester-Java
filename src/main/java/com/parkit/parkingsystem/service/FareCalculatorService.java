package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket) {
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

        if (durationHours <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }

        // Calcul du tarif
        switch (ticket.getParkingSpot().getParkingType()) {
            case CAR: {
                ticket.setPrice(durationHours * Fare.CAR_RATE_PER_HOUR);
                break;
            }
            case BIKE: {
                ticket.setPrice(durationHours * Fare.BIKE_RATE_PER_HOUR);
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown Parking Type");
        }
    }
}


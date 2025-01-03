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
    
        // Calcul de la durée en heures
        long durationInMillis = ticket.getOutTime().getTime() - ticket.getInTime().getTime();
        double durationHours = durationInMillis / (1000.0 * 60 * 60);
    
        // Gratuité pour une durée inférieure à 30 minutes
        if (durationHours < 0.5) {
            ticket.setPrice(0.0);
            return;
        }
    
        // Sélection du tarif en fonction du type de parking
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
    
        // Appliquer la réduction si applicable
        if (discount) {
            price *= 0.95; // Réduction de 5 %
        }
    
        // Arrondir à trois décimales
        price = Math.round(price * 1000.0) / 1000.0;
    
        ticket.setPrice(price);
    }    

    // Ancienne méthode qui appelle la nouvelle avec discount = false
    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }
}

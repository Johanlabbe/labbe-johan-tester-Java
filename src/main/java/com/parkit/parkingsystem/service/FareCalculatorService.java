package com.parkit.parkingsystem.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {
    private static final Logger logger = LoggerFactory.getLogger(FareCalculatorService.class);
    
    public void calculateFare(Ticket ticket, boolean discount) {
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket is null");
        }

        if (ticket.getInTime() == null || ticket.getOutTime() == null) {
            throw new IllegalArgumentException("In time or Out time is null");
        }

        if (ticket.getOutTime().before(ticket.getInTime())) {
            throw new IllegalArgumentException("Out time is before In time");
        }

        ParkingSpot parkingSpot = ticket.getParkingSpot();
        if (parkingSpot == null || parkingSpot.getParkingType() == null) {
            throw new IllegalArgumentException("Unknown Parking Type");
        }

        long durationInMillis = ticket.getOutTime().getTime() - ticket.getInTime().getTime();
        double durationHours = durationInMillis / (1000.0 * 60 * 60);
        logger.debug("Duration in hours for vehicle {}: {}", ticket.getVehicleRegNumber(), durationHours);

        if (durationHours < 0.5) {
            ticket.setPrice(0.0);
            logger.info("Duration is less than 30 minutes, parking is free.");
            return;
        }

        double ratePerHour;
        switch (parkingSpot.getParkingType()) {
            case CAR:
                ratePerHour = Fare.CAR_RATE_PER_HOUR;
                break;
            case BIKE:
                ratePerHour = Fare.BIKE_RATE_PER_HOUR;
                break;
            default:
                logger.warn("Unknown Parking Type: {}", parkingSpot.getParkingType());
                throw new IllegalArgumentException("Unknown Parking Type");
        }

        double price = durationHours * ratePerHour;

        if (discount) {
            price *= 0.95; // Réduction de 5 %
            logger.info("Discount applied: 5% for recurring user");
        }

        price = BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP).doubleValue();

        ticket.setPrice(price);
        logger.info("Fare calculated: {} for vehicle {}", price, ticket.getVehicleRegNumber());
    }
}

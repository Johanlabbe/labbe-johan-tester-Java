package com.parkit.parkingsystem.dao;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService.PriceCalculator;

import org.apache.logging.log4j.LogManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.parkit.parkingsystem.constants.ParkingType;

public class TicketDAO {

    private static final Logger logger = LoggerFactory.getLogger(TicketDAO.class);
    public DataBaseConfig dataBaseConfig;

    public boolean saveTicket(Ticket ticket) {
        int result = 0;
        try (
            Connection con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO ticket (PARKING_NUMBER, VEHICLE_REG_NUMBER, PRICE, IN_TIME, OUT_TIME) VALUES (?, ?, ?, ?, ?)"
            )
        ) {
            ps.setInt(1, ticket.getParkingSpot().getId());
            ps.setString(2, ticket.getVehicleRegNumber());
            ps.setDouble(3, ticket.getPrice());
            ps.setTimestamp(4, new java.sql.Timestamp(ticket.getInTime().getTime()));
            ps.setTimestamp(5, ticket.getOutTime() != null ? new java.sql.Timestamp(ticket.getOutTime().getTime()) : null);
    
            result = ps.executeUpdate();
            logger.info("Ticket successfully saved for vehicle: {}", ticket.getVehicleRegNumber());
            con.commit(); // Validation explicite de la transaction
        } catch (Exception ex) {
            logger.error("Error saving ticket for vehicle: {}", ticket.getVehicleRegNumber(), ex);
        }
        return result > 0;
    }    

    public Ticket getTicket(String vehicleRegNumber) {
        Ticket ticket = null;
        try (
            Connection con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(
                "SELECT t.ID, t.PARKING_NUMBER, t.PRICE, t.IN_TIME, t.OUT_TIME, p.TYPE " +
                "FROM ticket t " +
                "INNER JOIN parking p ON p.PARKING_NUMBER = t.PARKING_NUMBER " +
                "WHERE t.VEHICLE_REG_NUMBER = ? " +
                "ORDER BY t.IN_TIME DESC LIMIT 1"
            )
        ) {
            ps.setString(1, vehicleRegNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ticket = mapResultSetToTicket(rs, vehicleRegNumber);
                    logger.info("Ticket successfully retrieved for vehicle: {}", vehicleRegNumber);
                } else {
                    logger.warn("No ticket found for vehicle: {}", vehicleRegNumber);
                }
            }
        } catch (Exception ex) {
            logger.error("Error retrieving ticket for vehicle: {}", vehicleRegNumber, ex);
        }
        return ticket;
    }

    private Ticket mapResultSetToTicket(ResultSet rs, String vehicleRegNumber) throws SQLException {
        Ticket ticket = new Ticket();
        ticket.setId(rs.getInt("ID"));
        ParkingSpot parkingSpot = new ParkingSpot(
            rs.getInt("PARKING_NUMBER"),
            ParkingType.valueOf(rs.getString("TYPE").toUpperCase()),
            false
        );
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber(vehicleRegNumber);
        ticket.setPrice(rs.getDouble("PRICE"));
        ticket.setInTime(rs.getTimestamp("IN_TIME"));
        ticket.setOutTime(rs.getTimestamp("OUT_TIME"));
        return ticket;
    }

    public boolean updateTicket(Ticket ticket) {
        try (
            Connection con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(
                "UPDATE ticket SET PRICE = ?, OUT_TIME = ? WHERE ID = ?"
            )
        ) {
            PriceCalculator calculator = new PriceCalculator();
            double price = calculator.calculatePrice(ticket.getInTime(), ticket.getOutTime(), 2.5); // Exemple : tarif horaire = 2.5
            ticket.setPrice(price);

            ps.setDouble(1, ticket.getPrice());
            ps.setTimestamp(2, new Timestamp(ticket.getOutTime().getTime()));
            ps.setInt(3, ticket.getId());

            int result = ps.executeUpdate();
            return result > 0;
        } catch (Exception ex) {
            logger.error("Error updating ticket", ex);
            return false;
        }
    }

    public int getNbTicket(String vehicleRegNumber) {
        int ticketCount = 0;
        try (
            Connection con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM ticket WHERE VEHICLE_REG_NUMBER = ?"
            )
        ) {
            ps.setString(1, vehicleRegNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ticketCount = rs.getInt(1);
                }
            }
        } catch (Exception ex) {
            logger.error("Error counting tickets for vehicle: {}", vehicleRegNumber, ex);
        }
        return ticketCount;
    }
}
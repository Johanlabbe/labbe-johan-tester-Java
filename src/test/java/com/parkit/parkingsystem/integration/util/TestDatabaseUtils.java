package com.parkit.parkingsystem.integration.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;

public class TestDatabaseUtils {

    private DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();

    public TestDatabaseUtils(DataBaseTestConfig dataBaseTestConfig) {
        this.dataBaseTestConfig = dataBaseTestConfig;
    }

    public void verifyTicketInDatabase(String vehicleRegNumber) {
        String sql = "SELECT * FROM ticket WHERE VEHICLE_REG_NUMBER = ? ORDER BY IN_TIME DESC";
        try (Connection con = dataBaseTestConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, vehicleRegNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.println("Ticket en base -> ID: " + rs.getInt("ID") +
                            ", InTime: " + rs.getTimestamp("IN_TIME") +
                            ", OutTime: " + rs.getTimestamp("OUT_TIME") +
                            ", Price: " + rs.getDouble("PRICE"));
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des tickets pour " + vehicleRegNumber + ": " + e.getMessage());
        }
    }
}


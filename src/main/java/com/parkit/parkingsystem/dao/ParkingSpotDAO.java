package com.parkit.parkingsystem.dao;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.ParkingType;
// import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.model.ParkingSpot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ParkingSpotDAO {
    private static final Logger logger = LogManager.getLogger("ParkingSpotDAO");

    // public DataBaseConfig dataBaseConfig = new DataBaseConfig();
    public DataBaseConfig dataBaseConfig;

    public ParkingSpotDAO(DataBaseConfig dataBaseConfig) {
        this.dataBaseConfig = dataBaseConfig;
    }

    /**
     * Récupère les informations sur une place de parking à partir de son identifiant.
     *
     * @param parkingSpotId l'identifiant de la place de parking
     * @return un objet {@link ParkingSpot} contenant les informations sur la place de parking,
     *         ou {@code null} si aucune place correspondante n'est trouvée
     */
    public ParkingSpot getParkingSpot(int parkingSpotId) {
        ParkingSpot parkingSpot = null;
        String sql = "SELECT PARKING_NUMBER, TYPE, AVAILABLE FROM parking WHERE PARKING_NUMBER = ?";
        try (Connection con = dataBaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
    
            ps.setInt(1, parkingSpotId);
    
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ParkingType parkingType = ParkingType.valueOf(rs.getString("TYPE").toUpperCase());
                    boolean isAvailable = rs.getBoolean("AVAILABLE");
                    parkingSpot = new ParkingSpot(parkingSpotId, parkingType, isAvailable);
                }
            }
        } catch (Exception ex) {
            logger.error("Error fetching parking spot with ID: {}", parkingSpotId, ex);
        }
        return parkingSpot;
    }

    /**
     * Récupère le prochain numéro de place de parking disponible.
     * 
     * @param parkingType Le type de parking (CAR ou BIKE)
     * @return Le numéro de la place disponible ou 0 si aucune n'est disponible.
     */
    public int getNextAvailableSlot(ParkingType parkingType) {
        Connection con = null;
        int parkingNumber = 0;
        try {
            con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(
                "SELECT PARKING_NUMBER FROM parking WHERE AVAILABLE = true AND TYPE = ? LIMIT 1");
            ps.setString(1, parkingType.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                parkingNumber = rs.getInt(1);
            }
        } catch (Exception ex) {
            logger.error("Error fetching parking slot", ex);
        } finally {
            dataBaseConfig.closeConnection(con);
        }
        return parkingNumber;
    }
    
    /**
     * Met à jour la disponibilité d'une place de parking.
     * 
     * @param parkingSpot L'objet ParkingSpot à mettre à jour
     * @return true si la mise à jour est réussie, false sinon.
     */
    public boolean updateParking(ParkingSpot parkingSpot) {
        if (parkingSpot == null) {
            logger.error("ParkingSpot is null. Cannot update.");
            return false;
        }
    
        try (Connection con = dataBaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(DBConstants.UPDATE_PARKING_SPOT)) {
            ps.setBoolean(1, parkingSpot.isAvailable());
            ps.setInt(2, parkingSpot.getId());
            int updateRowCount = ps.executeUpdate();
            if (updateRowCount == 1) {
                logger.info("Parking spot with ID: {} successfully updated to available: {}", parkingSpot.getId(), parkingSpot.isAvailable());
                return true;
            } else {
                logger.warn("No parking spot updated for ID: {}", parkingSpot.getId());
                return false;
            }
        } catch (Exception ex) {
            logger.error("Error updating parking spot with ID: {} to available: {}", parkingSpot.getId(), parkingSpot.isAvailable(), ex);
            return false;
        }
    }    

    /**
     * Vérifie si une place de parking est disponible.
     * 
     * @param parkingSpotId L'ID de la place de parking.
     * @return true si la place est disponible, false sinon.
     */
    public boolean getParkingSpotAvailability(int parkingSpotId) {
        boolean isAvailable = false;
        try (Connection con = dataBaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT AVAILABLE FROM parking WHERE PARKING_NUMBER = ?")) {
    
            ps.setInt(1, parkingSpotId);
    
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    isAvailable = rs.getBoolean("AVAILABLE");
                } else {
                    logger.warn("No parking spot found for ID: {}", parkingSpotId);
                }
            }
        } catch (Exception ex) {
            logger.error("Error fetching parking spot availability", ex);
        }
        return isAvailable;
    }
}

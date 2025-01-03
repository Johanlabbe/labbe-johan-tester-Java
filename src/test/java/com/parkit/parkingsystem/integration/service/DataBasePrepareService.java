package com.parkit.parkingsystem.integration.service;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.Statement;

public class DataBasePrepareService {

    private static final Logger logger = LogManager.getLogger(DataBasePrepareService.class);

    // Utilisation de DataBaseTestConfig pour les tests
    private final DataBaseConfig dataBaseConfig;

    /**
     * Constructeur pour injecter la configuration de la base de données.
     * Par défaut, utilise DataBaseTestConfig.
     */
    public DataBasePrepareService() {
        this.dataBaseConfig = new DataBaseTestConfig();
    }

    /**
     * Efface les entrées de la base de données pour préparer un état initial.
     */
    public void clearDataBaseEntries() {
        try (Connection con = dataBaseConfig.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("TRUNCATE TABLE ticket");
            stmt.executeUpdate("UPDATE parking SET available = true");
            logger.info("Database entries cleared.");
        } catch (Exception ex) {
            logger.error("Error clearing database entries: ", ex);
        }
    }
}

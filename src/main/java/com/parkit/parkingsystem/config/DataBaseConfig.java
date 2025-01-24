package com.parkit.parkingsystem.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DataBaseConfig {

    private static final Logger logger = LogManager.getLogger("DataBaseConfig");

    // Constantes pour les paramètres de connexion à la base de données
    private static final String DB_URL = "jdbc:mysql://localhost:3306/prod?serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    /**
     * Ouvre une connexion à la base de données.
     *
     * @return Une instance de {@link Connection}.
     * @throws ClassNotFoundException Si le driver MySQL n'est pas trouvé.
     * @throws SQLException           Si une erreur SQL survient lors de l'ouverture de la connexion.
     */
    public Connection getConnection() throws ClassNotFoundException, SQLException {
        logger.info("Creating DB connection to {}", DB_URL);
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Ferme une connexion à la base de données.
     *
     * @param con L'objet {@link Connection} à fermer.
     */
    public void closeConnection(Connection con) {
        if (con != null) {
            try {
                con.close();
                logger.info("DB connection closed successfully");
            } catch (SQLException e) {
                logger.error("Error while closing DB connection", e);
            }
        } else {
            logger.warn("Attempted to close a null connection");
        }
    }

    /**
     * Ferme une instance de {@link PreparedStatement}.
     *
     * @param ps L'objet {@link PreparedStatement} à fermer.
     */
    public void closePreparedStatement(PreparedStatement ps) {
        if (ps != null) {
            try {
                ps.close();
                logger.info("Prepared Statement closed successfully");
            } catch (SQLException e) {
                logger.error("Error while closing Prepared Statement", e);
            }
        } else {
            logger.warn("Attempted to close a null PreparedStatement");
        }
    }

    /**
     * Ferme une instance de {@link ResultSet}.
     *
     * @param rs L'objet {@link ResultSet} à fermer.
     */
    public void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
                logger.info("Result Set closed successfully");
            } catch (SQLException e) {
                logger.error("Error while closing Result Set", e);
            }
        } else {
            logger.warn("Attempted to close a null ResultSet");
        }
    }
}

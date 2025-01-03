package com.parkit.parkingsystem.integration;

import com.mysql.cj.exceptions.InvalidConnectionAttributeException;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
// import static org.mockito.Mockito.when;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static final Logger logger = LoggerFactory.getLogger(ParkingDataBaseIT.class);

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception {
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        lenient().when(inputReaderUtil.readSelection()).thenReturn(1); // Car type
        lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown() {
        dataBasePrepareService.clearDataBaseEntries();
    }

    @Test
    public void testParkingACar() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, new FareCalculatorService());
    
        // Process incoming vehicle
        parkingService.processIncomingVehicle();

        // Vérifiez que le ticket est sauvegardé
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket, "Ticket should be saved in the database.");
        logger.info("Ticket saved: {}", ticket);
    }

    @Test
    public void testParkingLotExit() {
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, new FareCalculatorService());
    
        // Processus d'entrée du véhicule
        parkingService.processIncomingVehicle();
    
        // Récupérez le ticket
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket, "Ticket should exist before exiting.");
    
        // Simulez une sortie
        parkingService.processExitingVehicle();
    
        // Vérifiez que l'heure de sortie est définie
        Ticket updatedTicket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(updatedTicket.getOutTime(), "Out time should be set.");
        assertTrue(updatedTicket.getPrice() > 0, "Price should be calculated.");
    }

    @Test
    public void testParkingLotExitRecurringUser() {
        Ticket recurringTicket = new Ticket();
        recurringTicket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        recurringTicket.setVehicleRegNumber("ABCDEF");
        recurringTicket.setInTime(new Date(System.currentTimeMillis() - (2 * 60 * 60 * 1000))); // 2 heures avant
        recurringTicket.setPrice(0.0);
        ticketDAO.saveTicket(recurringTicket);
    
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, new FareCalculatorService());
    
        parkingService.processExitingVehicle();
    
        Ticket updatedTicket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(updatedTicket.getOutTime(), "Out time should be set.");
        assertTrue(updatedTicket.getPrice() > 0, "Price should be calculated after exit.");
    }    
}

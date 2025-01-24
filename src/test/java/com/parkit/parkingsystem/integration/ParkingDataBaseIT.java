package com.parkit.parkingsystem.integration;

// import com.mysql.cj.exceptions.InvalidConnectionAttributeException;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
// import com.parkit.parkingsystem.service.ParkingService.PriceCalculator;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
// import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static final Logger logger = LoggerFactory.getLogger(ParkingDataBaseIT.class);

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static DataBasePrepareService dataBasePrepareService;
    
    @Mock
    private static TicketDAO ticketDAO;
    
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    
    @Mock
    private static InputReaderUtil inputReaderUtil;

    @Mock
    private FareCalculatorService fareCalculatorService;

    private ParkingService parkingService;
    private Ticket ticket;

    @BeforeAll
    static void setUpAll() {
        MockitoAnnotations.initMocks(ParkingDataBaseIT.class);
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    void setUpPerTest() throws Exception {
        // MockitoAnnotations.initMocks(this);
    
        dataBasePrepareService.clearDataBaseEntries();
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);
    
        ticket = new Ticket();
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // Entrée il y a 1 heure
        ticket.setOutTime(new Date());
    }
    
    @AfterAll
    private static void tearDown() {
        dataBasePrepareService.clearDataBaseEntries();
    }

    @Test
    public void testSaveAndRetrieveTicket() {
        TicketDAO realTicketDAO = new TicketDAO(dataBaseTestConfig);
        Ticket ticket = new Ticket();
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setPrice(0.0);
        ticket.setInTime(new Date());
        ticket.setOutTime(null);

        boolean result = realTicketDAO.saveTicket(ticket);
        assertTrue(result, "Ticket should be saved successfully.");

        Ticket retrievedTicket = realTicketDAO.getTicket("ABCDEF");
        assertNotNull(retrievedTicket, "Retrieved ticket should not be null.");
        assertEquals("ABCDEF", retrievedTicket.getVehicleRegNumber(), "Vehicle registration number should match.");
    }

    @Test
    public void testParkingACar() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1); // CAR
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
    
        ParkingSpotDAO realParkingSpotDAO = new ParkingSpotDAO(dataBaseTestConfig);
        TicketDAO realTicketDAO = new TicketDAO(dataBaseTestConfig);
        ParkingService parkingService = new ParkingService(inputReaderUtil, realParkingSpotDAO, realTicketDAO, new FareCalculatorService());
        
        parkingService.processIncomingVehicle();
        
        Ticket ticket = realTicketDAO.getTicket("ABCDEF");
        assertNotNull(ticket, "Ticket should be saved in the database.");
        logger.info("Ticket saved: {}", ticket);
        
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        assertNotNull(parkingSpot, "Parking spot should not be null.");
        assertFalse(parkingSpot.isAvailable(), "Parking spot should be marked as unavailable.");
    }    

    @Test
    public void testParkingLotExit() throws Exception {
        ParkingSpotDAO realParkingSpotDAO = new ParkingSpotDAO(dataBaseTestConfig);
        TicketDAO realTicketDAO = new TicketDAO(dataBaseTestConfig);
        ParkingService parkingService = new ParkingService(inputReaderUtil, realParkingSpotDAO, realTicketDAO, new FareCalculatorService());
    
        dataBasePrepareService.clearDataBaseEntries();
    
        when(inputReaderUtil.readSelection()).thenReturn(1); // CAR
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
    
        parkingService.processIncomingVehicle();
    
        Ticket ticket = realTicketDAO.getTicket("ABCDEF");
        assertNotNull(ticket, "Ticket should be saved in the database.");
        assertNotNull(ticket.getInTime(), "In-time should be set.");
        assertNull(ticket.getOutTime(), "Out-time should not be set for an incoming vehicle.");
    
        ParkingSpot parkingSpot = realParkingSpotDAO.getParkingSpot(ticket.getParkingSpot().getId());
        assertNotNull(parkingSpot, "Parking spot should not be null.");
        assertFalse(parkingSpot.isAvailable(), "Parking spot should be marked as unavailable.");
    
        parkingService.processExitingVehicle(new Date(System.currentTimeMillis() + 60 * 60 * 1000));
    
        Ticket updatedTicket = realTicketDAO.getTicket("ABCDEF");
        assertNotNull(updatedTicket, "Updated ticket should not be null.");
        assertNotNull(updatedTicket.getOutTime(), "Out-time should be set.");
        assertTrue(updatedTicket.getPrice() > 0, "Price should be calculated.");
    
        ParkingSpot updatedParkingSpot = realParkingSpotDAO.getParkingSpot(updatedTicket.getParkingSpot().getId());
        assertNotNull(updatedParkingSpot, "Parking spot should not be null.");
        assertTrue(updatedParkingSpot.isAvailable(), "Parking spot should be marked as available after exit.");
    }

    @Test
    public void testExitingVehicleWithNoTicket() throws Exception {
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("NONEXISTENT");
        when(ticketDAO.getTicket(anyString())).thenThrow(new RuntimeException("No ticket found"));
    
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, new FareCalculatorService());
    
        Exception exception = assertThrows(RuntimeException.class, () -> parkingService.processExitingVehicle(new Date()));
    
        assertEquals("No ticket found", exception.getMessage());
    
        verify(ticketDAO, never()).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class));
    }    

    @Test
    public void testExitingVehicleWithTicketUpdateFailure() throws Exception {
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
    
        Ticket ticket = new Ticket();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // Entrée il y a 1h
        ticket.setOutTime(new Date());
    
        doAnswer(invocation -> {
            Ticket t = invocation.getArgument(0);
            t.setPrice(5.0); // Prix simulé
            return null;
        }).when(fareCalculatorService).calculateFare(any(Ticket.class), anyBoolean());
        when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);
    
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);
        parkingService.processExitingVehicle(new Date());
    
        verify(ticketDAO, times(1)).getTicket("ABCDEF");
        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class));
    
        assertNotNull(ticket.getOutTime(), "Out time should not be null.");
        assertTrue(ticket.getPrice() > 0, "Price should be greater than 0.");
    }
    
    @Test
    public void testExitingVehicleWithExceptionInGetTicket() throws Exception {
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.getTicket(anyString())).thenThrow(new RuntimeException("Database error"));
    
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, new FareCalculatorService());
        Exception exception = assertThrows(RuntimeException.class, () -> parkingService.processExitingVehicle(new Date()));

        assertEquals("Database error", exception.getMessage());

        verify(ticketDAO, never()).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class));
    }
    
    @Test
    public void testExitingVehicleUpdatesParkingSpot() {
        try {
            Ticket ticket = new Ticket();
            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");
            ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // 1 heure avant
            ticket.setPrice(0.0);
    
            when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket);
            when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
            doAnswer(invocation -> {
                Ticket t = invocation.getArgument(0);
                t.setPrice(10.0); // Simule un tarif calculé
                t.setOutTime(new Date());
                return null;
            }).when(fareCalculatorService).calculateFare(any(Ticket.class), anyBoolean());
    
            ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);
            parkingService.processExitingVehicle(new Date());
    
            assertNotNull(ticket.getOutTime(), "Out time should not be null.");
            assertTrue(ticket.getPrice() > 0, "Price should be greater than 0.");
    
            verify(ticketDAO).updateTicket(argThat(ticketArg -> 
                ticketArg.getOutTime() != null && ticketArg.getPrice() > 0
            ));
    
            verify(parkingSpotDAO).updateParking(argThat(parkingSpotArg -> 
                parkingSpotArg.isAvailable() && parkingSpotArg.getId() == 1
            ));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testParkingLotExitRecurringUser() throws Exception {
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
    
        Ticket recurringTicket = new Ticket();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        recurringTicket.setParkingSpot(parkingSpot);
        recurringTicket.setVehicleRegNumber("ABCDEF");
        recurringTicket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // Entrée il y a 1h
    
        when(ticketDAO.getTicket("ABCDEF")).thenReturn(recurringTicket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
    
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, new FareCalculatorService());
        parkingService.processExitingVehicle(new Date());
    
        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
    }
}

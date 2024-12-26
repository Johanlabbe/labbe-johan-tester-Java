package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.util.InputReaderUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

public class FareCalculatorServiceTest {

    private static FareCalculatorService fareCalculatorService;
    private Ticket ticket;

    @Mock
    private TicketDAO ticketDAO;

    @Mock
    private ParkingSpotDAO parkingSpotDAO;

    @Mock
    private InputReaderUtil inputReaderUtil;

    private ParkingService parkingService;

    @BeforeAll
    private static void setUp() {
        fareCalculatorService = new FareCalculatorService();
    }

    @BeforeEach
    private void setUpPerTest() {
        ticket = new Ticket();
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
    }

    @Test
    public void calculateFareCar(){
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000));
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
    
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(Fare.CAR_RATE_PER_HOUR, ticket.getPrice(), 0.001);
    }
    
    @Test
    public void calculateFareBike(){
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000));
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);
    
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(Fare.BIKE_RATE_PER_HOUR, ticket.getPrice(), 0.001);
    }

    @Test
    public void calculateFareUnkownType(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  60 * 60 * 1000) );
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, null,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        assertThrows(NullPointerException.class, () -> fareCalculatorService.calculateFare(ticket));
    }

    @Test
    public void calculateFareBikeWithFutureInTime(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() + (  60 * 60 * 1000) );
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
    }

    @Test
    public void calculateFareCarWithLessThan30minutesParkingTime() {
        // Configurer une durée de stationnement inférieure à 30 minutes
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (25 * 60 * 1000)); // 25 minutes
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);

        // Calculer le tarif
        fareCalculatorService.calculateFare(ticket);

        // Vérifier que le tarif est 0
        assertEquals(0.0, ticket.getPrice(), 0.001);
    }

    @Test
    public void calculateFareBikeWithLessThan30minutesParkingTime() {
        // Configurer une durée de stationnement inférieure à 30 minutes
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (20 * 60 * 1000)); // 20 minutes
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);

        // Calculer le tarif
        fareCalculatorService.calculateFare(ticket);

        // Vérifier que le tarif est 0
        assertEquals(0.0, ticket.getPrice(), 0.001);
    }

    @Test
    public void calculateFareCarWithDiscount() {
        // Configurer une durée de stationnement de plus de 30 minutes
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000)); // 1 heure
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);

        // Calculer le tarif avec la remise
        fareCalculatorService.calculateFare(ticket, true);

        // Vérifier que le tarif est 95% du tarif normal
        double expectedPrice = 0.95 * Fare.CAR_RATE_PER_HOUR;
        assertEquals(expectedPrice, ticket.getPrice(), 0.001);
    }

    @Test
    public void calculateFareBikeWithDiscount() {
        // Configurer une durée de stationnement de plus de 30 minutes
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000)); // 1 heure
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);

        // Calculer le tarif avec la remise
        fareCalculatorService.calculateFare(ticket, true);

        // Vérifier que le tarif est 95% du tarif normal
        double expectedPrice = 0.95 * Fare.BIKE_RATE_PER_HOUR;
        assertEquals(expectedPrice, ticket.getPrice(), 0.001);
    }

    @Test
    public void calculateFareBikeWithLessThanOneHourParkingTime(){
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (45 * 60 * 1000)); // 45 minutes
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);
    
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(0.75 * Fare.BIKE_RATE_PER_HOUR, ticket.getPrice(), 0.001);
    }

    @Test
    public void calculateFareCarWithLessThanOneHourParkingTime() {
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (45 * 60 * 1000)); // 45 minutes
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
    
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(0.75 * Fare.CAR_RATE_PER_HOUR, ticket.getPrice(), 0.001);
    }

    @Test
    public void calculateFareCarWithMoreThanADayParkingTime(){
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  24 * 60 * 60 * 1000) );//24 hours parking time should give 24 * parking fare per hour
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket);
        assertEquals( (24 * Fare.CAR_RATE_PER_HOUR) , ticket.getPrice());
    }

    @Test
    public void processExitingVehicleTest() throws Exception {
        // Configuration des mocks
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
        when(ticketDAO.getNbTicket(anyString())).thenReturn(2); // Simule un utilisateur récurrent
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
    
        // Exécution de la méthode
        parkingService.processExitingVehicle();
    
        // Vérifications
        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class)); // Mise à jour du ticket
        verify(ticketDAO, times(1)).getNbTicket("ABCDEF"); // Vérifie le statut d'utilisateur récurrent
        verify(inputReaderUtil, times(1)).readVehicleRegistrationNumber(); // Lecture de la plaque
    }
    

    @Test
    public void processExitingVehicleTestUnableToUpdateTicket() throws Exception {
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false); 
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        parkingService.processExitingVehicle();
        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, times(0)).updateParking(any(ParkingSpot.class)); 
        verify(inputReaderUtil, times(1)).readVehicleRegistrationNumber(); 
    }

    @Test
    public void processExitingVehicleTestThrowsException() throws Exception {
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenThrow(new Exception("Error reading registration number"));
        parkingService.processExitingVehicle();
        verify(ticketDAO, times(0)).updateTicket(any(Ticket.class)); 
        verify(parkingSpotDAO, times(0)).updateParking(any(ParkingSpot.class)); 
    }
}

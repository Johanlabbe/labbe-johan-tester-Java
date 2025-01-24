package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import com.parkit.parkingsystem.service.ParkingService.PriceCalculator;
import com.parkit.parkingsystem.util.InputReaderUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FareCalculatorServiceTest {

    @Mock
    private TicketDAO ticketDAO;
    
    @Mock
    private ParkingSpotDAO parkingSpotDAO;
    
    @Mock
    private InputReaderUtil inputReaderUtil;

    
    private ParkingService parkingService;
    private Ticket ticket;
    private FareCalculatorService fareCalculatorService = new FareCalculatorService();

    @BeforeAll
    public static void setUpClass() {
        System.out.println("Starting tests for FareCalculatorService");
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);
    
        ticket = new Ticket();
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // Entrée il y a 1h
        ticket.setOutTime(new Date());
    }

    @Test
    public void testCalculatePrice() {
        PriceCalculator calculator = new PriceCalculator();

        Calendar calendar = Calendar.getInstance();
        calendar.set(2025, Calendar.JANUARY, 3, 10, 0);
        Date inTime = calendar.getTime();
        calendar.set(2025, Calendar.JANUARY, 3, 12, 30); // 2h30
        Date outTime = calendar.getTime();

        double price = calculator.calculatePrice(inTime, outTime, 2.5);
        assertEquals(7.5, price, 0.01);
    }

    @Test
    public void calculateFareCar() {
        Date inTime = new Date(System.currentTimeMillis() - (60 * 60 * 1000));
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket, false);

        assertEquals(Fare.CAR_RATE_PER_HOUR, ticket.getPrice(), 0.001);
    }

    @Test
    public void calculateFareBike() {
        Date inTime = new Date(System.currentTimeMillis() - (60 * 60 * 1000));
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        fareCalculatorService.calculateFare(ticket, false);

        assertEquals(Fare.BIKE_RATE_PER_HOUR, ticket.getPrice(), 0.001);
    }

    @Test
    public void calculateFareUnkownType() {
        Date inTime = new Date(System.currentTimeMillis() - (60 * 60 * 1000)); // 1 heure avant
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, null, false); // Type de stationnement null
    
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
    
        assertThrows(IllegalArgumentException.class, () -> {
            fareCalculatorService.calculateFare(ticket, false);
        });
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
        assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket, false));
    }

    @Test
    public void calculateFareCarWithLessThan30minutesParkingTime() {
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (25 * 60 * 1000)); // 25 minutes
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);

        fareCalculatorService.calculateFare(ticket, false);

        assertEquals(0.0, ticket.getPrice(), 0.001);
    }

    @Test
    public void calculateFareBikeWithLessThan30minutesParkingTime() {
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (20 * 60 * 1000)); // 20 minutes
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);

        fareCalculatorService.calculateFare(ticket, false);

        assertEquals(0.0, ticket.getPrice(), 0.001);
    }

    @Test
    public void calculateFareCarWithDiscount() {
        Date inTime = new Date(System.currentTimeMillis() - (60 * 60 * 1000)); // 1 heure
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
    
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
    
        fareCalculatorService.calculateFare(ticket, true);
    
        double expectedPrice = BigDecimal.valueOf(0.95 * Fare.CAR_RATE_PER_HOUR)
                                      .setScale(2, RoundingMode.HALF_UP)
                                      .doubleValue();
    
        assertEquals(expectedPrice, ticket.getPrice(), 0.001);
    }    

    @Test
    public void calculateFareBikeWithDiscount() {
        Date inTime = new Date();
        inTime.setTime(System.currentTimeMillis() - (60 * 60 * 1000)); // 1 heure
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.BIKE, false);

        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);

        fareCalculatorService.calculateFare(ticket, true);

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
        fareCalculatorService.calculateFare(ticket, false);
        assertEquals(0.75 * Fare.BIKE_RATE_PER_HOUR, ticket.getPrice(), 0.001);
    }

    @Test
    public void calculateFareCarWithLessThanOneHourParkingTime() {
        Date inTime = new Date(System.currentTimeMillis() - (45 * 60 * 1000)); // 45 minutes
        Date outTime = new Date();
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
    
        ticket.setInTime(inTime);
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
    
        fareCalculatorService.calculateFare(ticket, false);
    
        double expectedPrice = BigDecimal.valueOf(0.75 * Fare.CAR_RATE_PER_HOUR)
                                      .setScale(2, RoundingMode.HALF_UP)
                                      .doubleValue();
    
        assertEquals(expectedPrice, ticket.getPrice(), 0.001);
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
        fareCalculatorService.calculateFare(ticket, false);
        assertEquals( (24 * Fare.CAR_RATE_PER_HOUR) , ticket.getPrice());
    }

    @Test
    public void processExitingVehicleTest() throws Exception {
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, new FareCalculatorService());

        lenient().when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket);
        lenient().when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(2); // Utilisateur régulier
        lenient().when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
        lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

        parkingService.processExitingVehicle();

        verify(ticketDAO, times(1)).getTicket("ABCDEF");
        verify(ticketDAO, times(1)).getNbTicket("ABCDEF");
        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
    }

    @Test
    public void processExitingVehicleTestUnableToUpdateTicket() throws Exception {
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
    
        ticket.setPrice(0.0);
        when(ticketDAO.getTicket("ABCDEF")).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);
    
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, new FareCalculatorService());
        parkingService.processExitingVehicle();
    
        verify(ticketDAO, times(1)).getTicket("ABCDEF");
        verify(ticketDAO, times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, times(0)).updateParking(any(ParkingSpot.class));
        verify(inputReaderUtil, times(1)).readVehicleRegistrationNumber();
    }

    @Test
    public void processExitingVehicleTestThrowsException() throws Exception {
        lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenThrow(new Exception("Error reading registration number"));

        try {
            parkingService.processExitingVehicle();
        } catch (Exception e) {
            assertEquals("Error reading registration number", e.getMessage());
        }

        verify(ticketDAO, times(0)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, times(0)).updateParking(any(ParkingSpot.class));
    }
}

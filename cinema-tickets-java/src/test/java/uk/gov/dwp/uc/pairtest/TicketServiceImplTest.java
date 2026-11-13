package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class TicketServiceImplTest {

    private SeatReservationService seatReservationService;

    private TicketPaymentService ticketPaymentService;

    private TicketServiceImpl ticketServiceImpl;

    private TicketTypeRequest adult (int numberOfTickets){
        return new TicketTypeRequest(TicketTypeRequest.Type.ADULT, numberOfTickets);
    }

    private TicketTypeRequest child (int numberOfTickets){
        return new TicketTypeRequest(TicketTypeRequest.Type.CHILD, numberOfTickets);
    }

    private TicketTypeRequest infant (int numberOfTickets){
        return new TicketTypeRequest(TicketTypeRequest.Type.INFANT, numberOfTickets);
    }

    @BeforeEach
    void setUp() {
        seatReservationService = Mockito.mock(SeatReservationService.class);
        ticketPaymentService = Mockito.mock(TicketPaymentService.class);
        ticketServiceImpl = new TicketServiceImpl(ticketPaymentService,seatReservationService);
    }

    @Test
    public void purchaseTickets_whenValidPurchase_returnCorrectSeatsAndPrice() throws InvalidPurchaseException {
        ticketServiceImpl.purchaseTickets(1L, adult(1),child(1),infant(1));
        verify(ticketPaymentService).makePayment(1L,40);
        verify(seatReservationService).reserveSeat(1L,2);
    }

    @Test
    public void purchaseTickets_whenAdultTicket_oneSeatWithPrice25pounds() throws InvalidPurchaseException {
        ticketServiceImpl.purchaseTickets(1L, adult(1));
        verify(ticketPaymentService).makePayment(1L,25);
        verify(seatReservationService).reserveSeat(1L,1);
    }

    @Test
    public void purchaseTickets_whenAdultAndChild_purchaseTwoSeatsFor40Pound() throws InvalidPurchaseException {
        ticketServiceImpl.purchaseTickets(1L, adult(1),child(1));
        verify(ticketPaymentService).makePayment(1L,40);
        verify(seatReservationService).reserveSeat(1L,2);
    }

    @ParameterizedTest
    @ValueSource(ints = {1,2})
    public void purchaseTickets_whenAdultAndInfant_purchaseXAdultSeatsFor25XPounds(int ticketAmounts) throws InvalidPurchaseException {
        ticketServiceImpl.purchaseTickets(1L, adult(ticketAmounts),infant(ticketAmounts));
        verify(ticketPaymentService).makePayment(1L,25*ticketAmounts);
        verify(seatReservationService).reserveSeat(1L, ticketAmounts);
    }

    @Test
    public void purchaseTickets_whenSameTicketRequests_purchaseSumOfTicketTypes() throws InvalidPurchaseException {
        ticketServiceImpl.purchaseTickets(1L, adult(2), adult(2));
        verify(ticketPaymentService).makePayment(1L,100);
        verify(seatReservationService).reserveSeat(1L,4);
    }


    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0,-1})
    public void purchaseTickets_whenInvalidAccountId_throwInvalidPurchaseException(Long accountId) {
        Assertions.assertThrows(InvalidPurchaseException.class,()->{
            ticketServiceImpl.purchaseTickets(accountId, adult(2),child(2),infant(2));
        });
        verifyNoInteractions(seatReservationService,ticketPaymentService);
    }

    @Test
    public void purchaseTickets_whenChildTicketBoughtWithoutAdult_throwInvalidPurchaseException() {
        Assertions.assertThrows(InvalidPurchaseException.class,()->{
            ticketServiceImpl.purchaseTickets(1L, child(1));
        });
        verifyNoInteractions(seatReservationService,ticketPaymentService);
    }

    @Test
    public void purchaseTickets_whenInfantTicketBoughtWithoutAdult_throwInvalidPurchaseException() {
        Assertions.assertThrows(InvalidPurchaseException.class,()->{
            ticketServiceImpl.purchaseTickets(1L, infant(1));
        });
        verifyNoInteractions(seatReservationService,ticketPaymentService);
    }

    @Test
    public void purchaseTickets_whenExactly25TicketsBought_successfullyPurchase25Tickets() throws InvalidPurchaseException {
        ticketServiceImpl.purchaseTickets(1L, adult(25));
        verify(seatReservationService).reserveSeat(1L,25);
        verify(ticketPaymentService).makePayment(1L,25*25);

    }

    @Test
    public void purchaseTickets_whenMoreThan25TicketsBought_throwInvalidPurchaseException() {
        Assertions.assertThrows(InvalidPurchaseException.class,()->{
            ticketServiceImpl.purchaseTickets(1L, adult(25), child(1) );
        });
        verifyNoInteractions(seatReservationService,ticketPaymentService);
    }

    @Test
    public void purchaseTickets_whenMoreInfantsThanAdults_throwInvalidPurchaseException() {
        Assertions.assertThrows(InvalidPurchaseException.class,()->{
            ticketServiceImpl.purchaseTickets(1L, adult(1), infant(2));
        });
        verifyNoInteractions(seatReservationService,ticketPaymentService);
    }

    @ParameterizedTest
    @ValueSource(ints = {0,-1})
    public void purchaseTickets_whenInvalidTicketAmountInRequest_throwInvalidPurchaseException(int ticketAmount) {
        Assertions.assertThrows(InvalidPurchaseException.class,()->{
            ticketServiceImpl.purchaseTickets(1L, adult(ticketAmount));
        });
        verifyNoInteractions(seatReservationService,ticketPaymentService);
    }

    @Test
    public void purchaseTickets_whenANullTicketRequestPresent_throwInvalidPurchaseException(){
        Assertions.assertThrows(InvalidPurchaseException.class,()->{
            ticketServiceImpl.purchaseTickets(1L, null,adult(1), child(1) , infant(1));
        });
        verifyNoInteractions(seatReservationService,ticketPaymentService);
    }

    @Test

    public void purchaseTickets_whenNullTicketType_throwInvalidPurchaseException(){
        TicketTypeRequest nullTypeRequest = new TicketTypeRequest(null,1);
        Assertions.assertThrows(InvalidPurchaseException.class,()->{
            ticketServiceImpl.purchaseTickets(1L, nullTypeRequest);
        });
        verifyNoInteractions(seatReservationService,ticketPaymentService);
    }

    @Test
    public void purchaseTickets_whenNoTicketRequests_throwInvalidPurchaseException(){
        Assertions.assertThrows(InvalidPurchaseException.class,()->{
            ticketServiceImpl.purchaseTickets(1L);
        });
        verifyNoInteractions(seatReservationService,ticketPaymentService);
    }

    @Test
    public void purchaseTickets_whenNullTicketRequests_throwInvalidPurchaseException(){
        Assertions.assertThrows(InvalidPurchaseException.class,()->{
            ticketServiceImpl.purchaseTickets(1L, (TicketTypeRequest[]) null);
        });
        verifyNoInteractions(seatReservationService,ticketPaymentService);
    }
}

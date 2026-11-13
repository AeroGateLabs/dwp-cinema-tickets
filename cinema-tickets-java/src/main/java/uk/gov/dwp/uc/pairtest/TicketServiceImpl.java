package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.HashMap;
import java.util.Map;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */

    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;
    private static final int MAX_TICKETS_PER_PURCHASE = 25;
    private static final int ADULT_TICKET_PRICE = 25;
    private static final int CHILD_TICKET_PRICE = 15;

    public TicketServiceImpl(TicketPaymentService ticketPaymentService, SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        if (accountId == null || accountId <= 0 || ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException();
        }

        Map<TicketTypeRequest.Type, Integer> ticketMap = new HashMap<TicketTypeRequest.Type, Integer>();

        for (TicketTypeRequest ticketTypeRequest : ticketTypeRequests) {
            if (ticketTypeRequest == null || ticketTypeRequest.getNoOfTickets() <= 0 || ticketTypeRequest.getTicketType() == null) {
                throw new InvalidPurchaseException();
            }
            ticketMap.merge(ticketTypeRequest.getTicketType(), ticketTypeRequest.getNoOfTickets(), Integer::sum);
        }

        if (!isValidRequest(ticketMap)){
            throw new InvalidPurchaseException();
        }

        int totalAmountToPay = calculateTotalPrice(ticketMap);
        ticketPaymentService.makePayment(accountId, totalAmountToPay);

        int totalSeatsToAllocate = calculateTotalSeats(ticketMap);
        seatReservationService.reserveSeat(accountId, totalSeatsToAllocate);
    }

    private boolean isValidRequest (Map<TicketTypeRequest.Type, Integer> ticketMap) {
        int totalTickets = ticketMap.values().stream().mapToInt(Integer::intValue).sum();

        if (totalTickets > MAX_TICKETS_PER_PURCHASE) {
            return false;
        }

        if (!ticketMap.containsKey(TicketTypeRequest.Type.ADULT)){
            return false;
        }

        if (ticketMap.getOrDefault(TicketTypeRequest.Type.INFANT,0) > (ticketMap.getOrDefault(TicketTypeRequest.Type.ADULT,0))) {
            return false;
        }

        return true;
    }

    private int calculateTotalPrice (Map<TicketTypeRequest.Type, Integer> ticketMap) {
        int price = 0;
        for (TicketTypeRequest.Type ticketType: ticketMap.keySet()){
            if(ticketType.equals(TicketTypeRequest.Type.CHILD)){
                price += (ticketMap.get(ticketType) * CHILD_TICKET_PRICE);
            }
            if(ticketType.equals(TicketTypeRequest.Type.ADULT)) {
                price += (ticketMap.get(ticketType) * ADULT_TICKET_PRICE);
            }
        }
        return price;
    }

    private int calculateTotalSeats (Map<TicketTypeRequest.Type, Integer> ticketMap) {
        int seats = 0;
        for (TicketTypeRequest.Type ticketType: ticketMap.keySet()){
            if(ticketType.equals(TicketTypeRequest.Type.CHILD)){
                seats += (ticketMap.get(ticketType));
            }
            if(ticketType.equals(TicketTypeRequest.Type.ADULT)) {
                seats += (ticketMap.get(ticketType));
            }
        }
        return seats;
    }
}

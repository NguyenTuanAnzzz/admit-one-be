package com.an.admit_one.service;

import com.an.admit_one.dto.request.ScanTicketRequest;
import com.an.admit_one.dto.response.CheckinEventResponse;
import com.an.admit_one.dto.response.CheckinStatsResponse;
import com.an.admit_one.dto.response.ScanTicketResponse;
import com.an.admit_one.entity.Event;
import com.an.admit_one.entity.IssuedTicket;
import com.an.admit_one.enums.IssuedTicketStatus;
import com.an.admit_one.exception.AppException;
import com.an.admit_one.repository.EventRepository;
import com.an.admit_one.repository.IssuedTicketRepository;
import com.an.admit_one.security.UserPrincipal;
import com.an.admit_one.specification.EventSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CheckinService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private IssuedTicketRepository issuedTicketRepository;

    // Danh sách sự kiện để Staff/Admin chọn khi vào trang check-in
    public List<CheckinEventResponse> listEvents(String keyword) {
        Specification<Event> spec = Specification
                .where(EventSpecifications.checkinEligible())
                .and(EventSpecifications.search(keyword));

        return eventRepository.findAll(spec, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "startsAt")))
                .map(e -> new CheckinEventResponse(e.getId(), e.getName(), e.getVenue(), e.getStartsAt(), e.getStatus().name()))
                .getContent();
    }

    public CheckinStatsResponse getStats(UUID eventId) {
        long total = issuedTicketRepository.countByEventId(eventId);
        long checkedIn = issuedTicketRepository.countByEventIdAndStatus(eventId, IssuedTicketStatus.CHECKED_IN);
        return new CheckinStatsResponse(checkedIn, total);
    }

    @Transactional
    public ScanTicketResponse scan(ScanTicketRequest request) {
        IssuedTicket ticket = issuedTicketRepository.findByQrSecret(request.getQrSecret()).orElse(null);

        if (ticket == null) {
            return new ScanTicketResponse("NOT_FOUND", null, null, null, null);
        }

        if (!ticket.getEvent().getId().equals(request.getEventId())) {
            return new ScanTicketResponse("WRONG_EVENT", ticket.getCode(), ticket.getTicket().getName(), ticket.getOrder().getUser().getFullName(), null);
        }

        if (ticket.getStatus() == IssuedTicketStatus.VOID) {
            return new ScanTicketResponse("VOID", ticket.getCode(), ticket.getTicket().getName(), ticket.getOrder().getUser().getFullName(), null);
        }

        if (ticket.getStatus() == IssuedTicketStatus.CHECKED_IN) {
            return new ScanTicketResponse("ALREADY_CHECKED_IN", ticket.getCode(), ticket.getTicket().getName(), ticket.getOrder().getUser().getFullName(), ticket.getCheckedInAt());
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        Instant now = Instant.now();
        int updated = issuedTicketRepository.checkInIfValid(ticket.getId(), now, principal.getUser().getId());

        if (updated == 0) {
            // Vừa bị người khác quét trước trong lúc mình xử lý
            return new ScanTicketResponse("ALREADY_CHECKED_IN", ticket.getCode(), ticket.getTicket().getName(), ticket.getOrder().getUser().getFullName(), null);
        }

        return new ScanTicketResponse("OK", ticket.getCode(), ticket.getTicket().getName(), ticket.getOrder().getUser().getFullName(), now);
    }
}
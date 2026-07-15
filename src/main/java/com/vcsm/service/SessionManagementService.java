package com.vcsm.service;

import com.vcsm.model.CustomerSession;
import com.vcsm.model.SessionTurn;
import com.vcsm.repository.CustomerSessionRepository;
import com.vcsm.repository.SessionTurnRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
@lombok.RequiredArgsConstructor
public class SessionManagementService {
    private static final Logger log = LoggerFactory.getLogger(SessionManagementService.class);

    private final CustomerSessionRepository sessionRepository;

    private final SessionTurnRepository turnRepository;

    private static final int SESSION_ARCHIVAL_DAYS = 90;
    private static final int MAX_RECENT_SESSIONS = 50;

    @Transactional
    public CustomerSession createSession(String customerId) {
        CustomerSession session = new CustomerSession(customerId);
        return sessionRepository.save(session);
    }

    @Transactional
    public void addSessionTurn(String sessionId, String speaker, String content) {
        Optional<CustomerSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        CustomerSession session = sessionOpt.get();
        List<SessionTurn> turns = turnRepository.findBySessionIdOrderByTurnIndexAsc(sessionId);
        int nextIndex = turns.isEmpty() ? 0 : turns.size();

        SessionTurn turn = new SessionTurn(session, nextIndex, speaker, content);
        turnRepository.save(turn);
    }

    @Transactional
    public void endSession(String sessionId, String intent, String resolutionStatus) {
        Optional<CustomerSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            CustomerSession session = sessionOpt.get();
            session.setEndedAt(LocalDateTime.now());
            session.setIntent(intent);
            session.setResolutionStatus(resolutionStatus);

            List<SessionTurn> turns = turnRepository.findBySessionIdOrderByTurnIndexAsc(sessionId);
            StringBuilder transcript = new StringBuilder();
            for (SessionTurn turn : turns) {
                transcript.append("[").append(turn.getSpeaker()).append("] ")
                         .append(turn.getContent()).append("\n");
            }
            session.setTranscript(transcript.toString());
            sessionRepository.save(session);
        }
    }

    public List<CustomerSession> getRecentSessions(String customerId) {
        List<CustomerSession> sessions = sessionRepository.findRecentSessionsForCustomer(customerId);
        return sessions.size() <= MAX_RECENT_SESSIONS ? sessions : sessions.subList(0, MAX_RECENT_SESSIONS);
    }

    public Optional<CustomerSession> getSessionWithTranscript(String sessionId) {
        return sessionRepository.findById(sessionId);
    }

    @Transactional
    public void updateResolutionStatus(String sessionId, String newStatus) {
        Optional<CustomerSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            CustomerSession session = sessionOpt.get();
            session.setResolutionStatus(newStatus);
            sessionRepository.save(session);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void archiveOldSessions() {
        LocalDateTime cutoffDate = LocalDateTime.now().minus(SESSION_ARCHIVAL_DAYS, ChronoUnit.DAYS);
        List<CustomerSession> sessionsToArchive = sessionRepository.findSessionsEligibleForArchival(cutoffDate);

        for (CustomerSession session : sessionsToArchive) {
            session.setArchived(true);
            session.setArchivedAt(LocalDateTime.now());
            sessionRepository.save(session);
        }

        if (!sessionsToArchive.isEmpty()) {
            log.info("Archived " + sessionsToArchive.size() + " sessions older than " + SESSION_ARCHIVAL_DAYS + " days");
        }
    }

    public long getSessionCountForCustomer(String customerId) {
        return sessionRepository.countByCustomerId(customerId);
    }
}

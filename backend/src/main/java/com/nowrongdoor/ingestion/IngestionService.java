package com.nowrongdoor.ingestion;

import com.nowrongdoor.adapters.rest.RestFetchResult;
import com.nowrongdoor.adapters.rest.RestResident;
import com.nowrongdoor.adapters.rest.RestSourceAdapter;
import com.nowrongdoor.adapters.xml.XmlFetchResult;
import com.nowrongdoor.adapters.xml.XmlRecord;
import com.nowrongdoor.adapters.xml.XmlSourceAdapter;
import com.nowrongdoor.matching.MatchResult;
import com.nowrongdoor.matching.MatchingService;
import com.nowrongdoor.model.IngestionRun;
import com.nowrongdoor.model.MatchStatus;
import com.nowrongdoor.model.UnifiedResident;
import com.nowrongdoor.repository.IngestionRunRepository;
import com.nowrongdoor.repository.UnifiedResidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class IngestionService {
    private final RestSourceAdapter restAdapter;
    private final XmlSourceAdapter xmlAdapter;
    private final MatchingService matchingService;
    private final UnifiedResidentRepository residentRepository;
    private final IngestionRunRepository runRepository;

    public IngestionService(RestSourceAdapter restAdapter,
                            XmlSourceAdapter xmlAdapter,
                            MatchingService matchingService,
                            UnifiedResidentRepository residentRepository,
                            IngestionRunRepository runRepository) {
        this.restAdapter = restAdapter;
        this.xmlAdapter = xmlAdapter;
        this.matchingService = matchingService;
        this.residentRepository = residentRepository;
        this.runRepository = runRepository;
    }

    @Transactional(noRollbackFor = RuntimeException.class)
    public IngestionResult ingest() {
        Instant started = Instant.now();
        IngestionRun run = runRepository.save(new IngestionRun()
                .startedAt(started)
                .status(IngestionRun.Status.RUNNING));

        RestFetchResult restResult;
        XmlFetchResult xmlResult;
        try {
            restResult = restAdapter.fetchAllResidents();
        } catch (Exception e) {
            restResult = new RestFetchResult.Failure("REST source failure: " + message(e), e);
        }
        try {
            xmlResult = xmlAdapter.fetchAllRecords();
        } catch (Exception e) {
            xmlResult = new XmlFetchResult.Failure("XML source failure: " + message(e), 0, e);
        }

        boolean restAvailable = restResult instanceof RestFetchResult.Success;
        boolean xmlAvailable = xmlResult instanceof XmlFetchResult.Success;
        List<RestResident> rest = restResult instanceof RestFetchResult.Success success
                ? success.residents() : List.of();
        List<XmlRecord> xml = xmlResult instanceof XmlFetchResult.Success success
                ? success.records() : List.of();

        try {
            List<MatchResult> matches = matchingService.match(rest, xml);
            Instant finished = Instant.now();
            matches.forEach(match -> upsert(match, finished));
            populateRun(run, restResult, xmlResult, matches, restAvailable, xmlAvailable,
                    started, finished);
            runRepository.save(run);
            return IngestionResult.from(run);
        } catch (Exception e) {
            Instant finished = Instant.now();
            run.status(IngestionRun.Status.FAILED)
                    .finishedAt(finished)
                    .errors(appendErrors(run.getErrors(), "Persistence failure: " + message(e)));
            runRepository.save(run);
                return IngestionResult.from(run);
        }
    }

    public Optional<IngestionRun> latestRun() {
        return runRepository.findTopByOrderByStartedAtDesc();
    }

    private void populateRun(IngestionRun run,
                             RestFetchResult restResult,
                             XmlFetchResult xmlResult,
                             List<MatchResult> matches,
                             boolean restAvailable,
                             boolean xmlAvailable,
                             Instant started,
                             Instant finished) {
        int restCount = restResult instanceof RestFetchResult.Success success ? success.residents().size() : 0;
        int xmlCount = xmlResult instanceof XmlFetchResult.Success success ? success.records().size() : 0;
        String errors = null;
        if (restResult instanceof RestFetchResult.Failure failure) errors = appendErrors(errors, failure.message());
        if (xmlResult instanceof XmlFetchResult.Failure failure) errors = appendErrors(errors, failure.message());

        run.restRecordsFetched(restCount)
                .restDuplicatesDropped(restResult instanceof RestFetchResult.Success success ? success.duplicatesDropped() : 0)
                .restPagesFetched(restResult instanceof RestFetchResult.Success success ? success.pagesFetched() : 0)
                .xmlRecordsFetched(xmlCount)
                .xmlAttempts(xmlResult instanceof XmlFetchResult.Success success ? success.attempts()
                        : xmlResult instanceof XmlFetchResult.Failure failure ? failure.attempts() : 0)
                .xmlSucceeded(xmlAvailable)
                .exactMatches(count(matches, MatchStatus.EXACT))
                .probableMatches(count(matches, MatchStatus.PROBABLE))
                .ambiguousMatches(count(matches, MatchStatus.AMBIGUOUS))
                .restOnly(count(matches, MatchStatus.REST_ONLY))
                .xmlOnly(count(matches, MatchStatus.XML_ONLY))
                .finishedAt(finished)
                .status(!restAvailable && !xmlAvailable ? IngestionRun.Status.FAILED
                        : restAvailable && xmlAvailable ? IngestionRun.Status.SUCCESS
                        : IngestionRun.Status.PARTIAL)
                .errors(errors);
    }

    private void upsert(MatchResult match, Instant ingestedAt) {
        UnifiedResident resident = findExisting(match);
        if (resident == null) resident = new UnifiedResident();
        RestResident rest = match.rest();
        XmlRecord xml = match.xml();
        if (rest != null) {
            resident.restId(rest.getId()).restFirstName(rest.getFirstName()).restLastName(rest.getLastName())
                    .restDateOfBirth(rest.getDateOfBirth()).restAddressLine(rest.getAddressLine())
                    .restCity(rest.getCity()).restPhone(rest.getPhone()).restProgramStatus(rest.getProgramStatus())
                    .restLastContact(rest.getLastContact());
        }
        if (xml != null) {
            resident.xmlRef(xml.getRef()).xmlName(xml.getName()).xmlBorn(xml.getBorn()).xmlAddr(xml.getAddr())
                    .xmlTown(xml.getTown()).xmlBenefitCode(xml.getBenefitCode()).xmlReviewDue(xml.getReviewDue());
        }
        resident.matchStatus(match.status()).matchConfidence(match.matchConfidence())
                .matchNotes(match.matchNotes()).ingestedAt(ingestedAt);
        residentRepository.save(resident);
    }

    private UnifiedResident findExisting(MatchResult match) {
        Optional<UnifiedResident> byXml = Optional.empty();
        if (match.xml() != null && nonBlank(match.xml().getRef())) {
            byXml = residentRepository.findByXmlRef(match.xml().getRef());
        }
        if (match.rest() != null && nonBlank(match.rest().getId())) {
            Optional<UnifiedResident> byRest = residentRepository.findByRestId(match.rest().getId());
            if (byRest.isPresent()) {
                if (byXml.isPresent() && byXml.get() != byRest.get()) {
                    residentRepository.delete(byXml.get());
                }
                return byRest.get();
            }
        }
        return byXml.orElseGet(() -> match.xml() != null && nonBlank(match.xml().getRef())
                ? residentRepository.findByXmlRef(match.xml().getRef()).orElse(null)
                : null);
    }

    private static int count(List<MatchResult> matches, MatchStatus status) {
        return (int) matches.stream().filter(match -> match.status() == status).count();
    }

    private static String appendErrors(String current, String next) {
        if (next == null || next.isBlank()) return current;
        return current == null || current.isBlank() ? next : current + "; " + next;
    }

    private static String message(Exception exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    private static boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }
}

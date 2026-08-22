package com.nowrongdoor.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Audit record for one ingestion pipeline run.
 * <p>
 * Every time {@code POST /api/ingest} is called, a new {@code IngestionRun}
 * is created at the start and updated at the end with statistics and status.
 * All fields are [app-generated].
 */
@Entity
@Table(name = "ingestion_runs")
public class IngestionRun {

    public enum Status { RUNNING, COMPLETE, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    // ── REST statistics ────────────────────────────────────────────────────
    @Column(name = "rest_records_fetched")
    private int restRecordsFetched;

    @Column(name = "rest_duplicates_dropped")
    private int restDuplicatesDropped;

    @Column(name = "rest_pages_fetched")
    private int restPagesFetched;

    // ── XML statistics ────────────────────────────────────────────────────
    @Column(name = "xml_records_fetched")
    private int xmlRecordsFetched;

    @Column(name = "xml_attempts")
    private int xmlAttempts;

    @Column(name = "xml_succeeded")
    private boolean xmlSucceeded;

    // ── Matching statistics ───────────────────────────────────────────────
    @Column(name = "exact_matches")
    private int exactMatches;

    @Column(name = "probable_matches")
    private int probableMatches;

    @Column(name = "ambiguous_matches")
    private int ambiguousMatches;

    @Column(name = "rest_only")
    private int restOnly;

    @Column(name = "xml_only")
    private int xmlOnly;

    /** Any error messages encountered during this run. */
    @Column(name = "errors", length = 2000)
    private String errors;

    // ── Builder-style setters ─────────────────────────────────────────────

    public IngestionRun startedAt(Instant v)          { this.startedAt = v; return this; }
    public IngestionRun finishedAt(Instant v)         { this.finishedAt = v; return this; }
    public IngestionRun status(Status v)              { this.status = v; return this; }
    public IngestionRun restRecordsFetched(int v)     { this.restRecordsFetched = v; return this; }
    public IngestionRun restDuplicatesDropped(int v)  { this.restDuplicatesDropped = v; return this; }
    public IngestionRun restPagesFetched(int v)       { this.restPagesFetched = v; return this; }
    public IngestionRun xmlRecordsFetched(int v)      { this.xmlRecordsFetched = v; return this; }
    public IngestionRun xmlAttempts(int v)            { this.xmlAttempts = v; return this; }
    public IngestionRun xmlSucceeded(boolean v)       { this.xmlSucceeded = v; return this; }
    public IngestionRun exactMatches(int v)           { this.exactMatches = v; return this; }
    public IngestionRun probableMatches(int v)        { this.probableMatches = v; return this; }
    public IngestionRun ambiguousMatches(int v)       { this.ambiguousMatches = v; return this; }
    public IngestionRun restOnly(int v)               { this.restOnly = v; return this; }
    public IngestionRun xmlOnly(int v)                { this.xmlOnly = v; return this; }
    public IngestionRun errors(String v)              { this.errors = v; return this; }

    // ── Getters ───────────────────────────────────────────────────────────

    public Long getId()                  { return id; }
    public Instant getStartedAt()        { return startedAt; }
    public Instant getFinishedAt()       { return finishedAt; }
    public Status getStatus()            { return status; }
    public int getRestRecordsFetched()   { return restRecordsFetched; }
    public int getRestDuplicatesDropped(){ return restDuplicatesDropped; }
    public int getRestPagesFetched()     { return restPagesFetched; }
    public int getXmlRecordsFetched()    { return xmlRecordsFetched; }
    public int getXmlAttempts()          { return xmlAttempts; }
    public boolean isXmlSucceeded()      { return xmlSucceeded; }
    public int getExactMatches()         { return exactMatches; }
    public int getProbableMatches()      { return probableMatches; }
    public int getAmbiguousMatches()     { return ambiguousMatches; }
    public int getRestOnly()             { return restOnly; }
    public int getXmlOnly()              { return xmlOnly; }
    public String getErrors()            { return errors; }

    public int totalResidents() {
        return exactMatches + probableMatches + ambiguousMatches + restOnly + xmlOnly;
    }
}

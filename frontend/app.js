(() => {
    'use strict';

    const API_BASE = 'http://localhost:8080';
    const POLL_INTERVAL = 15_000;
    const $ = (id) => document.getElementById(id);

    // ── DOM references ──────────────────────────────────────────────
    const searchInput      = $('search-input');
    const searchButton     = $('search-button');
    const statusFilter     = $('status-filter');
    const resultsList      = $('results-list');
    const resultsCount     = $('results-count');
    const errorMessage     = $('error-message');
    const detailSection    = $('detail-section');
    const detailContent    = $('detail-content');
    const ingestionMessage = $('ingestion-message');
    const clearSearch      = $('clear-search');

    // ── State display ───────────────────────────────────────────────
    function showState(state) {
        ['state-empty', 'state-loading', 'state-error', 'state-no-results', 'results-list'].forEach((id) => {
            $(id).hidden = id !== (state === 'results' ? 'results-list' : `state-${state}`);
        });
        resultsCount.hidden = state !== 'results';
    }

    function setBusy(button, busy, label) {
        button.disabled = busy;
        const text = button.querySelector('.btn-text');
        if (text) text.textContent = busy ? label : button.dataset.label;
        else button.textContent = busy ? label : button.dataset.label;
        const spinner = button.querySelector('.btn-spinner');
        if (spinner) spinner.hidden = !busy;
    }

    // ── API helper ──────────────────────────────────────────────────
    async function api(path, options = {}) {
        const response = await fetch(`${API_BASE}${path}`, options);
        const data = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(data.error || `Backend returned ${response.status}`);
        return data;
    }

    // ── Search ──────────────────────────────────────────────────────
    async function performSearch() {
        const query = searchInput.value.trim();
        if (!query) {
            showState('empty');
            return;
        }
        showState('loading');
        setBusy(searchButton, true, 'Searching...');
        try {
            const params = new URLSearchParams({ q: query });
            if (statusFilter.value) params.set('status', statusFilter.value);
            const data = await api(`/api/residents/search?${params}`);
            if (!data.results?.length) {
                showState('no-results');
                return;
            }
            renderResults(data.results);
            resultsCount.textContent = `${data.totalResults} result${data.totalResults === 1 ? '' : 's'}`;
            showState('results');
        } catch (error) {
            errorMessage.textContent = error.message;
            showState('error');
        } finally {
            setBusy(searchButton, false);
        }
    }

    // ── Render helpers ──────────────────────────────────────────────
    function statusBadge(status) {
        return `<span class="status-pill status-pill--${status.toLowerCase()}">${escapeHtml(status)}</span>`;
    }

    function renderResults(results) {
        resultsList.innerHTML = '';
        results.forEach((resident) => {
            const card = document.createElement('button');
            card.type = 'button';
            card.className = 'resident-card';
            card.innerHTML = `
                <div class="resident-avatar">${escapeHtml(initials(resident.name))}</div>
                <div class="resident-info">
                    <div class="resident-name">${escapeHtml(resident.name || 'Unnamed resident')}</div>
                    <div class="resident-details">
                        <span>${escapeHtml(resident.dateOfBirth || 'DOB unknown')}</span>
                        <span>${escapeHtml(resident.city || 'Location unknown')}</span>
                    </div>
                    <div class="resident-address">${escapeHtml(resident.address || 'Address unavailable')}</div>
                    <div class="resident-sources">${resident.sourceAvailability?.rest ? '<span class="resident-source resident-source--rest">REST</span>' : ''}${resident.sourceAvailability?.xml ? '<span class="resident-source resident-source--xml">XML</span>' : ''}</div>
                </div>
                <div class="resident-result-meta">${statusBadge(resident.matchStatus)}<span>${escapeHtml(String(resident.matchConfidence))}%</span><span class="confidence-track"><span style="width:${Math.min(100, Number(resident.matchConfidence) || 0)}%"></span></span></div>`;
            card.addEventListener('click', () => loadDetail(resident.id));
            resultsList.appendChild(card);
        });
    }

    // ── Resident detail ─────────────────────────────────────────────
    async function loadDetail(id) {
        detailSection.hidden = false;
        detailContent.innerHTML = '<div class="detail-loading">Loading resident detail...</div>';
        try {
            const resident = await api(`/api/residents/${encodeURIComponent(id)}`);
            detailContent.innerHTML = detailMarkup(resident);
            detailSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
        } catch (error) {
            detailContent.innerHTML = `<p class="state-message">${escapeHtml(error.message)}</p>`;
        }
    }

    function detailMarkup(resident) {
        const rest = resident.rest || {};
        const xml  = resident.xml  || {};
        const availability = `${resident.sourceAvailability?.rest ? 'REST available' : 'REST unavailable'} · ${resident.sourceAvailability?.xml ? 'XML available' : 'XML unavailable'}`;
        const reviewMessage = resident.matchStatus === 'AMBIGUOUS'
            ? '<p class="review-warning">⚠ MANUAL REVIEW REQUIRED</p>'
            : resident.matchStatus === 'EXACT'
                ? '<p class="review-success">✓ Verified match.</p>'
                : '';
        const rows = [
            ['ID / Ref',        rest.id,                                            xml.ref,          'MATCH'],
            ['Name',            [rest.firstName, rest.lastName].filter(Boolean).join(' '), xml.name,  'NORMALIZED MATCH'],
            ['Date of Birth',   rest.dateOfBirth,                                   xml.born,         'MATCH'],
            ['Address',         rest.address,                                        xml.address,      'NORMALIZED MATCH'],
            ['City / Town',     rest.city,                                           xml.town,         'MATCH'],
            ['Phone',           rest.phone,                                          null,             'NOT AVAILABLE'],
            ['Program Status',  rest.programStatus,                                  null,             'NOT AVAILABLE'],
            ['Last Contact',    rest.lastContact,                                    null,             'NOT AVAILABLE'],
            ['Benefit Code',    null,                                                xml.benefitCode,  'NOT AVAILABLE'],
            ['Review Due',      null,                                                xml.reviewDue,    'NOT AVAILABLE'],
        ];
        return `
            <div class="detail-heading">
                <h3>${escapeHtml(resident.name || 'Unnamed resident')}</h3>
                ${statusBadge(resident.matchStatus)}
                <strong>Confidence: ${resident.matchConfidence}%</strong>
                <span class="detail-availability">${escapeHtml(availability)}</span>
            </div>
            ${reviewMessage}
            <p class="detail-note">${escapeHtml(resident.matchNotes || '')}</p>
            <div class="comparison-wrap">
                <table class="comparison-table">
                    <thead><tr><th scope="col">Field</th><th scope="col">REST SOURCE</th><th scope="col">XML SOURCE</th><th scope="col">Comparison</th></tr></thead>
                    <tbody>${rows.map((row) => comparisonRow(row, resident)).join('')}</tbody>
                </table>
            </div>
            <section class="evidence-section">
                <h4>Why was this record matched?</h4>
                <ul>${(resident.evidence || []).map((item) =>
                    `<li class="evidence-${item.comparison.toLowerCase().replace(/\s+/g, '-')}">
                        <strong>${escapeHtml(item.comparison)}</strong>
                        <span>${escapeHtml(item.reason)}</span>
                        ${item.candidateRefs?.length ? `<small>Candidates: ${escapeHtml(item.candidateRefs.join(', '))}</small>` : ''}
                    </li>`
                ).join('')}</ul>
            </section>`;
    }

    function comparisonRow(row, resident) {
        const [field, restValue, xmlValue, defaultComparison] = row;
        const restPresent = restValue !== undefined && restValue !== null && restValue !== '';
        const xmlPresent  = xmlValue  !== undefined && xmlValue  !== null && xmlValue  !== '';
        let comparison = defaultComparison;
        if (!restPresent || !xmlPresent) comparison = (restPresent || xmlPresent) ? 'NOT AVAILABLE' : 'MISSING';
        if (restPresent && xmlPresent && field === 'Address'      && resident.evidence?.some((item) => item.field === 'address and town' && item.comparison === 'DIFFERENT')) comparison = 'DIFFERENT';
        if (restPresent && xmlPresent && field === 'Date of Birth' && resident.evidence?.some((item) => item.field === 'date of birth'    && item.comparison === 'DIFFERENT')) comparison = 'DIFFERENT';
        return `<tr class="comparison-${comparison.toLowerCase().replace(/\s+/g, '-')}">
            <th scope="row">${escapeHtml(field)}</th>
            <td>${escapeHtml(restPresent ? restValue : sourceMissing('REST', resident.matchStatus))}</td>
            <td>${escapeHtml(xmlPresent  ? xmlValue  : sourceMissing('XML',  resident.matchStatus))}</td>
            <td><span class="comparison-label">${escapeHtml(comparison)}</span></td>
        </tr>`;
    }

    function sourceMissing(source, status) {
        if (source === 'REST' && status === 'XML_ONLY') return 'No matching REST resident record found.';
        if (source === 'XML'  && status === 'REST_ONLY') return 'No matching XML benefits record found.';
        return 'Unavailable';
    }

    // ── Stats / dashboard refresh ───────────────────────────────────
    async function refreshStats() {
        try {
            const stats = await api('/api/status');
            applyStats(stats);
        } catch (_) {
            $('last-ingestion').textContent = 'Backend unavailable';
        }
    }

    function applyStats(stats) {
        [
            ['total',    stats.totalResidents],
            ['exact',    stats.exact],
            ['probable', stats.probable],
            ['ambiguous',stats.ambiguous],
            ['rest-only',stats.restOnly],
            ['xml-only', stats.xmlOnly],
        ].forEach(([id, value]) => {
            const el = $(`stat-${id}`);
            if (el) el.textContent = value ?? '-';
        });

        // Update legend values if present
        if ($('legend-exact'))    $('legend-exact').textContent    = stats.exact    ?? '-';
        if ($('legend-probable')) $('legend-probable').textContent = stats.probable ?? '-';
        if ($('legend-ambiguous'))$('legend-ambiguous').textContent= stats.ambiguous?? '-';
        if ($('legend-rest'))     $('legend-rest').textContent     = stats.restOnly ?? '-';
        if ($('legend-xml'))      $('legend-xml').textContent      = stats.xmlOnly  ?? '-';

        const unified = Number(stats.totalResidents || 0);
        $('unified-record-total').textContent = unified.toLocaleString();

        // source-record-total comes from /api/ingest (restRecords + xmlRecords).
        // It is stored in sessionStorage at ingest time — never reconstructed from match categories.
        const storedSourceTotal = sessionStorage.getItem('nwd-source-total');
        if (storedSourceTotal) $('source-record-total').textContent = storedSourceTotal;

        $('last-ingestion').textContent = stats.lastIngestionStatus
            ? `${stats.lastIngestionStatus} · ${stats.lastIngestionTime || 'in progress'}`
            : 'No ingestion run';
    }

    // ── Source health polling ───────────────────────────────────────
    async function checkStatus(source) {
        try { return await api(`/api/status/${source}`); }
        catch (_) { return { status: 'DOWN' }; }
    }

    function updateStatusUI(source, data) {
        const status = data.status || 'DOWN';
        $(`status-${source}`).dataset.status = status;
        $(`source-card-${source}`).dataset.status = status;
        $(`source-status-${source}`).textContent = status === 'UP' ? 'Connected' : `${source.toUpperCase()} source unavailable`;
    }

    async function pollStatuses() {
        const [rest, xml] = await Promise.all([checkStatus('rest'), checkStatus('xml')]);
        updateStatusUI('rest', rest);
        updateStatusUI('xml',  xml);
    }

    // ── Utilities ───────────────────────────────────────────────────
    function initials(name) {
        return (name || '?').split(/\s+/).slice(0, 2).map((part) => part[0]).join('');
    }

    function escapeHtml(value) {
        const div = document.createElement('div');
        div.textContent = value ?? '';
        return div.innerHTML;
    }

    // ── Event listeners ─────────────────────────────────────────────
    searchButton.dataset.label = 'Search';
    searchButton.addEventListener('click', performSearch);
    searchInput.addEventListener('input', () => { clearSearch.hidden = !searchInput.value; });
    clearSearch.addEventListener('click', () => { searchInput.value = ''; clearSearch.hidden = true; searchInput.focus(); showState('empty'); });
    searchInput.addEventListener('keydown', (event) => { if (event.key === 'Enter') performSearch(); });
    statusFilter.addEventListener('change', performSearch);
    $('detail-close').addEventListener('click', () => { detailSection.hidden = true; });

    // ── Auto-ingestion on startup (fires at most once per page load) ─
    let autoIngestionDone = false;

    async function autoInit() {
        showState('empty');
        pollStatuses();
        setInterval(pollStatuses, POLL_INTERVAL);

        // Step 1 — fetch current status
        let stats;
        try {
            stats = await api('/api/status');
        } catch (_) {
            // Backend not reachable yet — fall back to refreshStats display
            await refreshStats();
            return;
        }

        // Step 2 — update stat cards immediately with what we have
        applyStats(stats);

        // Step 3 — if database is empty and no ingestion has ever run, auto-ingest (once)
        if (stats.totalResidents === 0 && stats.lastIngestionStatus === null && !autoIngestionDone) {
            autoIngestionDone = true;

            ingestionMessage.hidden = false;
            ingestionMessage.className = 'ingestion-message ingestion-message--running';
            ingestionMessage.textContent = 'Reconciling REST and XML records...';

            try {
                const result = await api('/api/ingest', { method: 'POST' });
                const total  = Number(result.restRecords || 0) + Number(result.xmlRecords || 0);

                // Store authoritative raw-record count before refreshStats() so applyStats() reads it correctly
                sessionStorage.setItem('nwd-source-total', total.toLocaleString());

                // Refresh stats — applyStats() inside will read the sessionStorage value above
                await refreshStats();

                // Now build the success message from the live stat element
                const unifiedCount = $('stat-total')?.textContent || '0';
                const sourceCount  = $('source-record-total')?.textContent || total.toLocaleString();

                if (result.status === 'PARTIAL') {
                    ingestionMessage.className = 'ingestion-message ingestion-message--partial';
                    ingestionMessage.textContent = `Data loaded with source limitations. (${result.restRecords} REST + ${result.xmlRecords} XML records)${result.error ? ' — ' + result.error : ''}`;
                } else if (result.status === 'SUCCESS') {
                    ingestionMessage.className = 'ingestion-message ingestion-message--success';
                    ingestionMessage.textContent = `${unifiedCount} residents loaded from ${sourceCount} source records.`;
                } else {
                    ingestionMessage.className = 'ingestion-message ingestion-message--partial';
                    ingestionMessage.textContent = `${result.status}: ${result.restRecords} REST, ${result.xmlRecords} XML records.`;
                }
                // Results section stays in "No search yet" state — user must search explicitly.

            } catch (error) {
                ingestionMessage.className = 'ingestion-message ingestion-message--failed';
                ingestionMessage.textContent = `Unable to load resident data. Check source availability. (${error.message})`;
            }

        }
        // Data already exists — stats are shown above; results section waits for explicit user search.
    }

    autoInit();
})();

(() => {
    'use strict';

    const API_BASE = 'http://localhost:8080';
    const POLL_INTERVAL = 15_000;
    const $ = (id) => document.getElementById(id);

    const searchInput = $('search-input');
    const searchButton = $('search-button');
    const statusFilter = $('status-filter');
    const ingestButton = $('ingest-button');
    const resultsList = $('results-list');
    const resultsCount = $('results-count');
    const errorMessage = $('error-message');
    const detailSection = $('detail-section');
    const detailContent = $('detail-content');
    const ingestionMessage = $('ingestion-message');

    function showState(state) {
        ['state-empty', 'state-loading', 'state-error', 'state-no-results', 'results-list'].forEach((id) => {
            $(id).hidden = id !== (state === 'results' ? 'results-list' : `state-${state}`);
        });
        resultsCount.hidden = state !== 'results';
    }

    function setBusy(button, busy, label) {
        button.disabled = busy;
        button.textContent = busy ? label : button.dataset.label;
    }

    async function api(path, options = {}) {
        const response = await fetch(`${API_BASE}${path}`, options);
        const data = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(data.error || `Backend returned ${response.status}`);
        return data;
    }

    async function performSearch() {
        const query = searchInput.value.trim();
        if (!query && !statusFilter.value) {
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
                        <span>${escapeHtml(resident.sourceAvailability?.rest ? 'REST' : '')}${resident.sourceAvailability?.xml ? ' + XML' : ''}</span>
                    </div>
                    <div class="resident-address">${escapeHtml(resident.address || 'Address unavailable')}</div>
                </div>
                <div class="resident-result-meta">${statusBadge(resident.matchStatus)}<span>${resident.matchConfidence}</span></div>`;
            card.addEventListener('click', () => loadDetail(resident.id));
            resultsList.appendChild(card);
        });
    }

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
        const xml = resident.xml || {};
        const reviewMessage = resident.matchStatus === 'AMBIGUOUS' ? '<p class="review-warning">Manual review required.</p>'
            : resident.matchStatus === 'EXACT' ? '<p class="review-success">Verified match.</p>' : '';
        const rows = [
            ['ID / Ref', rest.id, xml.ref, 'MATCH'],
            ['Name', [rest.firstName, rest.lastName].filter(Boolean).join(' '), xml.name, 'NORMALIZED MATCH'],
            ['Date of Birth', rest.dateOfBirth, xml.born, 'MATCH'],
            ['Address', rest.address, xml.address, 'NORMALIZED MATCH'],
            ['City / Town', rest.city, xml.town, 'MATCH'],
            ['Phone', rest.phone, null, 'NOT AVAILABLE'],
            ['Program Status', rest.programStatus, null, 'NOT AVAILABLE'],
            ['Last Contact', rest.lastContact, null, 'NOT AVAILABLE'],
            ['Benefit Code', null, xml.benefitCode, 'NOT AVAILABLE'],
            ['Review Due', null, xml.reviewDue, 'NOT AVAILABLE']
        ];
        return `<div class="detail-heading"><h3>${escapeHtml(resident.name || 'Unnamed resident')}</h3>${statusBadge(resident.matchStatus)}<strong>Confidence: ${resident.matchConfidence}%</strong></div>
            ${reviewMessage}<p class="detail-note">${escapeHtml(resident.matchNotes || '')}</p>
            <div class="comparison-wrap"><table class="comparison-table"><thead><tr><th>Field</th><th>REST SOURCE</th><th>XML SOURCE</th><th>Comparison</th></tr></thead><tbody>${rows.map((row) => comparisonRow(row, resident)).join('')}</tbody></table></div>
            <section class="evidence-section"><h4>Why was this record matched?</h4><ul>${(resident.evidence || []).map((item) => `<li class="evidence-${item.comparison.toLowerCase().replace(/\s+/g, '-')}"><strong>${escapeHtml(item.comparison)}</strong><span>${escapeHtml(item.reason)}</span>${item.candidateRefs?.length ? `<small>Candidates: ${escapeHtml(item.candidateRefs.join(', '))}</small>` : ''}</li>`).join('')}</ul></section>`;
    }

    function comparisonRow(row, resident) {
        const [field, restValue, xmlValue, defaultComparison] = row;
        const restPresent = restValue !== undefined && restValue !== null && restValue !== '';
        const xmlPresent = xmlValue !== undefined && xmlValue !== null && xmlValue !== '';
        let comparison = defaultComparison;
        if (!restPresent || !xmlPresent) comparison = restPresent || xmlPresent ? 'NOT AVAILABLE' : 'MISSING';
        if (restPresent && xmlPresent && field === 'Address' && resident.evidence?.some((item) => item.field === 'address and town' && item.comparison === 'DIFFERENT')) comparison = 'DIFFERENT';
        if (restPresent && xmlPresent && field === 'Date of Birth' && resident.evidence?.some((item) => item.field === 'date of birth' && item.comparison === 'DIFFERENT')) comparison = 'DIFFERENT';
        return `<tr class="comparison-${comparison.toLowerCase().replace(/\s+/g, '-')}"><th scope="row">${escapeHtml(field)}</th><td>${escapeHtml(restPresent ? restValue : sourceMissing('REST', resident.matchStatus))}</td><td>${escapeHtml(xmlPresent ? xmlValue : sourceMissing('XML', resident.matchStatus))}</td><td><span class="comparison-label">${escapeHtml(comparison)}</span></td></tr>`;
    }

    function sourceMissing(source, status) {
        if (source === 'REST' && status === 'XML_ONLY') return 'No matching REST resident record found.';
        if (source === 'XML' && status === 'REST_ONLY') return 'No matching XML benefits record found.';
        return 'Unavailable';
    }

    async function refreshStats() {
        try {
            const stats = await api('/api/status');
            [['total', stats.totalResidents], ['exact', stats.exact], ['probable', stats.probable], ['ambiguous', stats.ambiguous], ['rest-only', stats.restOnly], ['xml-only', stats.xmlOnly]].forEach(([id, value]) => $(
                `stat-${id}`).textContent = value ?? '-');
            $('last-ingestion').textContent = stats.lastIngestionStatus
                ? `${stats.lastIngestionStatus} · ${stats.lastIngestionTime || 'in progress'}` : 'No ingestion run';
        } catch (error) {
            $('last-ingestion').textContent = 'Backend unavailable';
        }
    }

    async function runIngestion() {
        setBusy(ingestButton, true, 'Ingesting...');
        ingestionMessage.hidden = false;
        ingestionMessage.className = 'ingestion-message ingestion-message--running';
        ingestionMessage.textContent = 'Ingestion running...';
        try {
            const result = await api('/api/ingest', { method: 'POST' });
            ingestionMessage.className = `ingestion-message ingestion-message--${result.status.toLowerCase()}`;
            ingestionMessage.textContent = `${result.status}: ${result.restRecords} REST, ${result.xmlRecords} XML records. ${result.error || ''}`;
            await refreshStats();
            if (!resultsList.hidden) await performSearch();
        } catch (error) {
            ingestionMessage.className = 'ingestion-message ingestion-message--failed';
            ingestionMessage.textContent = `FAILED: ${error.message}`;
        } finally {
            setBusy(ingestButton, false);
        }
    }

    async function checkStatus(source) {
        try { return await api(`/api/status/${source}`); }
        catch (error) { return { status: 'DOWN' }; }
    }

    function updateStatusUI(source, data) {
        const status = data.status || 'DOWN';
        $(`status-${source}`).dataset.status = status;
        $(`source-card-${source}`).dataset.status = status;
        $(`source-status-${source}`).textContent = status === 'UP' ? 'Connected' : 'Offline';
    }

    async function pollStatuses() {
        const [rest, xml] = await Promise.all([checkStatus('rest'), checkStatus('xml')]);
        updateStatusUI('rest', rest);
        updateStatusUI('xml', xml);
    }

    function initials(name) { return (name || '?').split(/\s+/).slice(0, 2).map((part) => part[0]).join(''); }
    function escapeHtml(value) { const div = document.createElement('div'); div.textContent = value ?? ''; return div.innerHTML; }

    searchButton.dataset.label = 'Search';
    ingestButton.dataset.label = 'Ingest Data';
    searchButton.addEventListener('click', performSearch);
    searchInput.addEventListener('keydown', (event) => { if (event.key === 'Enter') performSearch(); });
    statusFilter.addEventListener('change', performSearch);
    ingestButton.addEventListener('click', runIngestion);
    $('detail-close').addEventListener('click', () => { detailSection.hidden = true; });

    showState('empty');
    refreshStats();
    pollStatuses();
    setInterval(pollStatuses, POLL_INTERVAL);
})();

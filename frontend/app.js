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
        return `<div class="detail-heading"><h3>${escapeHtml(resident.name || 'Unnamed resident')}</h3>${statusBadge(resident.matchStatus)}<strong>${resident.matchConfidence}</strong></div>
            <p class="detail-note">${escapeHtml(resident.matchNotes || '')}</p>
            <div class="detail-grid"><div><h4>REST source</h4><p>ID: ${escapeHtml(rest.id || 'Unavailable')}</p><p>Name: ${escapeHtml([rest.firstName, rest.lastName].filter(Boolean).join(' ') || 'Unavailable')}</p><p>DOB: ${escapeHtml(rest.dateOfBirth || 'Unavailable')}</p><p>${escapeHtml(rest.address || 'Unavailable')}, ${escapeHtml(rest.city || 'Unavailable')}</p><p>Phone: ${escapeHtml(rest.phone || 'Unavailable')}</p></div>
            <div><h4>XML source</h4><p>Ref: ${escapeHtml(xml.ref || 'Unavailable')}</p><p>Name: ${escapeHtml(xml.name || 'Unavailable')}</p><p>DOB: ${escapeHtml(xml.born || 'Unavailable')}</p><p>${escapeHtml(xml.address || 'Unavailable')}, ${escapeHtml(xml.town || 'Unavailable')}</p><p>Benefit: ${escapeHtml(xml.benefitCode || 'Unavailable')}</p></div></div>`;
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

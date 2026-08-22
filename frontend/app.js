/**
 * No Wrong Door — Dashboard JavaScript
 *
 * All requests go through OUR backend (localhost:8080) only.
 * The frontend NEVER calls the mock services directly.
 */

(() => {
    'use strict';

    // ── Configuration ──────────────────────────────────
    const API_BASE = 'http://localhost:8080';
    const STATUS_POLL_INTERVAL = 15_000; // 15 seconds

    // ── DOM References ─────────────────────────────────
    const searchInput     = document.getElementById('search-input');
    const searchButton    = document.getElementById('search-button');
    const btnText         = searchButton.querySelector('.btn-text');
    const btnSpinner      = searchButton.querySelector('.btn-spinner');

    const stateEmpty      = document.getElementById('state-empty');
    const stateLoading    = document.getElementById('state-loading');
    const stateError      = document.getElementById('state-error');
    const stateNoResults  = document.getElementById('state-no-results');
    const resultsList     = document.getElementById('results-list');
    const resultsCount    = document.getElementById('results-count');
    const errorMessage    = document.getElementById('error-message');

    const statusRestBadge = document.getElementById('status-rest');
    const statusXmlBadge  = document.getElementById('status-xml');
    const sourceCardRest  = document.getElementById('source-card-rest');
    const sourceCardXml   = document.getElementById('source-card-xml');
    const sourceStatusRest = document.getElementById('source-status-rest');
    const sourceStatusXml  = document.getElementById('source-status-xml');

    // ── State Management ───────────────────────────────
    function showState(state) {
        // Hide all states
        stateEmpty.hidden     = true;
        stateLoading.hidden   = true;
        stateError.hidden     = true;
        stateNoResults.hidden = true;
        resultsList.hidden    = true;
        resultsCount.hidden   = true;

        switch (state) {
            case 'empty':
                stateEmpty.hidden = false;
                break;
            case 'loading':
                stateLoading.hidden = false;
                break;
            case 'error':
                stateError.hidden = false;
                break;
            case 'no-results':
                stateNoResults.hidden = false;
                break;
            case 'results':
                resultsList.hidden = false;
                resultsCount.hidden = false;
                break;
        }
    }

    // ── Search ─────────────────────────────────────────
    async function performSearch() {
        const query = searchInput.value.trim();

        if (!query) {
            showState('empty');
            return;
        }

        // Show loading state
        showState('loading');
        searchButton.disabled = true;
        btnText.hidden = true;
        btnSpinner.hidden = false;

        try {
            const response = await fetch(
                `${API_BASE}/api/residents/search?query=${encodeURIComponent(query)}`
            );

            if (!response.ok) {
                const errData = await response.json().catch(() => ({}));
                throw new Error(errData.error || `Backend returned ${response.status}`);
            }

            const data = await response.json();

            if (!data.results || data.results.length === 0) {
                showState('no-results');
                return;
            }

            // Render results
            renderResults(data.results);

            // Show matched count and optionally total REST records fetched
            const matchText = `${data.totalResults} result${data.totalResults !== 1 ? 's' : ''}`;
            const restTotalText = data.restTotal != null
                ? ` (from ${data.restTotal} unique REST records)`
                : '';
            resultsCount.textContent = matchText + restTotalText;

            showState('results');

        } catch (err) {
            console.error('Search failed:', err);
            errorMessage.textContent = err.message || 'Unable to reach the backend service';
            showState('error');
        } finally {
            searchButton.disabled = false;
            btnText.hidden = false;
            btnSpinner.hidden = true;
        }
    }

    function renderResults(results) {
        resultsList.innerHTML = '';

        results.forEach((resident) => {
            const initials = (resident.firstName?.[0] || '') + (resident.lastName?.[0] || '');

            const card = document.createElement('div');
            card.className = 'resident-card';
            card.innerHTML = `
                <div class="resident-avatar">${escapeHtml(initials)}</div>
                <div class="resident-info">
                    <div class="resident-name">${escapeHtml(resident.firstName)} ${escapeHtml(resident.lastName)}</div>
                    <div class="resident-details">
                        <span>ID: ${escapeHtml(resident.id)}</span>
                        ${resident.dateOfBirth ? `<span>DOB: ${escapeHtml(resident.dateOfBirth)}</span>` : ''}
                        ${resident.phone ? `<span>${escapeHtml(resident.phone)}</span>` : ''}
                    </div>
                    ${resident.address ? `<div class="resident-address">${escapeHtml(resident.address)}</div>` : ''}
                </div>
                <span class="resident-source">${escapeHtml(resident.source || 'unknown')}</span>
            `;
            resultsList.appendChild(card);
        });
    }

    // ── Status Polling ─────────────────────────────────
    async function checkStatus(source) {
        try {
            const response = await fetch(`${API_BASE}/api/status/${source}`);
            if (!response.ok) {
                return { status: 'DOWN', message: `HTTP ${response.status}` };
            }
            return await response.json();
        } catch (err) {
            return { status: 'DOWN', message: err.message };
        }
    }

    function updateStatusUI(source, data) {
        const status = data.status || 'DOWN';

        if (source === 'rest') {
            statusRestBadge.setAttribute('data-status', status);
            sourceCardRest.setAttribute('data-status', status);
            sourceStatusRest.textContent = status === 'UP' ? 'Connected' : 'Offline';
        } else {
            statusXmlBadge.setAttribute('data-status', status);
            sourceCardXml.setAttribute('data-status', status);
            sourceStatusXml.textContent = status === 'UP' ? 'Connected' : 'Offline';
        }
    }

    async function pollStatuses() {
        const [restData, xmlData] = await Promise.all([
            checkStatus('rest'),
            checkStatus('xml')
        ]);

        updateStatusUI('rest', restData);
        updateStatusUI('xml', xmlData);
    }

    // ── Utilities ──────────────────────────────────────
    function escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    // ── Event Listeners ────────────────────────────────
    searchButton.addEventListener('click', performSearch);

    searchInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            performSearch();
        }
    });

    // ── Initialization ─────────────────────────────────
    showState('empty');
    pollStatuses();
    setInterval(pollStatuses, STATUS_POLL_INTERVAL);
})();

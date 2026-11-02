// BookList — small progressive enhancements. Every page works without JavaScript.
(function () {
    'use strict';
    document.documentElement.classList.add('js');

    document.addEventListener('DOMContentLoaded', function () {
        setupCoverFallbacks(document);
        setupAlerts();
        setupConfirmations();
        setupTabs();
        setupSearch();
        setupAddForm();
    });

    // Broken or missing cover images fall back to the default cover.
    function setupCoverFallbacks(root) {
        root.querySelectorAll('img[data-fallback]').forEach(function (img) {
            var useFallback = function () {
                if (img.getAttribute('src') !== img.dataset.fallback) {
                    img.src = img.dataset.fallback;
                }
            };
            img.addEventListener('error', useFallback);
            if (img.complete && img.naturalWidth === 0 && img.getAttribute('src')) {
                useFallback();
            }
        });
    }

    // Close buttons on alerts, success alerts hide themselves after a few seconds.
    function setupAlerts() {
        document.querySelectorAll('.alert').forEach(function (alert) {
            var hide = function () {
                alert.classList.add('is-hiding');
                setTimeout(function () { alert.remove(); }, 300);
            };
            var close = alert.querySelector('.alert-close');
            if (close) close.addEventListener('click', hide);
            if (alert.dataset.dismissAfter) setTimeout(hide, Number(alert.dataset.dismissAfter));
        });
    }

    // Forms with data-confirm ask before submitting (used for deleting books).
    function setupConfirmations() {
        document.querySelectorAll('form[data-confirm]').forEach(function (form) {
            form.addEventListener('submit', function (event) {
                if (!window.confirm(form.dataset.confirm)) event.preventDefault();
            });
        });
    }

    // Tabs switch between the collection and the wishlist; the URL hash remembers the choice.
    function setupTabs() {
        var tabs = document.querySelectorAll('[data-tab]');
        if (!tabs.length) return;
        var panels = document.querySelectorAll('[data-panel]');

        var show = function (name) {
            tabs.forEach(function (tab) {
                var active = tab.dataset.tab === name;
                tab.classList.toggle('is-active', active);
                tab.setAttribute('aria-current', active ? 'page' : 'false');
            });
            panels.forEach(function (panel) { panel.hidden = panel.dataset.panel !== name; });
        };

        var fromHash = function () {
            var name = location.hash.replace('#', '');
            return document.querySelector('[data-panel="' + name + '"]') ? name : tabs[0].dataset.tab;
        };

        tabs.forEach(function (tab) {
            tab.addEventListener('click', function (event) {
                event.preventDefault();
                history.replaceState(null, '', '#' + tab.dataset.tab);
                show(tab.dataset.tab);
                filter();
            });
        });
        window.addEventListener('hashchange', function () { show(fromHash()); });
        show(fromHash());
    }

    // Search filters the books in the visible tab by title and author.
    var searchInput;
    function setupSearch() {
        searchInput = document.getElementById('book-search');
        if (!searchInput) return;
        searchInput.addEventListener('input', filter);
        document.addEventListener('keydown', function (event) {
            if (event.key === '/' && document.activeElement.tagName !== 'INPUT') {
                event.preventDefault();
                searchInput.focus();
            }
        });
    }

    function filter() {
        if (!searchInput) return;
        var query = searchInput.value.trim().toLowerCase();
        var visible = 0;
        document.querySelectorAll('[data-panel]').forEach(function (panel) {
            var cards = panel.querySelectorAll('.book-card');
            cards.forEach(function (card) {
                var match = !query || card.dataset.search.indexOf(query) !== -1;
                card.hidden = !match;
                if (match && !panel.hidden) visible++;
            });
        });
        var noResults = document.getElementById('no-results');
        if (noResults) noResults.hidden = !query || visible > 0;
    }

    // Add-book form: live preview and a loading state while the cover is looked up.
    function setupAddForm() {
        var form = document.getElementById('add-book-form');
        if (!form) return;

        var bindText = function (inputSelector, targetId) {
            var input = form.querySelector(inputSelector);
            var target = document.getElementById(targetId);
            var update = function () { target.textContent = input.value.trim() || target.dataset.empty; };
            input.addEventListener('input', update);
            update();
        };
        bindText('[data-preview="title"]', 'preview-title');
        bindText('[data-preview="author"]', 'preview-author');

        var coverInput = form.querySelector('[data-preview="cover"]');
        var coverImg = document.getElementById('preview-cover');
        var updateCover = function () {
            var url = coverInput.value.trim();
            coverImg.src = /^https?:\/\//i.test(url) ? url : coverImg.dataset.default;
        };
        coverInput.addEventListener('input', updateCover);
        updateCover();

        form.addEventListener('submit', function () {
            var button = form.querySelector('button[type="submit"]');
            if (!coverInput.value.trim() && button.dataset.loadingText) {
                button.textContent = button.dataset.loadingText;
            }
            button.disabled = true;
        });
    }
})();

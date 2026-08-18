(() => {
    document.documentElement.classList.add('mabillon-js-enabled');

    const csrfToken = () => document.cookie.split('; ').find((value) => value.startsWith('XSRF-TOKEN='))
        ?.substring('XSRF-TOKEN='.length);

    document.addEventListener('htmx:configRequest', (event) => {
        const token = csrfToken();
        if (token) {
            event.detail.headers['X-XSRF-TOKEN'] = decodeURIComponent(token);
        }
    });

    const panel = document.querySelector('[data-mabillon-navigation-panel]');
    const toggle = document.querySelector('[data-mabillon-navigation-toggle]');
    const closeButton = document.querySelector('[data-mabillon-navigation-close]');
    const backdrop = document.querySelector('[data-mabillon-navigation-backdrop]');
    const mobileNavigation = window.matchMedia('(max-width: 991.98px)');
    let returnFocus = null;

    if (!panel || !toggle || !closeButton || !backdrop) {
        return;
    }

    const focusable = () => Array.from(panel.querySelectorAll(
        'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
    ));

    const setOpen = (open, restoreFocus = true) => {
        if (!mobileNavigation.matches) {
            open = false;
        }

        panel.dataset.mabillonNavigationOpen = String(open);
        toggle.setAttribute('aria-expanded', String(open));
        backdrop.hidden = !open;
        document.body.classList.toggle('mabillon-navigation-open', open);

        if (open) {
            returnFocus = document.activeElement;
            closeButton.focus();
        } else if (restoreFocus && returnFocus instanceof HTMLElement) {
            returnFocus.focus();
            returnFocus = null;
        }
    };

    toggle.addEventListener('click', () => setOpen(panel.dataset.mabillonNavigationOpen !== 'true'));
    closeButton.addEventListener('click', () => setOpen(false));
    backdrop.addEventListener('click', () => setOpen(false));
    panel.addEventListener('click', (event) => {
        if (event.target.closest('a[href]')) {
            setOpen(false, false);
        }
    });

    document.addEventListener('keydown', (event) => {
        if (panel.dataset.mabillonNavigationOpen !== 'true') {
            return;
        }
        if (event.key === 'Escape') {
            event.preventDefault();
            setOpen(false);
            return;
        }
        if (event.key !== 'Tab') {
            return;
        }

        const items = focusable();
        if (items.length === 0) {
            event.preventDefault();
            return;
        }
        const first = items[0];
        const last = items[items.length - 1];
        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus();
        }
    });

    mobileNavigation.addEventListener('change', () => setOpen(false, false));
})();

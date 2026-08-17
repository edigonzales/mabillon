(() => {
    const csrfToken = () => document.cookie.split('; ').find((value) => value.startsWith('XSRF-TOKEN='))
        ?.substring('XSRF-TOKEN='.length);

    document.addEventListener('htmx:configRequest', (event) => {
        const token = csrfToken();
        if (token) {
            event.detail.headers['X-XSRF-TOKEN'] = decodeURIComponent(token);
        }
    });
})();

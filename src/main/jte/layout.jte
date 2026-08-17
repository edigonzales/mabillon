@param String title
@param String active
@param gg.jte.Content content
<!doctype html>
<html lang="de">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title} · Mabillon</title>
    <link rel="stylesheet" href="/mabillon.css">
    <script src="/htmx-2.0.10.min.js" defer></script>
    <script src="/mabillon.js" defer></script>
</head>
<body>
<header class="app-header">
    <div class="app-header__inner">
        <a class="brand" href="/">Mabillon <span>Geschäftsverwaltung</span></a>
        <span class="environment-label">Lesemodus</span>
    </div>
</header>
<div class="app-shell">
    <aside class="app-sidebar" aria-label="Hauptnavigation">
        <p class="sidebar-caption">Arbeitsbereich</p>
        <nav class="sidebar-nav">
            @if (active.equals("dashboard"))
                <a class="is-active" href="/">Übersicht</a>
            @else
                <a href="/">Übersicht</a>
            @endif
            @if (active.equals("dossiers"))
                <a class="is-active" href="/dossiers">Dossiers</a>
            @else
                <a href="/dossiers">Dossiers</a>
            @endif
            @if (active.equals("geschaefte"))
                <a class="is-active" href="/geschaefte">Geschäfte</a>
            @else
                <a href="/geschaefte">Geschäfte</a>
            @endif
            @if (active.equals("search"))
                <a class="is-active" href="/suche">Suche</a>
            @else
                <a href="/suche">Suche</a>
            @endif
            @if (active.equals("control"))
                <a class="is-active" href="/geschaeftskontrolle">Geschäftskontrolle</a>
            @else
                <a href="/geschaeftskontrolle">Geschäftskontrolle</a>
            @endif
            @if (active.equals("archive"))
                <a class="is-active" href="/archivierung">Archivierung</a>
            @else
                <a href="/archivierung">Archivierung</a>
            @endif
            <a href="/admin">Administration</a>
        </nav>
        <div class="sidebar-note">
            <span class="status-dot"></span>
            <span>PostgreSQL verbunden</span>
        </div>
    </aside>
    <main class="app-main">
        ${content}
    </main>
</div>
</body>
</html>

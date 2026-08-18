@param String title
@param String active
@param gg.jte.Content content
<!doctype html>
<html lang="de-CH">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title} · Mabillon</title>
    <link rel="stylesheet" href="/mabillon.css">
    <script src="/htmx-2.0.10.min.js" defer></script>
    <script src="/mabillon.js" defer></script>
</head>
<body>
<a class="mabillon-skip-link" href="#main-content">Zum Inhalt springen</a>
<header class="mabillon-topbar">
    <div class="mabillon-topbar-inner">
        <button class="mabillon-menu-toggle"
                type="button"
                aria-label="Navigation einblenden"
                aria-controls="mabillon-navigation-panel"
                aria-expanded="false"
                data-mabillon-navigation-toggle>
            @template.components.icon(name = "list")
        </button>
        <a class="mabillon-brand" href="/">
            @template.components.icon(name = "grid")
            <span>Mabillon</span>
        </a>
        <form class="mabillon-global-search" method="get" action="/suche" role="search">
            <label class="mabillon-visually-hidden" for="global-search">Systemweit suchen</label>
            <div class="mabillon-search-control">
                <span class="mabillon-search-icon" aria-hidden="true">@template.components.icon(name = "search")</span>
                <input id="global-search" name="q" type="search" placeholder="Systemweit suchen …" autocomplete="off">
            </div>
            <button class="mabillon-button" type="submit">Suchen</button>
        </form>
        <span class="mabillon-topbar-context">Geschäftsverwaltung</span>
    </div>
</header>
<div class="mabillon-shell-layout">
    <aside id="mabillon-navigation-panel"
           class="mabillon-sidebar"
           aria-label="Hauptnavigation"
           data-mabillon-navigation-panel
           data-mabillon-navigation-open="false">
        <div class="mabillon-sidebar-header">
            <strong>Navigation</strong>
            <button class="mabillon-sidebar-close" type="button" aria-label="Navigation schliessen" data-mabillon-navigation-close>
                @template.components.icon(name = "close")
            </button>
        </div>
        <section class="mabillon-navigation-group">
            <p class="mabillon-navigation-title">Arbeitsbereich</p>
            <nav class="mabillon-navigation">
                <a class="${active.equals("dashboard") ? "mabillon-navigation-link--active" : ""}" href="/" aria-current="${active.equals("dashboard") ? "page" : null}">
                    @template.components.icon(name = "house")<span>Übersicht</span>
                </a>
                <a class="${active.equals("dossiers") ? "mabillon-navigation-link--active" : ""}" href="/dossiers" aria-current="${active.equals("dossiers") ? "page" : null}">
                    @template.components.icon(name = "folder")<span>Dossiers</span>
                </a>
                <a class="${active.equals("geschaefte") ? "mabillon-navigation-link--active" : ""}" href="/geschaefte" aria-current="${active.equals("geschaefte") ? "page" : null}">
                    @template.components.icon(name = "folder")<span>Geschäfte</span>
                </a>
                <a class="${active.equals("aufgaben") ? "mabillon-navigation-link--active" : ""}" href="/aufgaben" aria-current="${active.equals("aufgaben") ? "page" : null}">
                    @template.components.icon(name = "list")<span>Aufgaben</span>
                </a>
                <a class="${active.equals("beteiligte") ? "mabillon-navigation-link--active" : ""}" href="/beteiligte" aria-current="${active.equals("beteiligte") ? "page" : null}">
                    @template.components.icon(name = "folder")<span>Beteiligte</span>
                </a>
                <a class="${active.equals("search") ? "mabillon-navigation-link--active" : ""}" href="/suche" aria-current="${active.equals("search") ? "page" : null}">
                    @template.components.icon(name = "search")<span>Suche</span>
                </a>
                <a class="${active.equals("control") ? "mabillon-navigation-link--active" : ""}" href="/geschaeftskontrolle" aria-current="${active.equals("control") ? "page" : null}">
                    @template.components.icon(name = "list")<span>Geschäftskontrolle</span>
                </a>
                <a class="${active.equals("archive") ? "mabillon-navigation-link--active" : ""}" href="/archivierung" aria-current="${active.equals("archive") ? "page" : null}">
                    @template.components.icon(name = "folder")<span>Archivierung</span>
                </a>
            </nav>
        </section>
        <section class="mabillon-navigation-group">
            <p class="mabillon-navigation-title">System</p>
            <nav class="mabillon-navigation">
                <a class="${active.equals("admin") || active.equals("catalogs") || active.equals("masterdata") || active.equals("registraturplan") ? "mabillon-navigation-link--active" : ""}"
                   href="/admin"
                   aria-current="${active.equals("admin") || active.equals("catalogs") || active.equals("masterdata") || active.equals("registraturplan") ? "page" : null}">
                    @template.components.icon(name = "grid")<span>Administration</span>
                </a>
            </nav>
        </section>
        <div class="mabillon-sidebar-note">
            <span class="mabillon-status-dot" aria-hidden="true"></span>
            <span>PostgreSQL verbunden</span>
        </div>
    </aside>
    <button class="mabillon-sidebar-backdrop"
            type="button"
            aria-label="Navigation schliessen"
            data-mabillon-navigation-backdrop
            hidden></button>
    <main id="main-content" class="mabillon-main-content" role="main">
        <div class="mabillon-main-grid">
            ${content}
        </div>
    </main>
</div>
</body>
</html>

@param String title
@param String active
@param org.springframework.security.web.csrf.CsrfToken csrfToken
@param java.util.List<guru.interlis.mabillon.interlis.ImportScope> scopes
@param guru.interlis.mabillon.interlis.ExchangeResult result
@template.layout(
    title = title,
    active = active,
    content = @`
        <nav class="mabillon-breadcrumbs" aria-label="Brotkrümel"><a href="/admin">Administration</a> / INTERLIS</nav>
        <div class="mabillon-page-header">
            <div>
                <p class="mabillon-eyebrow">Datenaustausch</p>
                <h1>${title}</h1>
                <p class="mabillon-page-subtitle">XTF wird vor dem Import und nach dem Export mit ilivalidator geprüft. Die Importreihenfolge ist Kataloge, Stammdaten, Geschäftsdaten.</p>
            </div>
        </div>
        @if (result != null)
            <section class="mabillon-notice ${result.successful() ? "mabillon-notice--success" : "mabillon-notice--error"}" aria-live="polite">
                @for (var message : result.messages())
                    <p>${message}</p>
                @endfor
                @if (!result.validation().valid())
                    <pre>${result.validation().diagnostics()}</pre>
                @elseif (result.importResult() != null && !result.importResult().successful())
                    <pre>${result.importResult().diagnostics()}</pre>
                @endif
            </section>
        @endif
        <section class="mabillon-tile">
            <div class="mabillon-section-heading">
                <h2>Topics</h2>
                <span class="mabillon-count-badge">${scopes.size()}</span>
            </div>
            <div class="mabillon-record-list">
                @for (var scope : scopes)
                    <div class="mabillon-record-row">
                        <span>
                            <strong>${scope.label()}</strong>
                            <span>Basket ${scope.qualifiedTopic()}</span>
                        </span>
                        <span class="mabillon-inline-actions">
                            <form method="post" action="/admin/interlis/import/${scope.name().toLowerCase()}" enctype="multipart/form-data">
                                <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                                <label class="mabillon-file-input">XTF wählen <input type="file" name="file" accept=".xtf,application/xml,text/xml" required></label>
                                <button class="mabillon-button" type="submit">Importieren</button>
                            </form>
                            <a class="mabillon-button mabillon-button--secondary" href="/admin/interlis/export/${scope.name().toLowerCase()}">Exportieren</a>
                        </span>
                    </div>
                @endfor
            </div>
        </section>
    `
)

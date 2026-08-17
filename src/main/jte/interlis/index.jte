@param String title
@param String active
@param org.springframework.security.web.csrf.CsrfToken csrfToken
@param java.util.List<guru.interlis.mabillon.interlis.ImportScope> scopes
@param guru.interlis.mabillon.interlis.ExchangeResult result
@template.layout(
    title = title,
    active = active,
    content = @`
        <p class="breadcrumb"><a href="/admin">Administration</a> / INTERLIS</p>
        <div class="page-header">
            <div>
                <p class="eyebrow">Datenaustausch</p>
                <h1>${title}</h1>
                <p class="lead">XTF wird vor dem Import und nach dem Export mit ilivalidator geprüft. Die Importreihenfolge ist Kataloge, Stammdaten, Geschäftsdaten.</p>
            </div>
        </div>
        @if (result != null)
            <section class="notice ${result.successful() ? "notice--success" : "notice--error"}" aria-live="polite">
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
        <section class="workspace-card">
            <div class="section-heading">
                <h2>Topics</h2>
                <span class="count-badge">${scopes.size()}</span>
            </div>
            <div class="record-list">
                @for (var scope : scopes)
                    <div class="admin-record">
                        <span>
                            <strong>${scope.label()}</strong>
                            <span>Basket ${scope.qualifiedTopic()}</span>
                        </span>
                        <span class="inline-actions">
                            <form method="post" action="/admin/interlis/import/${scope.name().toLowerCase()}" enctype="multipart/form-data">
                                <input type="hidden" name="${csrfToken.getParameterName()}" value="${csrfToken.getToken()}">
                                <label class="file-input">XTF wählen <input type="file" name="file" accept=".xtf,application/xml,text/xml" required></label>
                                <button class="button" type="submit">Importieren</button>
                            </form>
                            <a class="button button--quiet" href="/admin/interlis/export/${scope.name().toLowerCase()}">Exportieren</a>
                        </span>
                    </div>
                @endfor
            </div>
        </section>
    `
)

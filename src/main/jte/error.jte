@param String title
@param int status
@param String message
@param java.util.List<guru.interlis.mabillon.domain.FieldError> errors
@template.layout(title = title, active = "dashboard", content = @`
    <div class="page-header">
        <div><p class="eyebrow">HTTP ${status}</p><h1>${title}</h1><p class="lead">${message}</p></div>
    </div>
    @if (!errors.isEmpty())
        <section class="workspace-card">
            <h2>Eingaben</h2>
            <ul>
                @for (var error : errors)
                    <li><strong>${error.field()}</strong>: ${error.message()} <span class="muted">(${error.code()})</span></li>
                @endfor
            </ul>
        </section>
    @endif
    <section class="workspace-card"><a class="button" href="/">Zur Übersicht</a></section>
`)

@param String title
@param int status
@param String message
@param java.util.List<guru.interlis.mabillon.domain.FieldError> errors
@template.layout(title = title, active = "dashboard", content = @`
    <div class="mabillon-page-header">
        <div><p class="mabillon-eyebrow">HTTP ${status}</p><h1>${title}</h1><p class="mabillon-page-subtitle">${message}</p></div>
    </div>
    @if (!errors.isEmpty())
        <section class="mabillon-tile">
            <h2>Eingaben</h2>
            <ul>
                @for (var error : errors)
                    <li><strong>${error.field()}</strong>: ${error.message()} <span class="mabillon-muted">(${error.code()})</span></li>
                @endfor
            </ul>
        </section>
    @endif
    <section class="mabillon-tile"><a class="mabillon-button" href="/">Zur Übersicht</a></section>
`)

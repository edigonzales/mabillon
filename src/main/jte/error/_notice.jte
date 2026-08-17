@param int status
@param String message
@param java.util.List<guru.interlis.mabillon.domain.FieldError> errors
<section class="workspace-card" role="alert">
    <p class="eyebrow">HTTP ${status}</p>
    <strong>${message}</strong>
    @if (!errors.isEmpty())
        <ul>
            @for (var error : errors)
                <li><strong>${error.field()}</strong>: ${error.message()} <span class="muted">(${error.code()})</span></li>
            @endfor
        </ul>
    @endif
</section>

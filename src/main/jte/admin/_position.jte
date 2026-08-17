@param guru.interlis.mabillon.registraturplan.RegistraturplanPositionView position
<li>
    <div class="tree-item">
        <span><strong>${position.code()}</strong> ${position.titel()} <span class="muted">· ${position.status()}</span></span>
        @if (position.leaf() && position.active())
            <span class="status-tag">auswählbar</span>
        @endif
    </div>
    @if (!position.children().isEmpty())
        <ul class="tree-list">
            @for (var child : position.children())
                @template.admin._position(position = child)
            @endfor
        </ul>
    @endif
</li>

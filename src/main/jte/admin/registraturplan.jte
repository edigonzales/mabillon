@param String title
@param String active
@param java.util.List<guru.interlis.mabillon.registraturplan.RegistraturplanView> plans
@param guru.interlis.mabillon.registraturplan.RegistraturplanTreeView tree
@template.layout(
    title = title,
    active = active,
    content = @`
        <p class="breadcrumb"><a href="/admin">Administration</a> / Registraturplan</p>
        <div class="page-header">
            <div>
                <p class="eyebrow">Ordnungssystem</p>
                <h1>${title}</h1>
                <p class="lead">Nur aktive Blattpositionen können für neue Dossiers verwendet werden.</p>
            </div>
        </div>
        <section class="workspace-card">
            <div class="section-heading"><h2>Pläne</h2><span class="count-badge">${plans.size()}</span></div>
            <ul class="record-list">
                @for (var plan : plans)
                    <li><a href="/admin/registraturplan?plan=${plan.code()}"><strong>${plan.name()}</strong><span><code>${plan.code()}</code> · ${plan.status()}</span></a></li>
                @endfor
            </ul>
        </section>
        @if (tree != null)
            <section class="workspace-card admin-section">
                <div class="section-heading"><h2>${tree.plan().name()}</h2><span class="status-tag">${tree.plan().status()}</span></div>
                <p class="muted"><code>${tree.plan().code()}</code> · gültig ab ${tree.plan().gueltigVon().toString()}</p>
                <ul class="tree-list">
                    @for (var position : tree.roots())
                        @template.admin._position(position = position)
                    @endfor
                </ul>
            </section>
        @endif
    `
)

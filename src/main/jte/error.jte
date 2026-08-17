@param String title
@param int status
@param String message
@template.layout(title = title, active = "dashboard", content = @`
    <div class="page-header">
        <div><p class="eyebrow">HTTP ${status}</p><h1>${title}</h1><p class="lead">${message}</p></div>
    </div>
    <section class="workspace-card"><a class="button" href="/">Zur Übersicht</a></section>
`)

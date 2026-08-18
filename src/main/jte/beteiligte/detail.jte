@param String title
@param String active
@param guru.interlis.mabillon.beteiligung.BeteiligterView beteiligter
@template.layout(
    title = title,
    active = active,
    content = @`
        <nav class="mabillon-breadcrumbs" aria-label="Brotkrümel"><a href="/beteiligte">Beteiligte</a> / ${beteiligter.name()}</nav>
        <div class="mabillon-page-header">
            <div><p class="mabillon-eyebrow">${beteiligter.typ()}</p><h1>${beteiligter.name()}</h1></div>
            <a class="mabillon-button" href="/beteiligte/${beteiligter.tid().toString()}/bearbeiten">Bearbeiten</a>
        </div>
        <section class="mabillon-tile">
            <dl class="mabillon-detail-grid">
                <div><dt>Vorname</dt><dd>${beteiligter.vorname() == null ? "–" : beteiligter.vorname()}</dd></div>
                <div><dt>Organisation</dt><dd>${beteiligter.organisation() == null ? "–" : beteiligter.organisation()}</dd></div>
                <div><dt>E-Mail</dt><dd>${beteiligter.email() == null ? "–" : beteiligter.email()}</dd></div>
                <div><dt>Telefon</dt><dd>${beteiligter.telefon() == null ? "–" : beteiligter.telefon()}</dd></div>
                <div><dt>Adresse</dt><dd>${beteiligter.adresse() == null ? "–" : beteiligter.adresse()}</dd></div>
                <div><dt>Externe Referenz</dt><dd>${beteiligter.externeReferenz() == null ? "–" : beteiligter.externeReferenz()}</dd></div>
            </dl>
        </section>
    `
)

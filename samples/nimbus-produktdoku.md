# Nimbus Task Service – Technische Produktdokumentation

> Fiktives Beispieldokument für das Tutorial „RAG mit Spring Boot und Spring AI entwickeln“.
> Produkt, Firma und alle Angaben sind frei erfunden.

## Überblick

Nimbus ist ein interner Aufgabenservice der fiktiven Firma Wolkenfabrik GmbH. Teams
legen darüber Aufgaben an, weisen sie Personen zu und verfolgen ihren Status. Der
Service besteht aus einer Spring-Boot-Anwendung, einer PostgreSQL-Datenbank und
einem Redis-Cache für häufig gelesene Aufgabenlisten.

## Authentifizierung der REST API

Alle Endpunkte der Nimbus REST API sind über OAuth 2.0 abgesichert. Clients holen
sich beim zentralen Identity Provider ein JWT Access Token über den Client
Credentials Flow und senden es im Header `Authorization: Bearer <token>` mit.

Access Tokens sind 15 Minuten gültig. Nimbus validiert bei jeder Anfrage die
Signatur, den Aussteller und die Audience `nimbus-api`. Für schreibende Zugriffe
ist zusätzlich der Scope `tasks:write` erforderlich, für lesende Zugriffe genügt
`tasks:read`.

API Keys oder Basic Auth werden nicht unterstützt.

## Rate Limits

Pro Client sind 300 Anfragen pro Minute erlaubt. Wird das Limit überschritten,
antwortet Nimbus mit HTTP 429 und dem Header `Retry-After`, der die Wartezeit in
Sekunden angibt.

## Deployment

Nimbus läuft in drei Umgebungen: `dev`, `staging` und `prod`. Deployments nach
`prod` finden dienstags und donnerstags zwischen 10 und 12 Uhr statt. Jedes
Deployment wird als Blue-Green-Deployment ausgerollt, sodass bei Problemen
innerhalb von zwei Minuten auf die vorherige Version zurückgeschaltet werden kann.

## Datenhaltung und Backups

Die PostgreSQL-Datenbank wird jede Nacht um 2 Uhr vollständig gesichert.
Zusätzlich werden die Write-Ahead-Logs kontinuierlich archiviert, wodurch eine
Wiederherstellung auf jeden beliebigen Zeitpunkt der letzten 14 Tage möglich ist.

Erledigte Aufgaben werden nach 24 Monaten automatisch archiviert und sind dann
nur noch über den Archiv-Endpunkt `GET /api/archive/tasks` abrufbar.

## Monitoring und Bereitschaft

Metriken werden über Micrometer an Prometheus übertragen. Ein Alarm wird
ausgelöst, wenn die 95. Perzentile der Antwortzeit länger als fünf Minuten über
800 Millisekunden liegt oder die Fehlerquote 2 Prozent übersteigt.

Das Team „Plattform Nord“ ist für Nimbus zuständig und stellt außerhalb der
Geschäftszeiten eine Rufbereitschaft. Störungen werden im Kanal `#nimbus-incidents`
gemeldet.

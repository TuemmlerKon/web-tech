#Aufgetretene Probleme

##Schlüsselspeicherung für Dateien

###Problem
Werden die Schlüssel, welcher zur Entschlüsselung der Dateien auf dem selben System gespeichert, könnte im Falle einer komprimitierung des Servers ein Angreifer sowohl auf die Dateien als auch die Schlüssel zugreifen.

###Lösung
Die Schlüssel werden in einer Datenbank gespeichert. Über Ebean ORM ist es möglich die Objekte von verschiedenen Servern abzurufen ohne die Funktionalität des Systems zu beeinträchtigen. Dadurch werden die Schlüssel bei einem Produktivsystem auf einen externen Server ausgelagert und über eine verschlüsselte Verbindung abgerufen. Wird jetzt ein System kompromitiert, hat der Angreifer nur zugriff auf eine der beiden benötigten Komponenten (Schlüssel und Datei).

##Dateisystemzugriff (Ordner und Pfadlänge)

###Problem
Der Zugriff auf das Dateisystem ist von JAVA aus im Prinzip recht einfach. Allerdings kann es unter Umständen auf den verschiedenen Platformen (Linux, MacOS, Windows) zu Problemen mit Pfadlänge bzw. Umlauten in den Ordnernamen kommen.

###Lösung
Ordnerstruktur auf dem Dateisystem wird nur auf einer Ebene gehalten. Jeder Ordner heißt auf dem Dateisystem nur wie seine ID ist. Der tatsächliche Ordnername steht nur in der Datenbank. Ebenso ist die vom Benutzer eingerichtete Baumstruktur nur virtuell (also nur in der Datenbank vorhanden)

##Rechtesystem

###Problem
Mit der Standardimplementierung für Benutzer existiert keine explizite klassifizierung von Benutzern. Das heißt Grundlegend sind am System alle Teilnehmer gleichberechtigt. Allerdings sollten solche Funktionen die Newsverwaltung einem Administrator oder höher berechtigtem User vorbehalten sein.

###Lösung
Implementierung eines Rollensystems. Jeder Benutzer hat standardmäßig eine Rolle namens ROLE_USER. Wird er nun befördert (aktuell nur über die Datenbank machbar), kann man ihm weitere Rollen zuteilen und diese in der entsprechenden Aktion abfragen.

###Beispielcode
	//Abrufen der Benutzerdaten aus dem aktuellen Kontext
	User user = Account.getCurrentUser();
	/*Wenn user == null ist, dann ist er nicht angemeldet. Ist user.isAdmin() == false dann ist er kein Admin
	  siehe hier entsprechende Methode im User-Objekt. isAdmin() prüft nur ob der Benutzer eine Rolle namens ROLE_ADMIN hat*/
	if (user == null || !user.isAdmin()) {
		//der Konsole melden, dass eine Aktion abgebrochen wurde, weil die entsprechenden Rechte fehlen	
		logger.debug("Filesystem: index(): User unauthenticated or no admin rights");
		//Der Benutzer wird dann automatisch auf die Login-Seite weitergeleitet
		return redirect(controllers.routes.Account.login());
	}

Nächste Seite: [Fazit](07_FAZIT.md)<br/>
Vorherige Seite: [Technologien](05_TECHNOLOGIEN.md)

#Architektur

##PlayFramework 2.3.x
Das Play ist ein für Web-Applikationen entwickeltes Framework, nach dem MVC-Schema (Model-View-Controller).
Es ist in Java sowie Scala geschrieben und ermöglicht ein einfaches, agiles, schnelles und sicheres Entwickeln von diversen Anwendungen im Web. Näheres gibt es [auf der Hompage](https://www.playframework.com/) des PlayFrameworks.

##Datenbank
Als Datenbankbackend haben wir uns für eine MySQL-Datenbank entschieden, welche wir über JDBC bzw. Ebean an das Playframework anbinden. Die Wahl zu MySQL fiel unter anderem deswegen, da wir durch die Verwaltung von Dateien onehin hohe Anforderungen an den Server bzw. das Dateisystem haben weswegen die gleichzeitige Nutzung einer sqlite-Datenbank von Nachteil wäre. Dadurch können wir im Falle von Ressourcenmangel die Datenbank leicht auf einen externen Server auslagern.
Ebenfalls haben wir uns wegen der Schlüsselverwaltung für die Dateiverschlüsselung für MySQL entschieden, da wir, wie schon im Punkt [Probleme](06_PROBLEME.md) beschrieben, die Speicherung der Schlüssel auf einem externen Server leicht realisieren können.

##Klassendiagramm

![Klassendiagramm](/Klassendiagramm/CloudPlex_ClassDiagram_1.jpg)

Nächste Seite: [Endstand](04_ENDSTAND.md)<br/>
Vorherige Seite: [Anforderungen](02_ANFORDERUNGEN.md)

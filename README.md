# Solver Eingabemöglichkeiten

## Generieren von Problemen mit
```generate <jobCount> <taskCount> <durationOutlier> <machineCount> <optionalCount> <altCount> <altGroupCount> <deadline> <durationConCount> <maxDurationConFromOneTask> <name>```

## Lösen von Problemen
`o`: nach optimalem Schedule suchen  
`f`: nach feasible Schedule suchen
### Von Generator erstellte Probleme
```
solve [o oder f] <name>.txt
```
### Über Problemfamilie/ XML-Datei
```
solve [o oder f] <Pfad zur XML-Datei>
```
### Über Menge von Instanzen 
```
solve [o oder f] <Pfad zur zugehörigen XML-Datei> <Pfad zu Ordern mit Konfigurations-Dateien>
```

## Path-Preferences
Speicherort für Lösungen
```
solutionpath <path>     # Speicherort festlegen
get solutionpath        # solutionpath ausgeben
```
Speicherort für Probleme
```
problempath <path>      # Speicherort festlegen
get problempath         # problempath ausgeben
```
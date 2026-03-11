# Compte Rendu de TP : Implémentation du Modèle Linda et Simulation de Nœud Ferroviaire

## 1. Objectif du Projet

L'objectif de ce projet est d'implémenter un système de coordination basé sur le langage Linda pour modéliser le stationnement dans une gare ferroviaire. La simulation met en œuvre un paradigme multi-agents (Trains et Opérateur) communiquant de manière asynchrone et totalement découplée via une mémoire partagée appelée Espace de Tuples.

## 2. Architecture Logicielle

L'architecture suit les principes de séparation des responsabilités via une arborescence de type Maven claire et modulaire :

- **`com.example.core`** : Contient l'implémentation bas niveau du middleware Linda (`TupleSpace`, `Tuple`).
- **`com.example.agents`** : Implémente la logique métier sous forme d'agents concurrents (`Train`, `Operator`).
- **`com.example.App`** : Le point d'entrée central orchestre la topologie réseau, l'injection des dépendances et les scénarios de test.

## 3. Implémentation du Noyau Linda (Thread-Safety & Synchronisation)

Le cœur du système repose sur la classe `TupleSpace`. Contrairement aux systèmes à passage de messages classiques, le modèle Linda offre un découplage temporel et spatial total.

- **Moniteur de Synchronisation** : La classe utilise les verrous intrinsèques de Java (`synchronized`) pour assurer l'atomicité des accès et des mutations sur la structure de données sous-jacente (`ArrayList`).
- **Wait & NotifyAll** : Les primitives bloquantes (`in`, `rd`) emploient un mécanisme `wait()`. Chaque insertion (`out`) déclenche un `notifyAll()` pour réévaluer de façon sécurisée les gardes des threads mis en sommeil, garantissant l'absence de _busy-waiting_.
- **Sondes non-bloquantes (Probes)** : L'implémentation inclut des primitives `inp` et `rdp`. Elles sont cruciales pour l'Opérateur, lui permettant de "scanner" l'état global du système sans s'interbloquer.
- **Pattern Matching Polymorphe** : L'évaluation de correspondance est encapsulée dans `Tuple.matches()`. Afin de rendre les requêtes dynamiques, l'utilisation du mot-clé `null` agit techniquement comme un _wildcard_ (joker conditionnel), simulant le comportement d'une variable formelle dans le paradigme Linda.

## 4. Modélisation des Agents et Automates d'États

Le système s'articule autour de threads autonomes implémentant l'interface `Runnable`.

### L'Agent Train

Chaque train modélise une machine à états stricts :

1. **Émission de la demande** : Insertion asynchrone du tuple `("demande", id, direction)` via `out()`.
2. **Attente d'autorisation** : Appel d'une primitive `in(("autorisation", id))` bloquante. Le thread du train est mis en sommeil par l'ordonnanceur de l'OS (consommation CPU nulle) jusqu'à ce que l'opérateur place le tuple exact.
3. **Libération de la ressource critique** : Après le transit, l'émission du tuple `("transit_termine", id)` fonctionne comme un signal de relâchement, restituant l'accès exclusif au nœud ferroviaire.

### L'Agent Opérateur : Arbitrage et Évitement des Interblocages

L'opérateur s'exécute dans un _Daemon Thread_ et gère l'accès en exclusion mutuelle au nœud ferroviaire. Son algorithme est une boucle infinie qui garantit l'application stricte du cahier des charges [file:1] :

- **Optimisation (Règle 1)** : L'opérateur utilise un `rd` bloquant initial sur n'importe quelle demande (`("demande", null, null)`). Si le réseau est vide, le thread s'endort proprement sans consommer de ressources [file:1].
- **Snapshot de l'état (Règles 2 & 3)** : Réveillé, l'opérateur effectue des sondes `rdp` pour photographier l'état des demandes et la jauge de disponibilité des voies (`voie_libre`). Si l'entrée est demandée mais que le parking est plein, l'opérateur force explicitement un `rd` sur une demande de sortie, mettant en pause l'allocation pour éviter de bloquer indéfiniment l'entrée du nœud [file:1].
- **Gestion des conflits d'allocation (Règle 4)** : Face à une demande simultanée, l'opérateur donne prioritairement le verrou logique à l'entrée afin de purger le trafic amont. Cette priorité est conditionnellement inversée au profit de la sortie si le parking est saturé [file:1]. Cette heuristique garantit la _Liveness_ (vivacité) globale du système.

## 5. Validation par Scénario d'Intégration

La méthode `App.main` met à l'épreuve les situations aux limites (_Edge Cases_) de la gestion de trafic :

- **Saturation des capacités** : L'espace est initialement peuplé de 2 jetons `voie_libre`. Le transit simultané de T1 et T2 consomme ces jetons et démontre l'intégrité de l'exclusion mutuelle distribuée.
- **Inversion de priorité sur contention** : La tentative d'entrée de T3 dans le parking plein teste la résilience de l'opérateur. Ce dernier bloque l'accès entrant avec succès, jusqu'à ce que le thread de T1 injecte asynchrone une requête de sortie, forçant l'opérateur à réévaluer sa matrice de priorités et à débloquer le goulot d'étranglement.

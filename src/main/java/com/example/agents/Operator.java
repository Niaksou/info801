package main.java.com.example.agents;

import com.example.core.Tuple;
import com.example.core.TupleSpace;

/**
 * Agent Opérateur qui gère l'accès au nœud ferroviaire.
 * Il applique les règles de priorité du TP en lisant l'espace de tuples.
 */
public class Operator implements Runnable {

    private final TupleSpace ts;

    public Operator(TupleSpace ts) {
        this.ts = ts;
    }

    @Override
    public void run() {
        System.out.println("👷 [Opérateur] en poste, prêt à gérer le trafic.");
        
        try {
            while (true) {
                // Règle 1 : Aucune demande => l'opérateur attend (rd bloquant).
                // Le null agit comme un joker "?" : on attend n'importe quelle demande.
                ts.rd(new Tuple("demande", null, null));

                // On regarde (sans bloquer) quelles sont les demandes actuelles
                Tuple inReq = ts.rdp(new Tuple("demande", null, "entrée"));
                Tuple outReq = ts.rdp(new Tuple("demande", null, "sortie"));
                
                // On vérifie (sans bloquer) s'il y a au moins une voie de garage libre
                Tuple track = ts.rdp(new Tuple("voie_libre"));

                Tuple selectedReq = null;

                // Application des règles de l'énoncé
                if (inReq != null && outReq != null) {
                    // Règle 4 : Conflit Entrée vs Sortie
                    if (track != null) {
                        selectedReq = inReq; // Priorité à l'entrée
                        System.out.println("👷 [Opérateur] Conflit E/S : Priorité à l'ENTRÉE (voies dispos).");
                    } else {
                        selectedReq = outReq; // Blocage en entrée, priorité à la sortie
                        System.out.println("👷 [Opérateur] Conflit E/S : Priorité à la SORTIE (parking plein).");
                    }
                } 
                else if (inReq != null && outReq == null) {
                    // Règle 3 : Uniquement Entrée
                    if (track != null) {
                        selectedReq = inReq;
                    } else {
                        System.out.println("👷 [Opérateur] Parking plein. Blocage de l'entrée en attendant une sortie...");
                        // On bloque l'opérateur jusqu'à ce qu'un train demande à sortir
                        ts.rd(new Tuple("demande", null, "sortie"));
                        continue; // On recommence la boucle pour réévaluer la situation
                    }
                } 
                else if (inReq == null && outReq != null) {
                    // Règle 2 : Uniquement Sortie
                    selectedReq = outReq;
                }

                if (selectedReq != null) {
                    // 1. L'opérateur retire DÉFINITIVEMENT la demande choisie de l'espace
                    ts.in(selectedReq);
                    String trainId = (String) selectedReq.get(1);
                    String direction = (String) selectedReq.get(2);

                    // 2. Gestion des voies de garage
                    if (direction.equals("entrée")) {
                        ts.in(new Tuple("voie_libre")); // On consomme une voie
                    } else if (direction.equals("sortie")) {
                        ts.out(new Tuple("voie_libre")); // On libère une voie
                    }

                    // 3. L'opérateur donne l'autorisation d'emprunter le nœud au train sélectionné
                    System.out.println("👷 [Opérateur] Autorise le Train " + trainId + " à faire sa " + direction + ".");
                    ts.out(new Tuple("autorisation", trainId));

                    // 4. L'opérateur attend que le train ait fini de traverser le nœud 
                    // (le nœud est à usage unique et exclusif)
                    ts.in(new Tuple("transit_termine", trainId));
                    System.out.println("👷 [Opérateur] Le nœud est de nouveau libre.");
                }
            }
        } catch (InterruptedException e) {
            System.err.println("❌ [Opérateur] Fin de service (interrompu).");
            Thread.currentThread().interrupt();
        }
    }
}

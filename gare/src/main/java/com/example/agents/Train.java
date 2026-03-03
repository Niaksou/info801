package com.example.agents;

import com.example.core.Tuple;
import com.example.core.TupleSpace;

/**
 * Agent Train qui s'exécute dans son propre Thread.
 * Il interagit avec l'espace de tuples pour demander l'accès au nœud ferroviaire.
 */
public class Train implements Runnable {
    
    private final TupleSpace ts;
    private final String id;
    private final String direction; // "entrée" ou "sortie"

    public Train(TupleSpace ts, String id, String direction) {
        this.ts = ts;
        this.id = id;
        this.direction = direction;
    }

    @Override
    public void run() {
        try {
            System.out.println("🚆 [Train " + id + "] arrive et souhaite faire une " + direction);

            // 1. Le train dépose sa demande dans l'espace de tuples
            // Format du tuple : ["demande", id_du_train, "entrée" ou "sortie"]
            ts.out(new Tuple("demande", id, direction));
            System.out.println("🚆 [Train " + id + "] a émis sa demande de " + direction);

            // 2. Le train attend l'autorisation de l'opérateur pour utiliser le nœud
            // L'opérateur déposera un tuple de type ["autorisation", id_du_train]
            Tuple autorisationPattern = new Tuple("autorisation", id);
            ts.in(autorisationPattern); // Appel bloquant jusqu'à ce que l'opérateur réponde

            // 3. Le train traverse le nœud ferroviaire
            System.out.println("🟢 [Train " + id + "] a reçu l'autorisation ! Il traverse le nœud en " + direction + "...");
            Thread.sleep(1000); // Simule le temps de transit

            // 4. Le train signale qu'il a fini de traverser le nœud
            ts.out(new Tuple("transit_termine", id));
            System.out.println("🏁 [Train " + id + "] a libéré le nœud.");

        } catch (InterruptedException e) {
            System.err.println("❌ [Train " + id + "] a été interrompu !");
            Thread.currentThread().interrupt();
        }
    }
}

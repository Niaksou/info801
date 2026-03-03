package com.example;

import com.example.agents.Operator;
import com.example.agents.Train;
import com.example.core.Tuple;
import com.example.core.TupleSpace;

/**
 * Point d'entrée de la simulation Linda - Gestion de parking ferroviaire.
 */
public class App {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("🚀 Démarrage de la simulation de la Gare Ferroviaire !");
        
        // 1. Création de l'espace de tuples centralisé
        TupleSpace ts = new TupleSpace();

        // 2. Initialisation des voies de garage (ex: 2 voies max pour forcer le parking plein)
        int nbVoies = 2;
        for (int i = 0; i < nbVoies; i++) {
            ts.out(new Tuple("voie_libre"));
        }
        System.out.println("🅿️  " + nbVoies + " voies de garage initialisées.");

        // 3. Démarrage de l'agent Opérateur (Daemon Thread)
        Thread operatorThread = new Thread(new Operator(ts), "Operator-Thread");
        operatorThread.setDaemon(true); // S'arrêtera automatiquement à la fin du main
        operatorThread.start();

        // 4. Lancement des trains simulés
        
        // SCÉNARIO 1 : Deux trains arrivent en entrée (remplit le parking)
        Thread t1 = new Thread(new Train(ts, "T1", "entrée"));
        Thread t2 = new Thread(new Train(ts, "T2", "entrée"));
        
        t1.start();
        t2.start();
        
        // On attend que le parking se remplisse
        t1.join();
        t2.join();
        
        System.out.println("\n--- 🛑 Parking Plein ---");
        
        // SCÉNARIO 2 : Conflit et Blocage (Parking plein)
        // T3 veut entrer, mais le parking est plein -> l'opérateur doit le bloquer
        Thread t3 = new Thread(new Train(ts, "T3", "entrée"));
        t3.start();
        
        Thread.sleep(2000); // Laisse le temps à l'opérateur de bloquer T3
        
        // T1 décide de sortir -> l'opérateur doit donner priorité à la sortie pour débloquer
        Thread t1_sortie = new Thread(new Train(ts, "T1", "sortie"));
        t1_sortie.start();
        
        // On attend la fin de la simulation
        t3.join();
        t1_sortie.join();
        
        System.out.println("\n✅ Fin de la simulation.");
    }
}

package com.example.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Implémentation de l'espace de tuples (Tuple Space) selon le modèle Linda.
 * Les méthodes sont synchronisées pour garantir la sécurité entre les threads (Thread-Safe).
 */
public class TupleSpace {
    // La mémoire partagée contenant tous les tuples
    private final List<Tuple> tuples = new ArrayList<>();

    /**
     * Primitive 'out' : Ajoute un tuple dans l'espace.
     * Réveille tous les threads qui étaient bloqués en attente d'un tuple.
     */
    public synchronized void out(Tuple t) {
        tuples.add(t);
        // On notifie tous les threads en attente (sur in ou rd) qu'un nouveau tuple est dispo
        notifyAll(); 
    }

    /**
     * Primitive 'in' : Retire et retourne un tuple correspondant au motif.
     * BLOQUANTE : Si aucun tuple ne correspond, le thread appelant est mis en attente.
     */
    public synchronized Tuple in(Tuple pattern) throws InterruptedException {
        while (true) {
            Iterator<Tuple> it = tuples.iterator();
            while (it.hasNext()) {
                Tuple t = it.next();
                if (t.matches(pattern)) {
                    it.remove(); // Retire le tuple de l'espace
                    return t;
                }
            }
            // Aucun tuple ne correspond, le thread s'endort et libère le verrou
            wait();
        }
    }

    /**
     * Primitive 'rd' : Lit et retourne un tuple correspondant au motif (sans le retirer).
     * BLOQUANTE : Si aucun tuple ne correspond, le thread appelant est mis en attente.
     */
    public synchronized Tuple rd(Tuple pattern) throws InterruptedException {
        while (true) {
            for (Tuple t : tuples) {
                if (t.matches(pattern)) {
                    return t; // Retourne le tuple sans le supprimer
                }
            }
            // Aucun tuple ne correspond, le thread s'endort
            wait();
        }
    }

    /**
     * Primitive 'inp' (in-probe) : Version NON-BLOQUANTE de 'in'.
     * Retire un tuple s'il existe, sinon retourne immédiatement null.
     */
    public synchronized Tuple inp(Tuple pattern) {
        Iterator<Tuple> it = tuples.iterator();
        while (it.hasNext()) {
            Tuple t = it.next();
            if (t.matches(pattern)) {
                it.remove();
                return t;
            }
        }
        return null; // Retourne null au lieu de bloquer
    }

    /**
     * Primitive 'rdp' (rd-probe) : Version NON-BLOQUANTE de 'rd'.
     * Lit un tuple s'il existe, sinon retourne immédiatement null.
     */
    public synchronized Tuple rdp(Tuple pattern) {
        for (Tuple t : tuples) {
            if (t.matches(pattern)) {
                return t;
            }
        }
        return null;
    }
    
    // Méthode utilitaire pour le debug
    public synchronized void debug() {
        System.out.println("État du TupleSpace : " + tuples);
    }
}

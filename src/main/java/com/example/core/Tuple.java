package main.java.com.example.core;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Représente un Tuple dans le modèle Linda.
 * Un tuple est une collection immuable d'éléments de types arbitraires.
 */
public class Tuple {
    private final Object[] elements;

    // Constructeur avec un nombre variable d'arguments (varargs)
    public Tuple(Object... elements) {
        this.elements = Arrays.copyOf(elements, elements.length);
    }

    public int size() {
        return elements.length;
    }

    public Object get(int index) {
        return elements[index];
    }
    
    public List<Object> toList() {
        return Arrays.asList(elements);
    }

    /**
     * Vérifie si ce tuple "match" (correspond) au motif fourni.
     * Pour Linda : même taille, et chaque élément doit être égal ou correspondre au type attendu.
     * Dans une implémentation simplifiée, on va considérer que si un élément du motif est null, 
     * il agit comme un joker (wildcard "?").
     */
    public boolean matches(Tuple pattern) {
        if (this.size() != pattern.size()) {
            return false;
        }
        for (int i = 0; i < this.size(); i++) {
            Object patternElem = pattern.get(i);
            // Si l'élément du pattern est null, on l'accepte comme un joker (wildcard)
            if (patternElem != null && !patternElem.equals(this.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple tuple = (Tuple) o;
        return Arrays.equals(elements, tuple.elements);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(elements);
    }

    @Override
    public String toString() {
        return Arrays.toString(elements);
    }
}

package org.m2;

public class Article {
    String id;
    String titre;
    String lien;
    String utilisateur;
    String timestamp;
    String nbvotes;

    public Article(String id, String titre, String lien, String utilisateur, String timestamp, String nbvotes) {
        this.id = id;
        this.titre = titre;
        this.lien = lien;
        this.utilisateur = utilisateur;
        this.timestamp = timestamp;
        this.nbvotes = nbvotes;
    }
}

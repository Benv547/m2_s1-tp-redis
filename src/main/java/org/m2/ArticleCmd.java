package org.m2;

import io.lettuce.core.MapScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.ScoredValueScanCursor;
import io.lettuce.core.api.sync.*;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArticleCmd {
    private static final Long UNE_SEMAINE = Long.valueOf(700000000);
    private static final Long SCORE_FOR_VOTE = Long.valueOf(197);

    public void test () {
    }

    public static String ajoutArticle(RedisCommands<String, String> cmd,
                                      String utilisateur,
                                      String titre, String url) {

        // On trouve le nouveau numéro
        String articleId = String.valueOf(cmd.incr("article:"));
        String articleSelectionne = "selectionne:" + articleId;

        // Creation d'un lien entre l'article et l'utilisateur : vote
        cmd.sadd(articleSelectionne, utilisateur);

        // On demande à Redis de faire expirer la clé dans X sec.
        cmd.expire(articleSelectionne, UNE_SEMAINE);

        long now = System.currentTimeMillis() / 1000;
        String article = "article:" + articleId;

        // On créé les données
        HashMap<String,String> donnees = new HashMap<>();
        donnees.put("titre", titre);
        donnees.put("lien", url);
        donnees.put("utilisateur", utilisateur);
        donnees.put("timestamp", String.valueOf(now));
        donnees.put("nbvotes", "1");
        cmd.hmset(article, donnees);

        // Initialize score
        String score = "score:";
        cmd.zadd(score, now + SCORE_FOR_VOTE, article);

        // Initialize time
        String time = "time:";
        cmd.zadd(time, now, article);

        return articleId;
    }

    public static Article getArticle(RedisCommands<String, String> cmd, String articleId) {
        String a = articleId;
        Map<String,String> article = cmd.hscan(a).getMap();
        return new Article(a, article.get("titre"), article.get("lien"), article.get("utilisateur"), article.get("timestamp"), article.get("nbvotes"));
    }

    public static String addScoreArticle(RedisCommands<String, String> cmd, String articleId, long scoreValue) {
        String score = "score:";
        String article = "article:" + articleId;
        String newScore = String.valueOf(cmd.zincrby(score, scoreValue, article));
        return newScore;
    }

    public static void addVote(RedisCommands<String, String> cmd, String articleId, String utilisateur) {

        String article = "article:" + articleId;
        String articleSelectionne = "selectionne:" + articleId;

        // Check if user hasn't already voted
        if (cmd.sismember(articleSelectionne, utilisateur)) {
            return;
        }

        cmd.sadd(articleSelectionne, utilisateur);
        cmd.expire(articleSelectionne, UNE_SEMAINE);

        // Scoring for +1 vote
        addScoreArticle(cmd, articleId, SCORE_FOR_VOTE);
        String.valueOf(cmd.hincrby(article, "nbvotes", 1));
    }

    public static List<Article> getAllArticles (RedisCommands<String, String> cmd) {
        List<Article> articles = new ArrayList<>();

        String time = "time:";
        List<ScoredValue<String>> result = cmd.zscan(time).getValues();
        for (ScoredValue<String> scoredValue: result) {
            Map<String,String> article = cmd.hscan(scoredValue.getValue()).getMap();
            Article a = new Article(scoredValue.getValue(), article.get("titre"), article.get("lien"), article.get("utilisateur"), article.get("timestamp"), article.get("nbvotes"));
            articles.add(a);
        }
        return articles;
    }

    public static List<Article> getTopTenVotedArticles (RedisCommands<String, String> cmd) {
        List<Article> articles = new ArrayList<>();

        String score = "score:";
        List<String> result = cmd.zrevrange(score, 0, 10);
        for (String value: result) {
            Map<String,String> article = cmd.hscan(value).getMap();
            Article a = new Article(value, article.get("titre"), article.get("lien"), article.get("utilisateur"), article.get("timestamp"), article.get("nbvotes"));
            articles.add(a);
        }
        return articles;
    }

    public static void addArticleToGroup (RedisCommands<String, String> cmd, String articleId, String groupe) {
        String categorie = "catégorie:" + groupe;
        String article = "article:" + articleId;

        // Check if not already present in set
        if (cmd.sismember(categorie, article)) {
            return;
        }

        cmd.sadd(categorie, article);
    }

    public static void removeArticleToGroup (RedisCommands<String, String> cmd, String articleId, String groupe) {
        String categorie = "catégorie:" + groupe;
        String article = "article:" + articleId;

        // Check if not present in set
        if (!cmd.sismember(categorie, article)) {
            return;
        }

        cmd.srem(categorie, article);
    }

    public static HashMap<String, Double> getScoresOfGroupe (RedisCommands<String, String> cmd, String groupe) {
        HashMap<String, Double> result = new HashMap<>();

        String categorie = "catégorie:" + groupe;
        List<String> listGroupe = cmd.sscan(categorie).getValues();

        String score = "score:";
        List<ScoredValue<String>> scores = cmd.zrevrangeWithScores(score, 0, -1);
        for (ScoredValue scoredValue: scores) {
            if (listGroupe.contains(scoredValue.getValue())) {
                result.put(scoredValue.getValue().toString(), scoredValue.getScore());
            }
        }

        return result;
    }
}

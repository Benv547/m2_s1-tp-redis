package org.m2;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.HashMap;
import java.util.List;

class TestRedis {

    // TODO : envoyer à "olivier.perrin@loria.fr avec le repot GIT
    // [M2 SID] ...

    public static final String host = "redis://localhost";
    public RedisClient redisClient;
    public StatefulRedisConnection<String, String> connection;
    public RedisCommands<String, String> cmd;

    @BeforeEach
    public void cleanUpDB() {
        redisClient = RedisClient.create(host);
        connection = redisClient.connect();
        connection.flushCommands();
        cmd = connection.sync();
        cmd.flushall();
    }
    
    @AfterEach
    public void closeDB() {
        connection.close();
        redisClient.shutdown();
    }

    @Test
    public void testConnexion() {
        assertEquals(cmd.ping(),"PONG");
    }

    @Test
    void testSetGet() {
        cmd.set("foo", "bar");
        String value = cmd.get("foo");
        assertEquals(value,"bar");
    }

    // TODO
    @Test
    void testAjoutArticle() {
        // TODO
        String id = ArticleCmd.ajoutArticle(cmd, "robert", "titre de test", "https://google.fr");
        String value = cmd.hget("article:" + id, "utilisateur");
        assertEquals("robert", value);
    }

    // TODO
    @Test
    void testAddScoreArticle() {
        String id = ArticleCmd.ajoutArticle(cmd, "robert", "titre de test", "https://google.fr");
        ArticleCmd.addScoreArticle(cmd, id, 10);

        Article a = ArticleCmd.getArticle(cmd, "article:" + id);

        Double value = cmd.zscore("score:", "article:" + id);
        assertEquals(Long.valueOf(a.timestamp) + 197 + 10, value);
    }

    // TODO
    @Test
    void testAddVote() {
        String id = ArticleCmd.ajoutArticle(cmd, "robert", "titre de test", "https://google.fr");
        Double old = cmd.zscore("score:", "article:" + id);
        ArticleCmd.addVote(cmd, id, "hubert");
        Double value = cmd.zscore("score:", "article:" + id);
        assertEquals(old + 197, value);
    }

    // TODO
    @Test
    void testGetAllArticles() {
        String id = ArticleCmd.ajoutArticle(cmd, "robert", "titre de test", "https://google.fr");
        String id2 = ArticleCmd.ajoutArticle(cmd, "robert", "titre de test", "https://google.fr");
        int value = ArticleCmd.getAllArticles(cmd).size();

        assertEquals(2, value);
    }

    // TODO
    @Test
    void testGetTopTenVotedArticles() {
        String id = ArticleCmd.ajoutArticle(cmd, "robert", "titre de test", "https://google.fr");
        String id2 = ArticleCmd.ajoutArticle(cmd, "robert", "titre de test", "https://google.fr");
        ArticleCmd.addVote(cmd, id2, "hubert");

        List<Article> result = ArticleCmd.getTopTenVotedArticles(cmd);
        assertEquals(2, result.size());

        Article a = result.get(0);
        assertEquals("article:" + id2, a.id);

        Article a2 = result.get(1);
        assertEquals("article:" + id, a2.id);
    }

    // TODO
    @Test
    void testAddArticleToGroup() {
        String id = ArticleCmd.ajoutArticle(cmd, "robert", "titre de test", "https://google.fr");
        ArticleCmd.addArticleToGroup(cmd, id, "java");

        assertTrue(cmd.sismember("catégorie:java", "article:" + id));
    }

    // TODO
    @Test
    void testRemoveArticleToGroup() {
        String id = ArticleCmd.ajoutArticle(cmd, "robert", "titre de test", "https://google.fr");
        ArticleCmd.addArticleToGroup(cmd, id, "java");
        ArticleCmd.removeArticleToGroup(cmd, id, "java");

        assertFalse(cmd.sismember("catégorie:java", "article:" + id));
    }

    // TODO
    @Test
    void testGetScoresOfGroupe() {
        String id = ArticleCmd.ajoutArticle(cmd, "robert", "titre de test1", "https://google.fr");
        String id2 = ArticleCmd.ajoutArticle(cmd, "hugues", "titre de test2", "https://google.fr");
        String id3 = ArticleCmd.ajoutArticle(cmd, "jean", "titre de test3", "https://google.fr");

        ArticleCmd.addArticleToGroup(cmd, id, "java");
        ArticleCmd.addArticleToGroup(cmd, id2, "java");

        HashMap<String, Double> result = ArticleCmd.getScoresOfGroupe(cmd, "java");
        assertEquals(2, result.size());

        assertTrue(result.containsKey("article:" + id));
        assertTrue(result.containsKey("article:" + id2));
        assertFalse(result.containsKey("article:" + id3));
    }

}

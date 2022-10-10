package org.m2;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

class TestRedis {

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

    @Test
    void testAjoutArticle() {
        // TODO
        ArticleCmd.ajoutArticle(cmd, "robert", "title", "url");
    }
}

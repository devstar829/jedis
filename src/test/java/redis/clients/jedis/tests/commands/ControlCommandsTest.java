package redis.clients.jedis.tests.commands;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import redis.clients.jedis.DebugParams;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisMonitor;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.args.ClientPauseMode;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.util.SafeEncoder;

public class ControlCommandsTest extends JedisCommandTestBase {
  @Test
  public void save() {
    try {
      String status = jedis.save();
      assertEquals("OK", status);
    } catch (JedisDataException e) {
      assertTrue("ERR Background save already in progress".equalsIgnoreCase(e.getMessage()));
    }
  }

  @Test
  public void bgsave() {
    try {
      String status = jedis.bgsave();
      assertEquals("Background saving started", status);
    } catch (JedisDataException e) {
      assertTrue("ERR Background save already in progress".equalsIgnoreCase(e.getMessage()));
    }
  }

  @Test
  public void bgrewriteaof() {
    String scheduled = "Background append only file rewriting scheduled";
    String started = "Background append only file rewriting started";

    String status = jedis.bgrewriteaof();

    boolean ok = status.equals(scheduled) || status.equals(started);
    assertTrue(ok);
  }

  @Test
  public void lastsave() throws InterruptedException {
    long saved = jedis.lastsave();
    assertTrue(saved > 0);
  }

  @Test
  public void info() {
    String info = jedis.info();
    assertNotNull(info);
    info = jedis.info("server");
    assertNotNull(info);
  }

  @Test
  public void readonly() {
    try {
      jedis.readonly();
    } catch (JedisDataException e) {
      assertTrue("ERR This instance has cluster support disabled".equalsIgnoreCase(e.getMessage()));
    }
  }

  @Test
  public void readwrite() {
    try {
      jedis.readwrite();
    } catch (JedisDataException e) {
      assertTrue("ERR This instance has cluster support disabled".equalsIgnoreCase(e.getMessage()));
    }
  }

  @Test
  public void monitor() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          // sleep 100ms to make sure that monitor thread runs first
          Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        Jedis j = new Jedis();
        j.auth("foobared");
        for (int i = 0; i < 5; i++) {
          j.incr("foobared");
        }
        j.disconnect();
      }
    }).start();

    jedis.monitor(new JedisMonitor() {
      private int count = 0;

      @Override
      public void onCommand(String command) {
        if (command.contains("INCR")) {
          count++;
        }
        if (count == 5) {
          client.disconnect();
        }
      }
    });
  }

  @Test
  public void configGet() {
    List<String> info = jedis.configGet("m*");
    assertNotNull(info);
  }

  @Test
  public void configSet() {
    List<String> info = jedis.configGet("maxmemory");
    assertEquals("maxmemory", info.get(0));
    String memory = info.get(1);
    assertEquals("OK", jedis.configSet("maxmemory", "200"));
    assertEquals("OK", jedis.configSet("maxmemory", memory));
  }

  @Test
  public void configGetSetBinary() {
    byte[] maxmemory = SafeEncoder.encode("maxmemory");
    List<byte[]> info = jedis.configGet(maxmemory);
    assertArrayEquals(maxmemory, info.get(0));
    byte[] memory = info.get(1);
    assertEquals("OK", jedis.configSet(maxmemory, Protocol.toByteArray(200)));
    assertEquals("OK", jedis.configSet(maxmemory, memory));
  }

  @Test
  public void configGetSetBinary2() {
    byte[] maxmemory = SafeEncoder.encode("maxmemory");
    List<byte[]> info = jedis.configGet(maxmemory);
    assertArrayEquals(maxmemory, info.get(0));
    byte[] memory = info.get(1);
    assertEquals("OK", jedis.configSetBinary(maxmemory, memory));
  }

  @Test
  public void debug() {
    jedis.set("foo", "bar");
    String resp = jedis.debug(DebugParams.OBJECT("foo"));
    assertNotNull(resp);
    resp = jedis.debug(DebugParams.RELOAD());
    assertNotNull(resp);
  }

  @Test
  public void waitReplicas() {
    assertEquals(1, jedis.waitReplicas(1, 100));
  }

  @Test
  public void clientPause() throws InterruptedException, ExecutionException {
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    try {
      final Jedis jedisToPause1 = createJedis();
      final Jedis jedisToPause2 = createJedis();

      int pauseMillis = 1250;
      jedis.clientPause(pauseMillis);

      Future<Long> latency1 = executorService.submit(new Callable<Long>() {
        @Override
        public Long call() throws Exception {
          long startMillis = System.currentTimeMillis();
          assertEquals("PONG", jedisToPause1.ping());
          return System.currentTimeMillis() - startMillis;
        }
      });
      Future<Long> latency2 = executorService.submit(new Callable<Long>() {
        @Override
        public Long call() throws Exception {
          long startMillis = System.currentTimeMillis();
          assertEquals("PONG", jedisToPause2.ping());
          return System.currentTimeMillis() - startMillis;
        }
      });

      long latencyMillis1 = latency1.get();
      long latencyMillis2 = latency2.get();

      int pauseMillisDelta = 100;
      assertTrue(pauseMillis <= latencyMillis1 && latencyMillis1 <= pauseMillis + pauseMillisDelta);
      assertTrue(pauseMillis <= latencyMillis2 && latencyMillis2 <= pauseMillis + pauseMillisDelta);

      jedisToPause1.close();
      jedisToPause2.close();
    } finally {
      executorService.shutdown();
      if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    }
  }

  @Test
  public void clientPauseAll() throws InterruptedException, ExecutionException {
    final int pauseMillis = 1250;
    final int pauseMillisDelta = 100;

    ExecutorService executorService = Executors.newFixedThreadPool(1);
    try (Jedis jedisPause = createJedis()) {

      jedis.clientPause(pauseMillis, ClientPauseMode.ALL);

      Future<Long> latency = executorService.submit(new Callable<Long>() {
        @Override
        public Long call() throws Exception {
          long startMillis = System.currentTimeMillis();
          jedisPause.get("key");
          return System.currentTimeMillis() - startMillis;
        }
      });

      long latencyMillis = latency.get();
      assertTrue(pauseMillis <= latencyMillis && latencyMillis <= pauseMillis + pauseMillisDelta);

    } finally {
      executorService.shutdown();
      if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    }
  }

  @Test
  public void clientPauseWrite() throws InterruptedException, ExecutionException {
    final int pauseMillis = 1250;
    final int pauseMillisDelta = 100;

    ExecutorService executorService = Executors.newFixedThreadPool(2);
    try (Jedis jedisRead = createJedis(); Jedis jedisWrite = createJedis();) {

      jedis.clientPause(pauseMillis, ClientPauseMode.WRITE);

      Future<Long> latencyRead = executorService.submit(new Callable<Long>() {
        @Override
        public Long call() throws Exception {
          long startMillis = System.currentTimeMillis();
          jedisRead.get("key");
          return System.currentTimeMillis() - startMillis;
        }
      });
      Future<Long> latencyWrite = executorService.submit(new Callable<Long>() {
        @Override
        public Long call() throws Exception {
          long startMillis = System.currentTimeMillis();
          jedisWrite.set("key", "value");
          return System.currentTimeMillis() - startMillis;
        }
      });

      long latencyReadMillis = latencyRead.get();
      assertTrue(0 <= latencyReadMillis && latencyReadMillis <= pauseMillisDelta);

      long latencyWriteMillis = latencyWrite.get();
      assertTrue(pauseMillis <= latencyWriteMillis && latencyWriteMillis <= pauseMillis + pauseMillisDelta);

    } finally {
      executorService.shutdown();
      if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    }
  }

  @Test
  public void memoryDoctorString() {
    String memoryInfo = jedis.memoryDoctor();
    assertNotNull(memoryInfo);
  }

  @Test
  public void memoryDoctorBinary() {
    byte[] memoryInfo = jedis.memoryDoctorBinary();
    assertNotNull(memoryInfo);
  }

  @Test
  public void memoryUsageString() {
    // Note: It has been recommended not to base MEMORY USAGE test on exact value, as the response
    // may subject to be 'tuned' especially targeting a major Redis release.

    jedis.set("foo", "bar");
    long usage = jedis.memoryUsage("foo");
    assertTrue(usage >= 30);
    assertTrue(usage <= 80);

    jedis.lpush("foobar", "fo", "ba", "sha");
    usage = jedis.memoryUsage("foobar", 2);
    assertTrue(usage >= 110);
    assertTrue(usage <= 190);

    assertNull(jedis.memoryUsage("roo", 2));
  }

  @Test
  public void memoryUsageBinary() {
    // Note: It has been recommended not to base MEMORY USAGE test on exact value, as the response
    // may subject to be 'tuned' especially targeting a major Redis release.

    byte[] bfoo = {0x01, 0x02, 0x03, 0x04};
    byte[] bbar = {0x05, 0x06, 0x07, 0x08};
    byte[] bfoobar = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};

    jedis.set(bfoo, bbar);
    long usage = jedis.memoryUsage(bfoo);
    assertTrue(usage >= 30);
    assertTrue(usage <= 80);

    jedis.lpush(bfoobar, new byte[]{0x01, 0x02}, new byte[]{0x05, 0x06}, new byte[]{0x00});
    usage = jedis.memoryUsage(bfoobar, 2);
    assertTrue(usage >= 110);
    assertTrue(usage <= 190);

    assertNull(jedis.memoryUsage("roo", 2));
  }
}

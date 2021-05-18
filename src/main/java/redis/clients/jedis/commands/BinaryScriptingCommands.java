package redis.clients.jedis.commands;

import redis.clients.jedis.args.FlushMode;

import java.util.List;

public interface BinaryScriptingCommands {

  /**
   * @deprecated Use {@link #eval(byte..., int, byte[]...)}.
   */
  @Deprecated
  Object eval(byte[] script, byte[] keyCount, byte[]... params);

  Object eval(byte[] script, int keyCount, byte[]... params);

  Object eval(byte[] script, List<byte[]> keys, List<byte[]> args);

  Object eval(byte[] script);

  Object evalsha(byte[] sha1);

  Object evalsha(byte[] sha1, List<byte[]> keys, List<byte[]> args);

  Object evalsha(byte[] sha1, int keyCount, byte[]... params);

  // TODO: should be Boolean, add singular version
  List<Long> scriptExists(byte[]... sha1);

  byte[] scriptLoad(byte[] script);

  String scriptFlush();

  String scriptFlush(FlushMode flushMode);

  String scriptKill();
}

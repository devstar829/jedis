package redis.clients.jedis.timeseries;

import static redis.clients.jedis.Protocol.toByteArray;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.*;

import java.util.LinkedHashMap;
import java.util.Map;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.params.IParams;

/**
 * Represents optional arguments of TS.INCRBY or TS.DECRBY commands.
 */
public class TSIncrOrDecrByParams implements IParams {

  private Long timestamp;
  private Long retentionPeriod;
  private EncodingFormat encoding;
  private Long chunkSize;
  private DuplicatePolicy duplicatePolicy;

  private boolean ignore;
  private long ignoreMaxTimediff;
  private double ignoreMaxValDiff;

  private Map<String, String> labels;

  public TSIncrOrDecrByParams() {
  }

  public static TSIncrOrDecrByParams params() {
    return new TSIncrOrDecrByParams();
  }

  public static TSIncrOrDecrByParams incrByParams() {
    return new TSIncrOrDecrByParams();
  }

  public static TSIncrOrDecrByParams decrByParams() {
    return new TSIncrOrDecrByParams();
  }

  public TSIncrOrDecrByParams timestamp(long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public TSIncrOrDecrByParams retention(long retentionPeriod) {
    this.retentionPeriod = retentionPeriod;
    return this;
  }

  public TSIncrOrDecrByParams encoding(EncodingFormat encoding) {
    this.encoding = encoding;
    return this;
  }

  public TSIncrOrDecrByParams chunkSize(long chunkSize) {
    this.chunkSize = chunkSize;
    return this;
  }

  public TSIncrOrDecrByParams duplicatePolicy(DuplicatePolicy duplicatePolicy) {
    this.duplicatePolicy = duplicatePolicy;
    return this;
  }

  public TSIncrOrDecrByParams ignore(long maxTimediff, double maxValDiff) {
    this.ignore = true;
    this.ignoreMaxTimediff = maxTimediff;
    this.ignoreMaxValDiff = maxValDiff;
    return this;
  }

  /**
   * Set label-value pairs
   *
   * @param labels label-value pairs
   * @return the object itself
   */
  public TSIncrOrDecrByParams labels(Map<String, String> labels) {
    this.labels = labels;
    return this;
  }

  /**
   * Add label-value pair. Multiple pairs can be added through chaining.
   * @param label
   * @param value
   * @return the object itself
   */
  public TSIncrOrDecrByParams label(String label, String value) {
    if (this.labels == null) {
      this.labels = new LinkedHashMap<>();
    }
    this.labels.put(label, value);
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {

    if (timestamp != null) {
      args.add(TIMESTAMP).add(timestamp);
    }

    if (retentionPeriod != null) {
      args.add(RETENTION).add(toByteArray(retentionPeriod));
    }

    if (encoding != null) {
      args.add(ENCODING).add(encoding);
    }

    if (chunkSize != null) {
      args.add(CHUNK_SIZE).add(toByteArray(chunkSize));
    }

    if (duplicatePolicy != null) {
      args.add(DUPLICATE_POLICY).add(duplicatePolicy);
    }

    if (ignore) {
      args.add(IGNORE).add(ignoreMaxTimediff).add(ignoreMaxValDiff);
    }

    if (labels != null) {
      args.add(LABELS);
      labels.entrySet().forEach((entry) -> args.add(entry.getKey()).add(entry.getValue()));
    }
  }
}

package cn.cygao.ams.service;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis操作工具类
 *
 * @author 王赛超、STEA_YY、cygao
 */
@SuppressWarnings("unchecked")
@Service
public class RedisService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // =============================common============================

    /**
     * 指定缓存失效时间
     *
     * @param key  键
     * @param time 时间(秒)
     */
    public void expire(String key, long time) {
        if (time > 0) {
            redisTemplate.expire(key, time, TimeUnit.SECONDS);
        }
    }

    /**
     * 指定缓存失效时间戳
     *
     * @param key  键
     * @param date 失效时间戳
     */
    public void expireAt(String key, Date date) {
        redisTemplate.expireAt(key, date);
    }

    /**
     * 删除缓存
     *
     * @param key 一个或多个键
     */
    @SuppressWarnings("unchecked")
    public void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete(CollectionUtils.arrayToList(key));
            }
        }
    }

    // ============================String=============================

    /**
     * 普通缓存获取
     *
     * @param key 键
     * @return 获取到的对象
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存写入
     *
     * @param key   键
     * @param value 值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒) time要大于0 如果time小于等于0 将设置无限期
     */
    public void set(String key, Object value, long time) {
        if (time > 0) {
            redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
        } else {
            set(key, value);
        }
    }

    public void incr(String key, double by) {
        redisTemplate.opsForValue().increment(key, by);
    }

    /**
     * 获取hashKey对应的所有键值
     *
     * @param key 键
     */
    public <K, V> Map<K, V> hGetMap(String key) {
        return (Map<K, V>) redisTemplate.opsForHash().entries(key);
    }

    /**
     * 删除hash表中的值
     *
     * @param key  键 不能为null
     * @param item 项 可以使多个 不能为null
     */
    public void hDel(String key, Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }

    /**
     * hash递增 如果不存在,就会创建一个 并把新增后的值返回
     *
     * @param key  键
     * @param item 项
     * @param by   要增加几(大于0)
     * @return 增加后的数值
     */
    public double hIncr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, by);
    }

    /**
     * 哈希缓存放入
     *
     * @param key     键
     * @param hashKey 哈希键
     * @param item    值
     */
    public void hPut(String key, Object hashKey, Object item) {
        redisTemplate.opsForHash().put(key, hashKey, item);
    }

    /**
     * set添加元素
     *
     * @param key   键
     * @param value 值
     */
    public <T> void sAdd(String key, T... value) {
        redisTemplate.opsForSet().add(key, value);
    }

    /**
     * 查询某个对象是否存在set中
     */
    public <T> boolean sExist(String key, T value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    /**
     * 获取整个set
     */
    public <T> Set<T> sGet(String key) {
        return (Set<T>) redisTemplate.opsForSet().members(key);
    }

    /**
     * 删除set的某个元素
     */
    public <T> void sDel(String key, T value) {
        redisTemplate.opsForSet().remove(key, value);
    }

    /**
     * 获取set的长度
     */
    public long sSize(String key) {
        return Optional.ofNullable(redisTemplate.opsForSet().size(key)).orElse(0L);
    }

    /**
     * 获取hash的值
     */
    public <T> T hGet(String key, Object hashKey) {
        return (T) redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 获取元素降序排名
     *
     * @param key   键
     * @param start 开始项
     * @param end   结束项
     * @return ZSet对象
     */
    public <T> Set<T> zRevRange(String key, long start, long end) {
        return (Set<T>) redisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    /**
     * 获取元素升序排名
     *
     * @param key   键
     * @param start 开始项
     * @param end   结束项
     * @return Set对象
     */
    public <T> Set<T> zRange(String key, long start, long end) {
        return (Set<T>) redisTemplate.opsForZSet().range(key, start, end);
    }

    /**
     * 获取zSet里value对应的排名
     *
     * @param key   键
     * @param value 值
     * @return 排名
     */
    public Long zGetRank(String key, Object value) {
        return redisTemplate.opsForZSet().reverseRank(key, value);
    }

    /**
     * 获取zSet里value对应的分数
     *
     * @param key   键
     * @param value 值
     * @return 分数
     */
    public Double zGetScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    /**
     * zSet写入
     *
     * @param key   键
     * @param value 值
     * @param score 分数
     */
    public void zAdd(String key, Object value, double score) {
        redisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     * zSet删除score在范围内的元素
     *
     * @param key 键
     * @param min 最小score
     * @param max 最大score
     * @return 删除的元素数量
     */
    public long zRemoveRangeByScore(String key, double min, double max) {
        return Optional.ofNullable(redisTemplate.opsForZSet().removeRangeByScore(key, min, max)).orElse(0L);
    }

    /**
     * list入队
     *
     * @param key   键
     * @param value 值
     */
    public <T> void lPush(String key, T value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * list入队
     *
     * @param key    键
     * @param values 值集合
     */
    public <T> void lPushAll(String key, T... values) {
        redisTemplate.opsForList().leftPushAll(key, values);
    }

    /**
     * list出队
     *
     * @param key 键
     * @return 出队的对象
     */
    public Object lPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    /**
     * 按索引获取List中的元素
     *
     * @param key   键值
     * @param index 索引
     * @return 对象
     */
    public Object lIndex(String key, int index) {
        return redisTemplate.opsForList().index(key, index);
    }

    /**
     * 获取list的大小
     *
     * @param key 键值
     * @return list长度
     */
    public Long lSize(String key) {
        return redisTemplate.opsForList().size(key);
    }

    /**
     * 获取队列
     *
     * @param key   键值
     * @param start 开始索引
     * @param end   结束索引
     * @return 对象列表
     */
    public <T> List<T> lRange(String key, int start, int end) {
        return (List<T>) redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 删除list的元素
     *
     * @param key   键
     * @param count 删除元素的个数
     * @param value 删除的对象
     */
    public <T> void lRem(String key, long count, T value) {
        redisTemplate.opsForList().remove(key, count, value);
    }

    /**
     * 判断key是否存在
     *
     * @param key redis键值
     * @return 是否存在
     */
    public boolean hasKey(String key) {
        if (key == null) {
            return false;
        }
        Boolean res = redisTemplate.hasKey(key);
        return res != null && res;
    }

    /**
     * zSet删除
     *
     * @param key   键
     * @param value 值
     */
    public void zRem(String key, Object... value) {
        redisTemplate.opsForZSet().remove(key, value);
    }

    /**
     * 获取zSet长度
     *
     * @param key 键
     * @return count
     */
    public Long zCard(String key) {
        return redisTemplate.opsForZSet().zCard(key);
    }

    /**
     * 设置BitMap位图的bit位
     *
     * @param key    键
     * @param offset 偏移量(一般为id)
     * @param value  结果
     */
    public void setBit(String key, int offset, boolean value) {
        redisTemplate.opsForValue().setBit(key, offset, value);
    }

    /**
     * 获取位图的bit位
     *
     * @param key    键
     * @param offset 偏移量
     * @return bit位
     */
    public Boolean getBit(String key, int offset) {
        return redisTemplate.opsForValue().getBit(key, offset);
    }

    /**
     * 获取位图中为1的个数
     *
     * @param key 键
     * @return bit为1的个数
     */
    public Long bitCount(String key) {
        return redisTemplate.execute((RedisCallback<Long>) conn -> conn.bitCount(key.getBytes()));
    }

    /**
     * 设置HyperLogLog（一般用于统计阅读数）
     *
     * @param key   键
     * @param value 值
     */
    public final <T> void setHyperLogLog(String key, T... value) {
        redisTemplate.opsForHyperLogLog().add(key, value);
    }

    /**
     * 获取hyperLogLog长度
     */
    public long getHyperLogLog(String key) {
        return Optional.ofNullable(redisTemplate.opsForHyperLogLog().size(key)).orElse(0L);
    }

    public Set<String> getKeysByPrefix(String keyPrefix) {
        return redisTemplate.keys(keyPrefix);
    }

}
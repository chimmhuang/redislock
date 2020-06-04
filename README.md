<h1 style="text-align: center">RedisLock——让 Redis 分布式锁变得简单</h1>

<p align="left">
	<img src='https://img.shields.io/github/license/chimmhuang/resetAutoincrement' alt='lisence'></img>
	<img src="https://img.shields.io/badge/JDK-1.8%2B-red" alt='jdk'></img>
	<img src="https://img.shields.io/badge/spring--version-%3E%3D2.1-success"/>
	<img src="https://img.shields.io/badge/redis--version-%3E%3D2.2.0-red"/>
</p>

# 1. 项目介绍
该项目主要简化了使用 redis 分布式事务所的操作，实现傻瓜式加锁，释放锁的操作，并优雅的实现了等待锁释放的操作。等待锁释放的过程主要是使用了redis的监听功能，所以在使用该项目前，要确保redis已经开启了key事件监听，即“Ex”。  

- 如何查看 redis 是否已经开启了监听功能？  
    登录 redis 后，使用命令 `config get notify-keyspace-events` 进行查看
    
# 2. 快速使用
## 2.1 引入 maven 坐标
```xml
<dependency>
    <groupId>com.github.chimmhuang</groupId>
    <artifactId>redislock</artifactId>
    <version>1.0-RELEASE</version>
</dependency>
```

## 2.2 注册 RedLock

- 方式一（推荐）： 在项目的启动类上添加包扫描的路径
```java
@ComponentScan(basePackages = "com.github.chimmhuang.redislock")
@SpringBootApplication
public class DataSteelQuotesApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataSteelQuotesApplication.class, args);
    }
}
```

- 方式二：手动注册相关的 bean
```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    @Bean
    public RedisListener redisListener(RedisMessageListenerContainer redisMessageListenerContainer) {
        return new RedisListener(redisMessageListenerContainer);
    }

    @Bean
    public RedisLock redisLock(RedisTemplate redisTemplate) {
        return new RedisLock(redisTemplate);
    }
}
```

## 2.3 使用
1. 注入 `redisLock`
2. 使用 `redisLock.lock()` 进行加锁
3. 使用 `redisLock.unlock()` 进行解锁

以下提供一个单元测试的案例（火车站卖票的案例）
```java
@RunWith(SpringRunner.class)
@SpringBootTest
public class RedisListenerTest {

    @Autowired
    private RedisLock redisLock;

    /** 100张票 */
    private static Integer count = 100;

    @Test
    public void ticketTest() throws Exception {
        TicketRunnable tr = new TicketRunnable();
        // 四个线程对应四个窗口
        Thread t1 = new Thread(tr,"窗口A");
        Thread t2 = new Thread(tr,"窗口B");
        Thread t3 = new Thread(tr,"窗口C");
        Thread t4 = new Thread(tr,"窗口D");

        t1.start();
        t2.start();
        t3.start();
        t4.start();

        Thread.currentThread().join();

    }

    public class TicketRunnable implements Runnable {
        @Override
        public void run() {
            while (count > 0) {
                redisLock.lock("ticketLock", 3L, TimeUnit.SECONDS);
                if (count > 0) {
                    System.out.println(Thread.currentThread().getName() + "售出第" + (count--) + "张火车票");
                }
                redisLock.unlock("ticketLock");

                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
```

# 3. 参与贡献
非常欢迎你的加入！[提一个 Issue](https://github.com/chimmhuang/redislock/issues/new) 或者提交一个 Pull Request。

# 4. 联系作者
QQ(Wechat) : 905369866  
Email : chimmhuang@163.com

# 5. 开源协议
[MIT](LICENSE) © Chimm Huang

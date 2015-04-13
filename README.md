zpush
=====

http://blog.csdn.net/hengyunabc/article/details/24325295

http://blog.csdn.net/hengyunabc/article/details/25435739


Apple APNs java client, based on netty4.
 
基于netty4实现的苹果通知推送服务Java客户端。

默认是用JDK7来编绎的，如果想用JDK6,可以修改pom.xml里maven-compiler-plugin的source和target为1.6。

特点：
* 支持第三版通知推送，即command = 2。目前的绝大部分Java客户端都只支持command = 1，即第二版。
* 支持SSL握手成功才返回，可以调用 pushManager.start().sync(); 等待握手成功才开始发送；
* 最大限度重试发送，内部自动处理重连，错误重发机制；
* 支持配置RejectListener，即通知被Apple服务器拒绝之后的回调接口；
* 支持配置ShutdownListener，即当shutdown时，没有发送完的消息处理的回调接口；
* 支持发送统计信息；
* 实现组件分离，可以利用PushClient，FeedbackClient来写一些灵活的代码；
* Notification发送者可以自己定义设置发送的Queue，自己灵活处理阻塞，超时等问题。

Example:

More example under src/test/java.
```java
public class MainExample {
	public static void main(String[] args) throws InterruptedException {
		Environment environment = Environment.Product;
		String password = "123456";
		String keystore = "/home/hengyunabc/test/apptype/app_type_1/productAPNS.p12";
		PushManager pushManager = new PushManagerImpl(keystore, password, environment);
		
		//set a push queue
		BlockingQueue<Notification> queue = new LinkedBlockingQueue<Notification>(8192);
		pushManager.setQueue(queue );
		
		//waiting for SSL handshake success
		pushManager.start().sync();

		//build a notification
		String token = "5f6aa01d8e3358949b7c25d461bb78ad740f4707462c7eafbebcf74fa5ddb387";
		Notification notification = new NotificationBuilder()
				.setToken(token)
				.setBadge(1)
				.setPriority(5)
				.setAlertBody("xxxxx").build();

		//put notification into the queue
		queue.put(notification);
		
		TimeUnit.SECONDS.sleep(10);
		
		//get statistic info
		Statistic statistic = pushManager.getStatistic();
		System.out.println(statistic);
	}
}
```

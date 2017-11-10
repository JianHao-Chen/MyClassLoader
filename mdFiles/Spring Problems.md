---
title: Spring知识点回顾
categories: Java笔记
---


<!--more-->

---


#### Spring能帮我们做什么?
* Spring能帮我们根据配置文件创建及组装对象之间的依赖关系。
* Spring 面向切面编程能帮助我们无耦合的实现日志记录，性能统计，安全控制。
* Spring能非常简单的帮我们管理数据库事务。
* Spring还方便我们访问数据库（即可通过集成第三方数据访问框架，也可以使用Spring自身的JDBC访问模板）
* 。。。

#### IoC是什么?
Ioc—Inversion of Control，即“控制反转”。
IoC不是一种技术，只是一种思想，一个重要的面向对象编程的法则。

改变如下：
传统的方式（我们直接在对象内部通过new进行创建对象，是程序主动去创建依赖对象）
==》变成
IoC方式（由IoC容器来创建及注入依赖对象）

这里

* “控制”指IoC容器控制了对象，控制了它对外部资源获取（不只是对象包括比如文件等）
* “反转”指依赖对象的获取被反转了


#### DI是什么?
DI—Dependency Injection，即“依赖注入”：是组件之间依赖关系由容器在运行期决定，形象的说，即由容器动态的将某个依赖关系注入到组件之中。
IoC和DI其实是同一个概念的不同角度描述。
“依赖注入”明确描述了“被注入对象依赖IoC容器配置依赖对象”。

#### IoC容器的功能?
IoC容器负责实例化、定位、配置应用程序中的对象及建立这些对象间的依赖。


#### Spring IoC容器如何知道哪些是它管理的对象呢?
配置文件，Spring IoC容器通过读取配置文件中的配置元数据，通过元数据对应用中的各个对象进行实例化及装配。


#### spring注入的几种方式?

* 构造器注入方式
* setter注入方式
* 静态工厂的方法注入
* 实例工厂的方法注入


#### Spring循环依赖的三种方式
1. 构造器参数循环依赖 ： 抛出BeanCurrentlyInCreationException
2. setter方式单例 ：正常
3. setter方式原型 ：抛出BeanCurrentlyInCreationException

原因：

1. Spring容器会将每一个正在创建的Bean 标识符放在一个“当前创建Bean池”中，Bean标识符在创建过程中将一直保持在这个池中，因此如果在创建Bean过程中发现自己已经在“当前创建Bean池”里时将抛出BeanCurrentlyInCreationException异常表示循环依赖；而对于创建完毕的Bean将从“当前创建Bean池”中清除掉。
2. Spring是先将Bean对象实例化之后再设置对象属性的
3. 对于“prototype”作用域Bean，Spring容器无法完成依赖注入，因为“prototype”作用域的Bean，Spring容器不进行缓存，因此无法提前暴露一个创建中的Bean。

#### Bean的作用域
Spring提供“singleton”和“prototype”两种基本作用域，另外提供“request”、“session”、“global session”三种web作用域；Spring还允许用户定制自己的作用域。

一. 基本的作用域

1. singleton
   “singleton”作用域的Bean只会在每个Spring IoC容器中存在一个实例，而且其完整生命周期完全由Spring容器管理。对于所有获取该Bean的操作Spring容器将只返回同一个Bean。
2. prototype
  即原型，指每次向Spring容器请求获取Bean都返回一个全新的Bean，相对于“singleton”来说就是不缓存Bean，每次都是一个根据Bean定义创建的全新Bean。


二. Web应用中的作用域

1. request
表示每个请求需要容器创建一个全新Bean。
2. session
表示每个会话需要容器创建一个全新Bean
3. globalSession
类似于session作用域，只是其用于portlet环境的web应用。


#### AOP相关概念
1. AOP是什么
面向切面编程，在运行时，动态地将代码切入到类的指定方法、指定位置上的编程思想就是面向切面的编程。
2. 连接点（Jointpoint）
表示需要在程序中插入横切关注点的扩展点，连接点可能是类初始化、方法执行、方法调用、字段调用或处理异常等等，Spring只支持方法执行连接点，在AOP中表示为“在哪里干”
3. 切入点（Pointcut）
表示符合特定条件的“连接点”。在AOP中表示为“在哪里干的集合”
4. 增强（通知，advice）
（植入到目标类的连接点上的）代码，在AOP中表示为“干什么”；
5. 方面/切面（Aspect）
在AOP中表示为“在哪干和干什么集合”；


#### Spring中AOP增强类型
（Spring只支持方法级的增强！）

1. 前置增强
   目标方法执行前实施增强
2. 后置增强
   目标方法执行后实施增强
3. 环绕增强
   目标方法执行的前后实施增强
4. 异常抛出增强
   目标方法抛出异常后实施增强
5. 引介增强
   在目标类中**添加一些方法和属性**

#### AOP中静态切面和动态切面

* 静态切面
  生成代理对象时，就确定了增强是否需要植入到目标类连接点
* 动态切面
  必须到运行期，根据输入的参数的值来确定增强是否需要植入到目标类的连接点


#### AOP中的流程切点
由某个方法直接或间接发起调用的其他方法。
所以，对于流程切面，代理对象在每次调用目标类方法时，都需要判断方法调用堆栈中是否满足流程切点的要求！



#### @AspectJ注解相关

1. JDK 1.5以上版本才支持注解
2. Spring处理@AspectJ注解，需要将asm模块添加到类路径，asm是轻量级字节码处理框架，因为Java的反射不能获取入参名字。


#### 什么是ORM
ORM全称对象关系映射（Object/Relation Mapping），指将Java对象状态自动映射到关系数据库中的数据上，从而提供透明化的持久化支持，即把一种形式转化为另一种形式。







---

## Spring的事务

#### 数据库事务
数据库事务必须满足4个特性：

1. <font color=green>原子性（Atomicity）</font>
表示组成一个事务的多个数据库操作是一个不可分割的原子单元。
2. <font color=green>一致性（Consistency）</font>
事务执行前后，数据库中数据都处于正确状态（A转帐给B，必须保证A的钱一定转给B，一定不会出现A的钱转了但B没收到）
3. <font color=green>隔离性（Isolation）</font>
并发操作数据库时，不同的事务有各自的数据空间，它们的操作不会对彼此产生干扰。
准确的说，不是做到完全无干扰，数据库规定了多种事务隔离级别，不同隔离级别对应不同的干扰程度，隔离级别越高，数据一致性越好，并发性越弱。
4. <font color=green>持久性（Durability）</font>
一旦事务提交成功，事务中所有的数据操作都必须被持久化到数据库，即使提交事务后，数据库立即崩溃，数据库重启后，也一定有某种机制恢复数据。


一致性是<font color=red>最终目标</font>，其他特性都是为它服务的。

<font color=blue>重执行日志：</font>
数据库一般采用重执行日志保证原子性、一致性、持久性，重执行日志记录了数据库变化的每个操作，数据库在一个事务中执行一部分操作后发生错误退出，数据库可以根据重执行日志撤销已经执行的操作。
如果提交事务后，数据库崩溃了，也可以根据重执行日志对尚未持久化的数据进行重执行。

数据库使用<font color=blue>数据库锁</font>来保证事务的隔离性。


#### 数据并发问题

<font color=brown>脏读（dirty read）</font>
A事务读取B事务尚未提交的更改数据，并在此数据的基础上操作。如果B的事务回滚，那么A读到的数据是不被承认的。
<img src="http://img.blog.csdn.net/20171109091431581">

<font color=brown>不可重复读（unrepeatable read）</font>
在同一事务中，多次读取同一数据却返回不同的结果；也就是有其他事务更改了这些数据
<img src="http://img.blog.csdn.net/20171109091442313">


<font color=brown>幻想读（phantom read）</font>
A事务读取到B事务提交的新增数据，如果新增数据刚好满足事务的查询条件，这个新数据将进入事务的视野。
<img src="http://img.blog.csdn.net/20171109091514244">


<font color=red>容易混淆：</font>幻想读和不可重复读
**幻想读**指读到了其他已经提交的事务的新增数据，**不可重复读**指读到了已经提交事务的更改数据（更改或删除）。
为了避免这2种情况，采取的对策是不一样的：
为了防止读到更新数据，只需要对操作的数据添加行级锁，而为了防止读到新增数据，需要添加表级锁。

<font color=brown>第一类丢失更新</font>
如下图所示，A事务撤销导致已提交的B事务的更新数据丢失了：
<img src="http://img.blog.csdn.net/20171109092653387">


<font color=brown>第二类丢失更新</font>
如下图所示，A事务提交导致已提交的B事务的更新数据丢失了：
<img src="http://img.blog.csdn.net/20171109092656566">



#### 数据库锁机制

按锁定的对象不同，一般分为<font color=pink>**表锁定**</font>和<font color=pink>**行锁定**</font>。

按并发事务锁定的关系来看，分为<font color=pink>**共享锁定**</font>和<font color=pink>**独占锁定**</font>。


#### 事务隔离级别
如果要用户直接操作数据库锁，是很麻烦的，所以数据库提供了自动锁机制。只有用户指定了事务隔离级别，然后数据库会分析事务中的SQL语句，自动为事务中的数据资源加上合适的锁。

SQL标准中定义了四种事务隔离级别，不同的事务隔离级别能解决的数据并发问题的能力是不同的，如下图：
<img src="http://img.blog.csdn.net/20171109095252674">

 * 未提交读（Read Uncommitted）：最低隔离级别，一个事务能读取到别的事务未提交的更新数据，很不安全，可能出现丢失更新、脏读、不可重复读、幻读；
 * 提交读（Read Committed）：一个事务能读取到别的事务提交的更新数据，不能看到未提交的更新数据，不可能可能出现丢失更新、脏读，但可能出现不可重复读、幻读；
 * 可重复读（Repeatable Read）：保证同一事务中先后执行的多次查询将返回同一结果，不受其他事务影响，可能可能出现丢失更新、脏读、不可重复读，但可能出现幻读；
 * 序列化（Serializable）：最高隔离级别，不允许事务并发执行，而必须串行化执行，最安全，不可能出现更新、脏读、不可重复读、幻读。



#### 事务类型分类

1. 数据库事务类型有本地事务和分布式事务：
 * 本地事务：就是普通事务，能保证单台数据库上的操作的ACID，被限定在一台数据库上；
 * 分布式事务：涉及两个或多个数据库源的事务，即跨越多台同类或异类数据库的事务（由每台数据库的本地事务组成的），分布式事务旨在保证这些本地事务的所有操作的ACID，使事务可以跨越多台数据库；

2. Java事务类型有JDBC事务和JTA事务：
 * JDBC事务：就是数据库事务类型中的本地事务，通过Connection对象的控制来管理事务；
 * JTA事务：JTA指Java事务API(Java Transaction API)，是Java EE数据库事务规范， JTA只提供了事务管理接口，由应用程序服务器厂商（如WebSphere Application Server）提供实现，JTA事务比JDBC更强大，支持分布式事务。

3. 按是否通过编程实现事务有声明式事务和编程式事务：
 * 声明式事务： 通过注解或XML配置文件指定事务信息；
 * 编程式事务：通过编写代码实现事务。

---


## Spring的事务管理

#### 事务管理关键抽象
在Spring事务管理高层抽象层主要包括3个接口：

 * PlatformTransactionManager（根据TransactionDefinition提供的事务属性配置信息创建事务）
 * TransactionDefinition（描述事务的隔离级别、超时时间、是否只读事务、事务传播规则等控制事务具体行为的事务属性，这些属性可以通过XML或者注解提供）
 * TransactionStatus（代表一个事务的具体运行状态）

它们的关系如下图：
<img src="http://img.blog.csdn.net/20171109104220894">

下面给出这3个接口的定义：

<font color=green>PlatformTransactionManager</font>：
```
public interface PlatformTransactionManager {  
    TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException;  
    void commit(TransactionStatus status) throws TransactionException;  
    void rollback(TransactionStatus status) throws TransactionException;  
}  
```
 * getTransaction()：返回一个已经激活的事务或创建一个新的事务（根据给定的TransactionDefinition类型参数定义的事务属性），返回的是TransactionStatus对象代表了当前事务的状态，其中该方法抛出TransactionException（未检查异常）表示事务由于某种原因失败。
 * commit()：用于提交TransactionStatus参数代表的事务
 * rollback：用于回滚TransactionStatus参数代表的事务




<font color=green>TransactionDefinition</font>：
```
public interface TransactionDefinition {  
    int getPropagationBehavior();  
    int getIsolationLevel();  
    int getTimeout();  
    boolean isReadOnly();  
    String getName();  
}  
```
 * getPropagationBehavior()：返回定义的事务传播行为；
 * getIsolationLevel()：返回定义的事务隔离级别；
 * getTimeout()：返回定义的事务超时时间；
 * isReadOnly()：返回定义的事务是否是只读的；
 * getName()：返回定义的事务名字。
 
TransactionDefinition定义了以下的事务属性：

 * 事务隔离：定义了和java.sql.Connection接口同名的4个隔离级别 ISOLATION_READ_UNCOMMITED、ISOLATION_READ_COMMITED、ISOLATION_REPEATABLE_READ、ISOLATION_SERIALIZABLE，还有一个ISOLATION_DEFAULT表示使用底层数据库默认的隔离级别。
 * 事务传播：后面单独将。
 * 事务超时：事务在超时前能运行多久，超时时间后，事务被回滚



<font color=green>TransactionStatus</font>：
```
public interface TransactionStatus extends SavepointManager {  
    boolean isNewTransaction();  
    boolean hasSavepoint();  
    void setRollbackOnly();  
    boolean isRollbackOnly();  
    void flush();  
    boolean isCompleted();  
}  
```
 *  isNewTransaction()：返回当前事务状态是否是新事务； 
 *  hasSavepoint()：返回当前事务是否有保存点；
 *  setRollbackOnly()：设置当前事务应该回滚；
 *  isRollbackOnly(()：返回当前事务是否应该回滚；
 *  flush()：用于刷新底层会话中的修改到数据库，一般用于刷新如Hibernate/JPA的会话，可能对如JDBC类型的事务无任何影响；
 *  isCompleted():当前事务否已经完成。



#### 事务同步管理器
由于JDBC的Connection、Hibernate的Session等资源是非线程安全的，为了Dao、Service类能做到singleton，Spring的事务同步管理器类 TransactionSynchronizationManager：
使用 **ThreadLocal**管理为不同事务线程提供独立的资源副本，同时维护事务配置的属性和运行状态信息。

不管编程式事务管理，还是声明式事务管理，都离不开事务同步管理器。


#### 事务传播行为
Spring通过事务传播行为控制当前的事务如何传播到被嵌套调用的目标服务接口方法中。

Spring定义了七种传播行为：

 1. PROPAGATION_REQUIRED：表示当前方法必须运行在事务中。如果当前事务存在，方法将会在该事务中运行。否则，会启动一个新的事务
 2. PROPAGATION_SUPPORTS：表示当前方法不需要事务上下文，但是如果存在当前事务的话，那么该方法会在这个事务中运行
 3. PROPAGATION_MANDATORY：表示该方法必须在事务中运行，如果当前事务不存在，则会抛出一个异常
 4. PROPAGATION_REQUIRED_NEW：表示当前方法必须运行在它自己的事务中。一个新的事务将被启动。如果存在当前事务，在该方法执行期间，当前事务会被挂起。如果使用JTATransactionManager的话，则需要访问TransactionManager
 5. PROPAGATION_NOT_SUPPORTED：表示该方法不应该运行在事务中。如果存在当前事务，在该方法运行期间，当前事务将被挂起。
 6. PROPAGATION_NEVER：表示当前方法不应该运行在事务上下文中。如果当前正有一个事务在运行，则会抛出异常
 7. PROPAGATION_NESTED ： 表示如果当前已经存在一个事务，那么该方法将会在嵌套事务中运行。嵌套的事务可以独立于当前事务进行单独地提交或回滚。如果当前事务不存在，那么其行为与PROPAGATION_REQUIRED一样。


#### 编程式事务管理

Spring提供两种编程式事务支持：直接使用PlatformTransactionManager实现和使用TransactionTemplate模板类，用于支持逻辑事务管理。
如果采用编程式事务推荐使用TransactionTemplate模板类和高级别解决方案。

1. 使用PlatformTransactionManager
  ```
  // 1、数据源定义

  // 2、事务管理器定义
  <bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">    
      <property name="dataSource" ref="dataSource"/>  
  </bean>  
    
  // 3.1 高级别方案(JdbcTemplate)来进行事务管理
  DefaultTransactionDefinition def = new DefaultTransactionDefinition();  
  def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);  
  def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);  
  TransactionStatus status = txManager.getTransaction(def);  
  jdbcTemplate.execute(CREATE_TABLE_SQL);  
  try {  
      jdbcTemplate.update(INSERT_SQL, "test");  
      txManager.commit(status);  
  } catch (RuntimeException e) {  
      txManager.rollback(status);  
  }
  
  // 3.2 低级别方案（面向JDBC编程）
  DefaultTransactionDefinition def = new DefaultTransactionDefinition(); 
  def.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
  def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
  TransactionStatus status = txManager.getTransaction(def);
  Connection conn = DataSourceUtils.getConnection(dataSource);
  try {  
      conn.prepareStatement(CREATE_TABLE_SQL).execute();  
      PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL);  
      pstmt.setString(1, "test");  
      pstmt.execute();  
      conn.prepareStatement(DROP_TABLE_SQL).execute();  
      txManager.commit(status);  
  } catch (Exception e) {  
      status.setRollbackOnly();  
      txManager.rollback(status);  
  } finally {  
      DataSourceUtils.releaseConnection(conn, dataSource);  
  }  
  ```
  到此事务管理还是很繁琐：必须手工提交或回滚事务。Spring提供了更好的解决方案（TransactionTemplate模板类来简化事务管理）。
 
2. 使用TransactionTemplate
事务管理由模板类定义，而具体操作需要通过TransactionCallback回调接口或TransactionCallbackWithoutResult回调接口指定，通过调用模板类的参数类型为TransactionCallback或TransactionCallbackWithoutResult的execute方法来自动享受事务管理。
  ```
  jdbcTemplate.execute(CREATE_TABLE_SQL);  
  TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);  
  transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);  
  transactionTemplate.execute(new TransactionCallbackWithoutResult() {  
      @Override  
      protected void doInTransactionWithoutResult(TransactionStatus status) {  
        jdbcTemplate.update(INSERT_SQL, "test");  
  }});  
  jdbcTemplate.execute(DROP_TABLE_SQL);  
  ```
  


#### XML配置声明式事务
Spring声明式事务是通过AOP实现的。


（1） 使用原始的 TransactionProxyFactoryBean
  
  ```
  <bean id="bbtForum"
    	class="org.springframework.transaction.interceptor.TransactionProxyFactoryBean">
		<property name="transactionManager" ref="txManager" />
		<property name="target">
			<bean id="bbtForumTarget"
				class="com.baobaotao.service.impl.BbtForumImpl">
				<property name="forumDao" ref="forumDao" />
				<property name="topicDao" ref="topicDao" />
				<property name="postDao" ref="postDao" />
			</bean>
		</property>
		<property name="transactionAttributes">
			<props>
				<prop key="get*">PROPAGATION_REQUIRED,readOnly</prop>
				<prop key="*">PROPAGATION_REQUIRED,-tion</prop>
			</props>
		</property>
	</bean>  
  ```
使用TransactionProxyFactoryBean有以下缺点：

 * 需要对每个需要事务支持的业务类进行单独配置
 * 在为业务类Bean添加事务支持时，在容器中既需要定义业务类Bean（XXXTarget），又需要通过TransactionProxyFactoryBean对其进行代理以生成事务的代理Bean。
 
这是由于低版本Spring没有强大的AOP切面描述语言造成的。

（2） 基于 tx/aop 命名空间的配置
  ```
  // 引入 tx命名空间的声明
  xmlns:tx="http://www.springframework.org/schema/tx"
  
  xsi:schemaLocation="
    http://www.springframework.org/schema/beans 
    http://www.springframework.org/schema/beans/spring-beans-3.0.xsd
    
    http://www.springframework.org/schema/tx 
    http://www.springframework.org/schema/tx/spring-tx-3.0.xsd
    http://www.springframework.org/schema/aop 
    http://www.springframework.org/schema/aop/spring-aop-3.0.xsd">
  
  
  // 业务类
  <bean id="bbtForum"
    	class="com.baobaotao.service.impl.BbtForumImpl">
		<property name="forumDao" ref="forumDao" />
		<property name="topicDao" ref="topicDao" />
		<property name="postDao" ref="postDao" />
  </bean>
  
  //事务管理器
  <bean id="transactionManager"
		class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
		<property name="dataSource" ref="dataSource" />
  </bean>
  
  
	<aop:config>
        // 定义切面
		<aop:pointcut id="serviceMethod"
			expression="execution(* com.baobaotao.service.*Forum.*(..))" />
		// 引用事务增强
        <aop:advisor pointcut-ref="serviceMethod"
			advice-ref="txAdvice" />
	</aop:config>
    
    // 事务增强
	<tx:advice id="txAdvice" >
        // 事务属性定义
        <tx:attributes> 
            <tx:method name="get*" read-only="false"/>
            <tx:method name="add*" rollback-for="PessimisticLockingFailureException"/>
            <tx:method name="update*"/>         
        </tx:attributes>
    </tx:advice>
  ```
  

#### 注解配置声明式事务
1. 在对应的业务类（或方法上）添加注解 @Transactional。
2. 配置文件：

```
<tx:annotation-driven transaction-manager="txManager"/>

<bean id="bbtForum"
    class="com.baobaotao.service.impl.BbtForumImpl">
    property name="forumDao" ref="forumDao" />
    property name="topicDao" ref="topicDao" />
    property name="postDao" ref="postDao" />
bean>
```

 * 注解驱动`<tx:annotation-driven>`默认会自动使用名称为"transactionManager"的事务管理器。
 * bean “bbtForum”标注了`@Transactional`，会被注解驱动自动植入事务




#### <font color="red">总结（spring如何实现事物管理的）</font>?
Spring通过“编程式事务管理”和“声明式事务管理”2种方式实现。

**编程式事务管理**分为：

 * 直接使用接口PlatformTransactionManager
 * 使用TransactionTemplate 
 
**声明式事务管理**分为：

 * XML配置
 * 注解配置




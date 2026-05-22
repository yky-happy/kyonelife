# Java 注解速查手册 — 本项目用到的全部注解

> 适用范围：kyonelife 博客后端 (Spring Boot 3 + MyBatis-Plus + Sa-Token + Knife4j)
>
> 每个注解给出：**所属包 / 作用域 / 干什么 / 怎么用 / 项目里在哪用过**。

---

## 一、Spring Web —— Controller 层用

### `@RestController`
- **包**：`org.springframework.web.bind.annotation`
- **作用域**：只能放在**类**上
- **作用**：声明这是一个"返回 JSON 的 Controller"。等价于 `@Controller + @ResponseBody`，方法返回的对象会自动序列化成 JSON 写到响应体。
- **用法**：
  ```java
  @RestController
  public class TagController { ... }
  ```
- **项目里**：`AuthController` 上已经用了。

### `@Controller`（本项目几乎不用）
- 区别于 `@RestController` 的地方：方法默认返回的是**视图名**（用于 JSP/Thymeleaf 模板渲染）。本项目是前后端分离的，**不要用这个**，永远用 `@RestController`。

### `@RequestMapping`
- **包**：`org.springframework.web.bind.annotation`
- **作用域**：类 或 方法
- **作用**：定义请求路径前缀。常用在**类上**统一定一个模块前缀。
- **用法**：
  ```java
  @RestController
  @RequestMapping("/admin/tag")    // 类上：定模块前缀
  public class TagController { ... }
  ```
- **项目里**：`AuthController` 上有 `@RequestMapping("/admin/auth")`。

### `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping`
- **包**：`org.springframework.web.bind.annotation`
- **作用域**：只放在**方法**上
- **作用**：分别对应 HTTP 的 GET、POST、PUT、DELETE，是 `@RequestMapping(method=...)` 的语法糖。
- **RESTful 用法约定**：
  | 注解 | 用途 | 例子 |
  |------|------|------|
  | `@GetMapping` | 查询 | `/admin/tag/page` |
  | `@PostMapping` | 新增 | `/admin/tag` |
  | `@PutMapping` | 修改 | `/admin/tag/{id}` |
  | `@DeleteMapping` | 删除 | `/admin/tag/{id}` |
- **用法**：
  ```java
  @GetMapping("/page")                         // 完整路径 = 类上前缀 + "/page"
  public Result<Page<Tag>> page(...) { ... }

  @PostMapping                                 // 不写 value 就是模块根路径
  public Result<Long> save(...) { ... }

  @PutMapping("/{id}")                         // {id} 是路径变量占位符
  public Result<Void> update(@PathVariable Long id, ...) { ... }
  ```
- **项目里**：`AuthController.login` 上的 `@PostMapping("/login")`。

### `@RequestParam`
- **包**：`org.springframework.web.bind.annotation`
- **作用域**：方法参数
- **作用**：把 **URL 查询参数**绑定到方法参数。即 `?key=value` 中的 value。
- **用法**：
  ```java
  // 请求：GET /admin/tag/page?page=1&size=10&keyword=java
  @GetMapping("/page")
  public Result<...> page(
      @RequestParam(defaultValue = "1") Integer page,        // 不传时默认 1
      @RequestParam(defaultValue = "10") Integer size,
      @RequestParam(required = false) String keyword) {      // 允许不传
      ...
  }
  ```

### `@PathVariable`
- **包**：`org.springframework.web.bind.annotation`
- **作用域**：方法参数
- **作用**：把 **URL 路径**中的占位符值绑到方法参数。
- **用法**：
  ```java
  // 请求：DELETE /admin/tag/123
  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Long id) { ... }
  //                                       ↑ 这里的 id 就是 URL 里的 123
  ```

### `@RequestBody`
- **包**：`org.springframework.web.bind.annotation`
- **作用域**：方法参数
- **作用**：把 HTTP 请求体里的 JSON 反序列化成对象。
- **用法**：
  ```java
  @PostMapping
  public Result<Long> save(@Valid @RequestBody TagSaveDTO dto) { ... }
  //                       ↑ 配合 @Valid 触发字段校验
  ```
- **项目里**：`AuthController.login` 用了 `@RequestBody LoginDTO`。

### `@Valid`
- **包**：`jakarta.validation`
- **作用域**：方法参数（**不放在 DTO 类上**）
- **作用**：触发参数对象上 `@NotBlank`、`@Size` 等校验注解。校验失败抛 `MethodArgumentNotValidException`，会被你项目的 `GlobalExceptionHandler` 捕获。
- **用法**：
  ```java
  public Result<Long> save(@Valid @RequestBody TagSaveDTO dto) { ... }
  ```
- **项目里**：`AuthController.login` 已用。

---

## 二、Spring 容器 / 依赖注入

### `@SpringBootApplication`
- **包**：`org.springframework.boot.autoconfigure`
- **作用域**：类（启动类上）
- **作用**：三合一注解（`@Configuration + @EnableAutoConfiguration + @ComponentScan`）。Spring Boot 启动入口必备。
- **项目里**：`BlogApplication` 类上。

### `@Configuration`
- **包**：`org.springframework.context.annotation`
- **作用域**：类
- **作用**：标记这是一个"配置类"，类里可以用 `@Bean` 声明 Spring 管理的对象。
- **项目里**：`Knife4jConfig`、`SaTokenConfig`、`MyBatisPlusHandler`、`CorsConfig`。

### `@Bean`
- **包**：`org.springframework.context.annotation`
- **作用域**：方法（必须在 `@Configuration` 类里）
- **作用**：把方法返回值注册为 Spring 容器里的 Bean，其他地方能 `@Autowired` 注入。
- **用法**：
  ```java
  @Configuration
  public class Knife4jConfig {
      @Bean
      public OpenAPI openAPI() {
          return new OpenAPI().info(...);
      }
  }
  ```
- **项目里**：`Knife4jConfig.openAPI()` 方法上。

### `@Component`
- **包**：`org.springframework.stereotype`
- **作用域**：类
- **作用**：通用的"让 Spring 管理这个类"注解。`@Service`、`@Repository`、`@Controller` 都是它的特化版本。
- **项目里**：`MyBatisPlusHandler` 上。
- **常用变体**：

  | 注解 | 用途 |
  |------|------|
  | `@Service` | 业务层（Service 实现类）|
  | `@Repository` | DAO 层（**MyBatis-Plus 用 `@Mapper` 替代**）|
  | `@Controller` | MVC 控制器（本项目用 `@RestController`）|
  | `@Component` | 都不沾边的通用组件 |

  这四个**对 Spring 来说功能完全一样**，区别只是语义（让人一看就知道是什么层）。

### `@Service`
- **包**：`org.springframework.stereotype`
- **作用域**：类
- **作用**：标记业务层的实现类。
- **项目里**：`AdminServiceImpl` 上。**`TagServiceImpl` 也要加这个**。

### `@Autowired` （本项目极少用）
- **包**：`org.springframework.beans.factory.annotation`
- **作用**：字段/构造器/setter 上注入依赖。
- **本项目不用的原因**：用 Lombok 的 `@RequiredArgsConstructor` 代替（详见下文）。

---

## 三、Lombok —— 干掉样板代码

### `@Data`
- **包**：`lombok`
- **作用域**：类
- **作用**：自动生成 **getter / setter / toString / equals / hashCode**。等于五个注解的合集。
- **用法**：
  ```java
  @Data
  public class TagSaveDTO {
      private String name;
      // 自动有了 getName() / setName(String) / toString() 等方法
  }
  ```
- **项目里**：几乎所有 Entity 和 DTO 上。

### `@RequiredArgsConstructor`
- **包**：`lombok`
- **作用域**：类
- **作用**：给所有 **`final` 字段**和 **`@NonNull` 字段**生成一个构造器。**配合 Spring 构造器注入，是最佳实践**。
- **用法**：
  ```java
  @RestController
  @RequiredArgsConstructor
  public class TagController {
      private final TagService tagService;
      // ↑ 不需要 @Autowired！Lombok 自动生成构造函数，Spring 自动注入。
  }
  ```
- **项目里**：`AuthController`、`AdminServiceImpl` 上。**TagController/Service 都用这个套路**。

### `@NoArgsConstructor` / `@AllArgsConstructor`
- 分别生成"无参构造器"和"全参构造器"。本项目里 `@Data` 默认就生成了无参构造器，单独用得少。

### `@EqualsAndHashCode(callSuper = true)`
- **包**：`lombok`
- **作用域**：类
- **作用**：让 `equals/hashCode` 包含父类字段（默认只看子类自己的字段）。
- **何时用**：子类**继承**了别的类时。
- **项目里**：`Tag extends BaseEntity` 等所有继承 BaseEntity 的实体上。

### `@Builder`
- **包**：`lombok`
- **作用**：生成"链式构造器"，能写 `Tag.builder().name("Java").color("#1890ff").build()`。
- **用法**：DTO/VO 上比较少用，Entity 上也少见。**不强求**。

### `@Slf4j`
- **包**：`lombok.extern.slf4j`
- **作用域**：类
- **作用**：自动生成一个 `log` 静态字段，可以直接 `log.info(...)`、`log.error(...)`。
- **用法**：
  ```java
  @Slf4j
  @RestController
  public class TagController {
      public ... save(...) {
          log.info("新增标签：{}", dto);
          ...
      }
  }
  ```
- **项目里**：`GlobalExceptionHandler` 上用了。

---

## 四、Jakarta Validation —— 参数校验

> 这些注解放在 **DTO 字段上**，配合 Controller 方法参数上的 `@Valid` 触发校验。

### `@NotNull`
- **包**：`jakarta.validation.constraints`
- **作用**：字段不能是 null（**空字符串 "" 能通过**）。
- **何时用**：非字符串字段，比如 `Integer age`、`Long id`。

### `@NotBlank` ⭐
- **作用**：字符串不能是 null、不能是 ""、不能是纯空白字符串（"   "）。
- **何时用**：**所有 String 字段必填校验都用这个**，比 `@NotNull` 更严格。
- **项目里**：`LoginDTO` 字段上、`TagSaveDTO.name` 上。

### `@NotEmpty`
- **作用**：字符串/集合/数组不能是 null、不能是空（长度 0）。但空白字符串 "   " 能通过。
- **何时用**：集合或数组字段。字符串场景仍然首选 `@NotBlank`。

### `@Size`
- **作用**：限定字符串长度或集合大小的上下限。
- **用法**：
  ```java
  @Size(min = 2, max = 20, message = "长度必须在 2-20 字符之间")
  private String name;
  ```

### `@Min` / `@Max`
- **作用**：数字下限/上限。
- **用法**：
  ```java
  @Min(value = 1, message = "页码不能小于 1")
  private Integer page;
  ```

### `@Email`
- **作用**：必须是合法邮箱格式。

### `@Pattern`
- **作用**：必须匹配正则。
- **用法**：
  ```java
  @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
  private String phone;
  ```

---

## 五、MyBatis-Plus —— 实体 → 数据库映射

### `@TableName`
- **包**：`com.baomidou.mybatisplus.annotation`
- **作用域**：实体类
- **作用**：指定实体对应的数据库表名。如果实体类名和表名能按约定对应（如 `Tag` ↔ `tag`），其实可以省略。
- **用法**：
  ```java
  @TableName("tag")
  public class Tag extends BaseEntity { ... }
  ```

### `@TableId`
- **包**：同上
- **作用域**：实体字段
- **作用**：标记主键字段，配置主键生成策略。
- **用法**：
  ```java
  @TableId(type = IdType.AUTO)         // AUTO = 数据库自增
  private Long id;
  ```
- **常见的 `IdType`**：
  | 值 | 含义 |
  |---|------|
  | `AUTO` | 数据库自增（MySQL 用这个）|
  | `ASSIGN_ID` | 雪花算法生成（默认）|
  | `ASSIGN_UUID` | UUID |
  | `INPUT` | 手动设值 |

### `@TableField`
- **作用域**：实体字段
- **作用**：自定义字段属性。
- **常用场景**：
  ```java
  @TableField(fill = FieldFill.INSERT)            // 插入时自动填充
  private LocalDateTime createTime;

  @TableField(fill = FieldFill.INSERT_UPDATE)     // 插入和更新时都自动填充
  private LocalDateTime updateTime;

  @TableField(exist = false)                      // 这个字段数据库里没有，只是 Java 类需要
  private String tempField;

  @TableField("nick_name")                        // 显式指定数据库字段名（默认下划线转驼峰）
  private String nickname;
  ```
- **项目里**：`BaseEntity` 的 `createTime`、`updateTime` 上。

### `@TableLogic`（你 application.yml 已全局配置，不用注解）
- 标记逻辑删除字段。你的 `application.yml` 里全局配了 `logic-delete-field: deleted`，所以**实体里的 deleted 字段不用加这个注解**，MyBatis-Plus 自动识别。

### `@Mapper`
- **包**：`org.apache.ibatis.annotations`
- **作用域**：接口
- **作用**：标记这是一个 Mapper 接口，Spring 会自动给它生成实现类。
- **用法**：
  ```java
  @Mapper
  public interface TagMapper extends BaseMapper<Tag> { }
  ```
- **替代方案**：你启动类上如果有 `@MapperScan("com.yky.blog.common.mapper")`，就可以不在每个 Mapper 上写 `@Mapper`。

---

## 六、Knife4j / Swagger OpenAPI —— 接口文档

### `@Tag`
- **包**：`io.swagger.v3.oas.annotations.tags`（**不是 lombok 的 @Tag**！）
- **作用域**：Controller 类
- **作用**：在 Knife4j 文档左侧目录里给这个 Controller 分组命名。
- **用法**：
  ```java
  @Tag(name = "标签管理")
  @RestController
  @RequestMapping("/admin/tag")
  public class TagController { ... }
  ```
- ⚠️ 容易踩坑：和 MyBatis-Plus 的 `@TableName` 没关系，也跟 Lombok 没关系。如果发现 IDE 自动 import 错了包，就是这里的锅。

### `@Operation`
- **包**：`io.swagger.v3.oas.annotations`
- **作用域**：方法
- **作用**：描述这个接口干什么。
- **用法**：
  ```java
  @Operation(summary = "分页查询标签")
  @GetMapping("/page")
  public Result<...> page(...) { ... }
  ```

### `@Schema`
- **包**：`io.swagger.v3.oas.annotations.media`
- **作用域**：类 / 字段
- **作用**：描述 DTO/VO 是什么、字段是什么、是否必填、示例值。
- **用法**：
  ```java
  @Schema(description = "标签新增/编辑入参")
  public class TagSaveDTO {

      @Schema(description = "标签名", example = "Java", requiredMode = Schema.RequiredMode.REQUIRED)
      private String name;
  }
  ```

### `@Parameter`
- **作用域**：方法参数
- **作用**：单独描述某个 `@RequestParam` 或 `@PathVariable` 参数。
- **用法**：
  ```java
  public Result<...> delete(
      @Parameter(description = "标签 ID") @PathVariable Long id) { ... }
  ```

---

## 七、Sa-Token —— 权限校验

### `@SaCheckLogin`
- **包**：`cn.dev33.satoken.annotation`
- **作用域**：方法 或 类
- **作用**：要求请求必须带有效 token，否则抛 `NotLoginException`（401）。
- **用法**：
  ```java
  @SaCheckLogin
  @GetMapping("/page")
  public Result<...> page(...) { ... }
  ```
- **建议**：放类上一次性给整个 Controller 加上，比每个方法重复写干净。

### `@SaCheckRole(value = "admin")`
- **作用**：要求当前登录用户拥有指定角色。

### `@SaCheckPermission(value = "article:delete")`
- **作用**：要求当前用户拥有指定权限标识。配合 RBAC 权限系统使用。

---

## 八、异常处理 —— 全局兜底

### `@RestControllerAdvice`
- **包**：`org.springframework.web.bind.annotation`
- **作用域**：类
- **作用**：定义全局异常处理类（=`@ControllerAdvice + @ResponseBody`，返回 JSON）。
- **项目里**：`GlobalExceptionHandler` 上。

### `@ExceptionHandler(XxxException.class)`
- **作用域**：方法（必须在 `@RestControllerAdvice` 类里）
- **作用**：捕获指定类型的异常，统一处理。
- **用法**：
  ```java
  @RestControllerAdvice
  @Slf4j
  public class GlobalExceptionHandler {

      @ExceptionHandler(BizException.class)
      public Result<Void> handleBizException(BizException e) {
          return Result.failure(e.getCode(), e.getMessage());
      }
  }
  ```
- **项目里**：`GlobalExceptionHandler` 内已经用了。

### `@Override`
- **包**：`java.lang`
- **作用域**：方法
- **作用**：声明这个方法重写了父类/接口的方法。**不强制**，但加上之后如果父类没这个方法，IDE 会报错（防写错方法名）。

---

## 九、备查：本项目暂未用到但常见的

| 注解 | 用途 | 包 |
|------|------|---|
| `@Transactional` | 方法/类加事务（必须在 Service 层）| `org.springframework.transaction.annotation` |
| `@Async` | 方法异步执行（配合 `@EnableAsync`）| `org.springframework.scheduling.annotation` |
| `@Scheduled` | 定时任务（配合 `@EnableScheduling`）| 同上 |
| `@Cacheable` / `@CachePut` / `@CacheEvict` | 方法级缓存 | `org.springframework.cache.annotation` |
| `@Value("${xxx}")` | 从 `application.yml` 注入配置值 | `org.springframework.beans.factory.annotation` |
| `@ConfigurationProperties(prefix="xxx")` | 一次性注入一组配置到对象 | `org.springframework.boot.context.properties` |

阅读量统计需要 `@Scheduled` + `@Async`，事务相关（文章新增多标签）需要 `@Transactional`，写到那一步再来查这份文档即可。

---

## 十、注解书写顺序建议（提高代码可读性）

类上的注解多了之后，按这个顺序排比较清爽：

```java
// 1. Swagger 文档
@Tag(name = "标签管理")
// 2. Spring Web
@RestController
@RequestMapping("/admin/tag")
// 3. Sa-Token 权限（可选）
@SaCheckLogin
// 4. Lombok
@RequiredArgsConstructor
@Slf4j
public class TagController { ... }
```

字段上：
```java
// 1. Swagger
@Schema(description = "标签名", example = "Java")
// 2. 校验
@NotBlank(message = "标签名不能为空")
@Size(max = 20)
// 3. MyBatis-Plus（仅 Entity 上）
@TableField("name")
private String name;
```

---

## 十一、常见错误对照表

| 报错关键字 | 多半是 |
|-----------|--------|
| `No qualifying bean of type` | 缺 `@Service` / `@Component` / `@Bean` |
| `Field 'xxx' annotated with @NotBlank` | DTO 字段校验失败（看 message） |
| `Required request body is missing` | 方法少 `@RequestBody` 或前端没传 JSON |
| `Required URI template variable 'id'` | 方法少 `@PathVariable` 或 URL 不匹配 |
| `Cannot resolve symbol 'getName'` | DTO 上少了 `@Data` |
| `No primary or single unique constructor` | DTO 没有无参构造（加 `@Data`/`@NoArgsConstructor`）|
| `Unknown column 'xxx' in 'field list'` | 实体字段对不上数据库列名，加 `@TableField("...")` 或检查命名 |
| `org.swagger 包找不到` | Import 错了 `@Tag`（要 swagger 的，不是 lombok 的）|

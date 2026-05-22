# 概念讲透：DTO / VO / Entity 分层 + TagSaveDTO 模板

> 时间：2026-05-22
> 背景：阶段 3 标签 CRUD 开干前的"地基概念"。搞透后续每个模块都不会困惑"这个字段该放哪个类"。

---

# 一、概念讲透：DTO / VO / Entity

## 1. 三者各自代表什么

| 名字 | 全称 | 中文 | 服务于 | 长什么样 |
|------|------|------|--------|---------|
| **Entity** | Entity（实体） | 数据库表对应的对象 | **数据库** | 字段和表完全一一对应 |
| **DTO** | Data Transfer Object | 数据传输对象 | **接收前端入参** | 只放前端**该传**的字段 |
| **VO** | View Object | 视图对象 | **返回前端出参** | 只放前端**该看到**的字段 |

**记忆口诀**：DTO 进，VO 出，Entity 守在数据库门口。

## 2. 用项目里的 `Tag` 来举具体例子

### 现在的 `Tag` 实体长这样

```java
public class Tag extends BaseEntity {
    private Long id;
    private String name;
    private String color;
    // 从 BaseEntity 继承的：
    // private LocalDateTime createTime;
    // private LocalDateTime updateTime;
}
```

### 三个场景，分别该用什么

**场景 A：前端新增标签时发给后端的数据**

前端只会发：
```json
{ "name": "Java", "color": "#1890ff" }
```

如果直接拿 `Tag` 实体接收：
- 用户可以伪造 `id`、`createTime` 字段塞进来，造成"用户自己指定 ID"的安全风险
- `createTime` 应该由后端 `MyBatisPlusHandler` 自动填，前端传了反而冲突
- 字段太多，IDE 自动补全混乱

**正确做法**：定义一个 `TagSaveDTO`，**只放真正需要前端传的字段**：
```java
public class TagSaveDTO {
    private String name;
    private String color;
}
```

---

**场景 B：后端从数据库查出来后返回给前端的列表**

数据库里的 `Tag` 字段有 `id / name / color / createTime / updateTime / deleted`（如果有逻辑删除字段）。前端要看到的：
- ✅ `id`（要用来发 PUT/DELETE 请求）
- ✅ `name`、`color`、`createTime`
- ❌ `updateTime`（前端不关心）
- ❌ `deleted`（这是后端内部用的逻辑删除标记，绝不能暴露给前端）

**严格的做法**：定义 `TagVO`，只放前端要看到的字段：
```java
public class TagVO {
    private Long id;
    private String name;
    private String color;
    private LocalDateTime createTime;
}
```

**实际项目里的简化**：标签这种**没有敏感字段**的简单场景，很多人直接返回 `Tag` 实体（懒得多建一个类）。这是工程上的"实用主义偏离"——能接受，但要清楚自己是在偷懒。

什么时候**必须**用 VO？
- 字段含敏感信息（密码、加密 token、内部状态码）
- 需要前端展示**关联表**的数据（比如"文章列表"要带上"作者昵称、标签名"，单个 Article 实体里只有 `authorId` 和标签 ID 列表，得拼数据→ 必须用 ArticleVO 多塞字段）

---

**场景 C：从数据库查出来 / 写到数据库去**

直接用 `Tag` 实体。这是它存在的唯一职责。

---

## 3. 整体数据流（这个图要记住）

```
浏览器（前端表单）
     │
     │  JSON: { name: "Java", color: "#1890ff" }
     ▼
┌─────────────────────────────────────────────────┐
│ TagController                                    │
│   接收 → TagSaveDTO  ← 注解 @Valid 自动校验      │
└─────────────────────────────────────────────────┘
     │
     │  调 service.saveTag(dto)
     ▼
┌─────────────────────────────────────────────────┐
│ TagServiceImpl                                   │
│   DTO → Entity (BeanUtil.copyProperties)         │
│   Tag tag = new Tag();                           │
│   tag.setName(dto.getName());                    │
│   tag.setColor(dto.getColor());                  │
│   save(tag)  ← MyBatis-Plus 写库                 │
└─────────────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────────────┐
│ TagMapper (MyBatis-Plus 自动实现)                │
│   INSERT INTO tag(name, color, create_time...)   │
└─────────────────────────────────────────────────┘
     │
     ▼
   MySQL


查询路径反过来：
   MySQL
     │
     ▼  SELECT * FROM tag
   Tag Entity
     │
     ▼  (有需要才转成 TagVO，否则直接返回)
   JSON → 前端
```

## 4. 为什么不能"一个类走天下"

新手最常想：**"反正都是这几个字段，为啥不直接用 Tag 接收前端入参 + 返回前端？"**

四个真实理由（按重要性排序）：

### 理由 1：安全（最重要）
直接用 Entity 接收前端入参，等于让前端**有机会修改任何字段**。比如 `User` 实体有 `isAdmin` 字段，黑客在请求里加一个 `"isAdmin": true`，Spring 自动给你绑定上，下一秒他就是管理员了。这个攻击叫 **Mass Assignment**，是 OWASP 排名前列的高危漏洞。**DTO 是天然的字段白名单。**

### 理由 2：解耦
数据库表 `tag` 加了个 `sort` 排序字段，但前端接口的入参不需要变。如果用 Entity，所有调这个接口的客户端都得跟着升级。用 DTO 的话，加字段只改实体，DTO 不动，前端无感知。

### 理由 3：校验逻辑该长在 DTO 上
```java
public class TagSaveDTO {
    @NotBlank(message = "标签名不能为空")
    @Size(max = 20, message = "标签名最多 20 字")
    private String name;
    ...
}
```
这些校验注解放在 Entity 上不合适——保存 Entity 时再校验已经晚了（业务逻辑可能已经修改过字段），而且每次查询的 Entity 都自动校验也浪费性能。

### 理由 4：阅读体验
一个新人接手代码，看到 `saveTag(TagSaveDTO dto)`，**立刻就知道**前端会传哪些字段。如果是 `saveTag(Tag tag)`，他得猜：所有字段都能传吗？哪些是必填？得到处翻代码确认。

---

## 5. 后续会遇到的命名约定

| 后缀 | 用途 |
|------|------|
| `XxxSaveDTO` / `XxxAddDTO` | 新增专用 |
| `XxxUpdateDTO` | 编辑专用（往往要带 id） |
| `XxxSaveDTO` 一个类两用 | 简单场景里新增/编辑共用，id 放 path 参数里 |
| `XxxQueryDTO` / `XxxPageDTO` | 查询/分页条件 |
| `XxxVO` | 返回单个对象 |
| `XxxListVO` / `XxxPageVO` | 返回列表/分页结果 |

不要被命名细节卡住——**没有统一标准**，团队内一致即可。这个项目你说了算。

---

# 二、模板示范：`TagSaveDTO.java`

下面这份代码详细注释了每一行**为什么这么写**，照着写其他 DTO 时就有了参考模板。

**放置路径**：`blog/src/main/java/com/yky/blog/admin/dto/TagSaveDTO.java`

```java
package com.yky.blog.admin.dto;
// ↑ 包路径：admin 模块的 dto 子包。
//   admin/ 表示这是给后台管理端用的（区别于将来给前台用的 api/），
//   dto/ 表示这是入参对象。

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
// ↑ 四个 import：
//   Schema      —— Knife4j 接口文档展示用
//   NotBlank    —— 校验"字符串非空"
//   Size        —— 校验字符串长度上下限
//   Data        —— Lombok 一键生成 getter/setter/toString/equals

@Data
// ↑ Lombok 注解。编译后会自动给所有字段生成 getter/setter/toString/equals/hashCode。
//   不加的话你得手写一堆 public String getName() { return name; } 这种样板代码。

@Schema(description = "标签新增/编辑入参")
// ↑ 给 Knife4j 用的描述。这个类会出现在文档的"实体列表"里，
//   description 决定文档上怎么标注这个对象。

public class TagSaveDTO {

    @Schema(description = "标签名", example = "Java", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标签名不能为空")
    @Size(max = 20, message = "标签名最多 20 个字符")
    private String name;
    // ↑ 三个注解三种作用：
    //   @Schema   —— 文档展示：字段是什么意思、示例值是啥、是否必填
    //   @NotBlank —— 校验：字符串不能是 null、不能是 ""、不能是纯空白
    //                注意区别：@NotNull 只校验非空，@NotBlank 包含 trim 后非空
    //   @Size     —— 校验：长度范围（这里只限上限 20）

    @Schema(description = "标签颜色（HEX）", example = "#1890ff")
    @Size(max = 20, message = "颜色值最多 20 个字符")
    private String color;
    // ↑ 这个字段没加 @NotBlank，意味着"允许不传"。
    //   如果前端不传，后端可以在 Service 里给个默认色（比如 "#1890ff"）。
}
```

## 关键点回顾

| 行为 | 原因 |
|------|------|
| 类名带 `SaveDTO` 后缀 | 一眼能看出"这是新增/编辑用的入参" |
| 没有 `id` 字段 | 新增时不需要，编辑时 id 走 URL path（`PUT /admin/tag/{id}`），不放在 body 里 |
| 没有 `createTime` / `updateTime` | 这些应该由后端自动填，前端永远不传 |
| 用 `@NotBlank` 而不是 `@NotNull` | 字符串场景永远用 NotBlank（避免空字符串通过校验）|
| `@Valid` **不在 DTO 上**，而在 Controller 方法参数上 | 校验是"调用时触发"，不是"类本身的属性" |

---

# 三、独立练习：`TagPageDTO.java`

照这个模板，自己写 `TagPageDTO.java`（分页查询入参），它应该长这样的雏形：

```java
@Data
@Schema(description = "标签分页查询入参")
public class TagPageDTO {
    @Schema(description = "页码", example = "1")
    private Integer page;

    @Schema(description = "每页条数", example = "10")
    private Integer size;

    @Schema(description = "关键字（按标签名模糊搜索）")
    private String keyword;
}
```

**注意**：
- 分页参数**也可以**不建 DTO，直接在 Controller 用 `@RequestParam Integer page, @RequestParam Integer size` 接，看你喜好。建 DTO 的好处是参数多了之后清爽。
- 这里没加校验 —— 因为分页参数即使前端不传，Controller 可以给默认值 `@RequestParam(defaultValue = "1")`。

---

# 四、写完后的检查清单

- [ ] 文件路径是 `blog/src/main/java/com/yky/blog/admin/dto/TagSaveDTO.java`
- [ ] 包声明是 `package com.yky.blog.admin.dto;`
- [ ] 类上有 `@Data` 和 `@Schema(description = "...")`
- [ ] `name` 字段有 `@NotBlank` + `@Size`
- [ ] IDE 没标红、可正常编译
- [ ] **没有** `id` / `createTime` / `updateTime` 字段

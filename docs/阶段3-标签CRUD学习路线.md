# 阶段 3：标签 CRUD —— 学习导向路线图

> 目标：以"标签管理"这个最简单的业务为载体，**亲手走完一遍完整的后端 → 前端 → 联调**流程，建立起后续所有 CRUD 模块的"肌肉记忆"。

---

## 〇、心态预设

- 这一遍**慢一点没关系**。你只需要走通这一次，标签、合集、Banner、用户管理几乎都是同一个套路。
- 卡住时不要直接问答案。先想"我这一步到底想干什么 → 中间数据应该长什么样"，再去查文档/搜索/问 AI。
- **每一步都要能"看到效果"**：后端写完用 Knife4j 自测、前端写完用浏览器自测。永远别一次写一大坨再调试。

---

## 一、模块全貌

### 后端要新建的文件（5 个）

```
blog/src/main/java/com/yky/blog/
├── admin/
│   ├── controller/
│   │   └── TagController.java          ← 接收 HTTP 请求
│   ├── service/
│   │   ├── TagService.java             ← 业务接口
│   │   └── impl/TagServiceImpl.java    ← 业务实现
│   └── dto/
│       ├── TagSaveDTO.java             ← 新增/编辑入参
│       └── TagPageDTO.java             ← 分页查询入参（可选）
```

### 前端要新建/改的文件（2 个）

```
blog-admin/src/
├── api/
│   └── tag.ts                          ← 封装四个接口
└── views/content/tag/
    └── index.vue                       ← 替换死数据为真实接口
```

### 4 个接口设计

| 方法 | 路径 | 用途 | 入参 | 出参 |
|------|------|------|------|------|
| GET | `/admin/tag/page` | 分页列表 | `page`, `size`, `keyword?` | `Page<Tag>` |
| POST | `/admin/tag` | 新增 | `TagSaveDTO` | `Long`（新 ID） |
| PUT | `/admin/tag/{id}` | 编辑 | `TagSaveDTO` | `void` |
| DELETE | `/admin/tag/{id}` | 删除 | path 上的 id | `void` |

---

## 二、后端实现 — 自上而下分步

### 第 1 步：写 DTO

**为什么需要 DTO？**
直接拿 `Tag` 实体接收前端数据有几个问题：
- 实体包含 `id / createTime / updateTime / deleted` 这些不该由前端控制的字段
- 实体绑死了数据库结构，将来表结构变动会影响接口
- 不方便做 `@Valid` 校验

所以单独建一个 `TagSaveDTO`，只放新增/编辑真正需要前端传的字段。

**`TagSaveDTO.java` 应该包含**：
- `name` —— 标签名，加 `@NotBlank` 校验非空
- `color` —— 颜色，可选

**用到的知识点**：
- `@Data`（Lombok 自动生成 getter/setter）
- `@Schema(description = "...")`（Knife4j 接口文档展示用）
- `@NotBlank(message = "...")`（参数校验，配合 Controller 的 `@Valid` 使用）

### 第 2 步：写 Service 接口和实现

**为什么要拆 Service / ServiceImpl 两层？**
- 接口暴露"业务能做什么"，实现暴露"具体怎么做"
- 测试时可以 mock Service，不需要真的连数据库
- 接口本身就是文档

**MyBatis-Plus 的便利**：可以让 `TagService extends IService<Tag>`，`TagServiceImpl extends ServiceImpl<TagMapper, Tag>`，免费拿到 `save`、`removeById`、`updateById`、`page`、`list` 等一大堆方法，你只需要写**业务特殊逻辑**。

**对于标签 CRUD，业务逻辑几乎不存在**，所以 Service 层薄到几乎只是转调。重点逻辑只有：

1. **新增/编辑时检查重名**：先 `lambdaQuery().eq(Tag::getName, name).count() > 0` 判断，重了就 `throw new BizException(...)`
2. **分页查询时按关键字过滤**：`lambdaQuery().like(StrUtil.isNotBlank(keyword), Tag::getName, keyword).page(...)`

**用到的知识点**：
- MyBatis-Plus 的 `IService` / `ServiceImpl`
- `LambdaQueryWrapper`（链式 + 类型安全的查询条件构造）
- `BizException`（你项目里已经有了，用来抛业务异常，会被 `GlobalExceptionHandler` 捕获并返回标准的 `Result`）
- `BeanUtil.copyProperties()`（DTO → Entity 字段拷贝，避免手写 setter）

### 第 3 步：写 Controller

**Controller 的职责非常单一**：
- 接收 HTTP 请求 → 转给 Service → 包装成 `Result` 返回
- **不写业务逻辑**

**关键代码骨架**（不给你完整代码，自己写一遍）：

```java
@Tag(name = "标签管理")
@RestController
@RequestMapping("/admin/tag")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public Result<Page<...>> page(@RequestParam ... ) {
        return Result.success(tagService.pageTag(...));
    }

    @PostMapping
    @Operation(summary = "新增标签")
    public Result<Long> save(@Valid @RequestBody TagSaveDTO dto) {
        return Result.success(tagService.saveTag(dto));
    }

    // PUT 和 DELETE 同理，自己尝试写
}
```

**用到的知识点**：
- `@RestController` = `@Controller` + `@ResponseBody`
- `@RequestMapping` 在类上定义模块前缀
- `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping`
- `@RequestParam`（URL 查询参数）vs `@PathVariable`（URL 路径参数）vs `@RequestBody`（请求体 JSON）
- `@Valid` 触发 DTO 上的校验注解
- `@RequiredArgsConstructor`（Lombok 自动生成包含 final 字段的构造函数，配合 Spring 构造器注入）

### 第 4 步：用 Knife4j 自测后端

后端跑起来后访问 **`http://localhost:8080/doc.html`**（不是 `swagger-ui.html`，你用的是 Knife4j）。

在里面你会看到刚写的 4 个接口，每个都能直接点击"调试"：
1. 先调 `POST /admin/tag` 新增一条数据
2. 调 `GET /admin/tag/page` 看能不能查出来
3. 调 `PUT /admin/tag/{id}` 改一下
4. 调 `DELETE /admin/tag/{id}` 删掉

**调试技巧**：
- 后端报错时**直接看控制台堆栈**，从最后一行（`Caused by:`）往上看，根因永远在最深处
- 校验失败（400）：DTO 字段名或类型对不上
- 500 异常：看堆栈，常见原因——空指针、字段映射错（`@TableField` 用错）、数据库字段类型不匹配
- 看不到接口：Controller 没被 Spring 扫到，检查 `@RestController` 是否漏写、包是否在 `@SpringBootApplication` 扫描范围内

**特别提醒**：登录后 Knife4j 调用受 `Sa-Token` 保护的接口需要带 token。先调 `/admin/auth/login` 拿到 token，复制到 Knife4j 顶部的"全局参数"里设 `Authorization: <token>`，否则会返回 401。

---

## 三、前端实现 — 自下而上分步

### 第 5 步：封装 API 函数（`api/tag.ts`）

**为什么单独建 `api/` 目录？**
- 把所有后端调用集中管理，组件里只写"我要拿数据/我要保存"，不关心 URL 是啥
- 接口路径变了只改一处
- 类型定义（TS 接口）集中放，组件用起来有自动补全

**骨架**（请求方法可以自己写，再用类型推断让 IDE 帮你）：

```ts
import request from '@/utils/request'

export interface Tag {
  id: number
  name: string
  color: string
  createTime: string
}

export interface TagSaveDTO {
  name: string
  color?: string
}

export const getTagPage = (params: { page: number; size: number; keyword?: string }) =>
  request.get('/admin/tag/page', { params })

export const saveTag = (data: TagSaveDTO) =>
  request.post('/admin/tag', data)

// updateTag / deleteTag 自己照葫芦画瓢
```

**用到的知识点**：
- TS 的 `interface` 定义对象类型
- 可选属性 `?:`
- axios 的 `request.get(url, { params })`（GET 参数走 query string）
- axios 的 `request.post(url, body)`（POST 参数走请求体）

### 第 6 步：改造 `views/content/tag/index.vue`

**当前问题**：里面的 `tableData` 是写死的假数据。

**改造步骤**（每一步都能独立验证）：

1. **拉真实列表**
   - 在 `<script setup>` 里 `import { onMounted, ref } from 'vue'`
   - 把 `tableData` 改成 `ref<Tag[]>([])`
   - 写一个 `loadData()` 函数：`const res = await getTagPage({page, size}); tableData.value = res.data.records; total.value = res.data.total`
   - `onMounted(() => loadData())`
   - 浏览器看是否能渲染出真实数据 → ✅ 才进入下一步

2. **关键字搜索**
   - `watch(keyword, () => loadData())` —— 关键字变化重新加载
   - 或者监听回车键 `@keyup.enter="loadData"`

3. **分页切换**
   - `el-pagination` 的 `@current-change="loadData"`、`@size-change="loadData"`

4. **新增**
   - 点击"新增标签"按钮 → `dialogVisible = true`，`form = { name: '', color: '#xxx' }`
   - 提交时调 `saveTag(form)` → 关闭弹窗 → `loadData()` 刷新列表
   - 用 `ElMessage.success('新增成功')` 给用户反馈

5. **编辑**
   - 与新增共用同一个弹窗
   - 区别是 `openDialog(row)` 时把 row 的字段填进 form，并记下 `editingId`
   - 提交时根据 `editingId` 是否存在决定调 `saveTag` 还是 `updateTag`

6. **删除**
   - 点击删除 → `ElMessageBox.confirm('确定删除吗？')` 二次确认
   - 确认后调 `deleteTag(id)` → `loadData()`

**用到的知识点**：
- Vue 3 Composition API：`ref`、`reactive`、`onMounted`、`watch`、`computed`
- async/await（处理 axios Promise）
- Element Plus 组件：`el-table`、`el-dialog`、`el-form`、`el-form-item`、`el-input`、`el-pagination`、`el-button`、`ElMessage`、`ElMessageBox`
- v-model 双向绑定
- 父子组件 props（如果你想把 Dialog 拆成独立子组件 `TagDialog.vue`）

---

## 四、联调常见坑预警

### 后端字段名 vs 前端字段名对不上

**症状**：前端表格列是空的，但 Network 看响应是有数据的。

**原因**：MyBatis-Plus 默认下划线转驼峰（`create_time` → `createTime`），但前端代码可能写成了 `create_time`。

**对策**：F12 → Network → 选中接口请求 → 看 Response 标签里实际字段名是啥，前端就用啥。

### 跨域问题（应该已经解决）

如果忽然又跨域报错，看 `bug/001` 的 CORS 文档。

### 401 Token 失效

**症状**：刚还能用，过了一会儿请求全 401。

**原因**：Sa-Token 默认 token 有过期时间。当前 `application.yml` 里写的是 `timeout: 2592000`（30 天），开发期不太会遇到。如果碰到，重新登录拿新 token。

### 删除后列表没刷新

**原因**：调完 `deleteTag` 没调 `loadData()`，或者 await 漏掉了。

### 新增标签后看不到新数据

**原因**：同上，没刷新列表。或者新增成功但分页停在最后一页，新数据进了第一页你没翻过去。

### MyBatis-Plus 软删除"幻觉"

**症状**：明明删除了，再次新增同名标签报"重复"。

**原因**：MyBatis-Plus 配的是逻辑删除（`deleted` 字段），数据库里数据没真的删，名字唯一性检查时把已删的数据也算进去了。

**对策**：要么"删除"时改成物理删除，要么 `lambdaQuery().eq(Tag::getName, name).eq(Tag::getDeleted, 0).count()` 显式只查未删的（其实 MyBatis-Plus 自动会过滤，但要确认你的配置生效）。

---

## 五、推进节奏建议

| 时间段 | 任务 | 完成标志 |
|--------|------|----------|
| 第 1 小时 | 写 DTO + Service 接口和实现 | 代码无编译报错 |
| 第 2 小时 | 写 Controller，启动后端 | Knife4j 能看到 4 个接口 |
| 第 3 小时 | Knife4j 自测 4 个接口 | 全部能正常调用并返回 |
| 第 4 小时 | 写 `api/tag.ts` + 改造 index.vue 的列表 | 浏览器能看到真实数据 |
| 第 5 小时 | 接通新增 + 编辑 + 删除 + 搜索 | 完整 CRUD 链路跑通 |

**5 小时是充裕的预算**，如果第一次走顺利可能 3 小时就够。

---

## 六、什么时候问我

- **思路卡壳**："这一步要做 X，但我不知道从哪下手" → 问我
- **报错看不懂**：把完整堆栈贴出来 → 问我
- **代码 review**：你自己写完一版，让我看看哪里能优化 → 给我贴代码
- **不要直接让我"帮你写完"** —— 那就违背了你自己学这一遍的目的

---

## 七、走完之后

标签 CRUD 走完，**合集 CRUD 几乎是复制粘贴**（字段不同而已，结构完全一样）。可以挑战一下：在不看本文档的情况下，从头自己再走一遍合集 CRUD，看 30 分钟能不能搞定。如果能，说明你已经掌握这套模式了，可以进入阶段 4（文章 CRUD + Markdown 编辑器，那是真正的硬骨头）。

# M7：测试、优化与交付手把手指导手册

> 适用项目：my-todo-list 待办事项项目  
> 适合人群：编程新手  
> 目标：完成项目测试、问题修复、性能优化、文档完善和最终交付。

---

## 一、M7 阶段要完成什么

M7 不是继续增加大量新功能，而是确认前面完成的功能能够稳定运行，并且项目可以交付给其他人使用。

完成 M7 后，项目应该达到以下目标：

- 需求文档中的功能全部实现。
- 新增、完成、取消完成、删除、筛选功能可以正常使用。
- 空输入、重复点击、长文本和特殊字符不会导致页面崩溃。
- 刷新页面后，数据行为符合需求。
- 手机、平板和电脑宽度下都可以正常使用。
- 浏览器控制台没有明显的红色错误。
- 项目能够成功构建。
- README 中有完整的安装、启动和使用说明。
- 其他人拿到项目后，可以按照说明运行项目。

M7 的执行顺序如下：

```text
阅读需求
  ↓
建立测试清单
  ↓
启动项目
  ↓
功能测试
  ↓
边界测试
  ↓
修复问题
  ↓
代码优化
  ↓
响应式检查
  ↓
构建项目
  ↓
整理交付文件
  ↓
最终验收
```

## 二、准备测试环境

### 2.1 检查 Node.js

打开终端，进入项目根目录，执行：

```
node -v
npm -v
```

如果能够正常显示版本号，说明 Node.js 和 npm 已经安装。

如果项目中存在 `package-lock.json`，使用 npm。

如果项目中存在 `pnpm-lock.yaml`，使用 pnpm。

如果项目中存在 `yarn.lock`，使用 yarn。

不要在同一个项目中混用多个包管理器。

### 2.2 安装依赖

如果项目使用 npm：

```
npm install
```

如果项目使用 pnpm：

```
pnpm install
```

如果项目使用 yarn：

```
yarn install
```

### 2.3 启动项目

常见启动命令：

```
npm run dev
```

如果项目使用 pnpm：

```
pnpm run dev
```

启动成功后，终端一般会显示类似地址：

```
http://localhost:5173
```

复制这个地址到浏览器打开。

### 2.4 查看项目脚本

打开项目根目录中的 `package.json`，查看 `scripts` 字段。

示例：

```
{
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview",
    "test": "vitest"
  }
}
```

常见命令说明：

| 命令              | 作用               |
| ----------------- | ------------------ |
| `npm run dev`     | 启动开发环境       |
| `npm run build`   | 构建生产版本       |
| `npm run preview` | 预览生产版本       |
| `npm test`        | 执行自动化测试     |
| `npm run lint`    | 检查代码格式和错误 |

------

## 三、阅读需求并建立验收表

测试不能只凭感觉，需要把需求转换成明确的测试项目。

建议在项目中建立一个文件：

```
M7-测试记录.md
```

可以使用下面的测试表：

| 编号 | 功能     | 操作步骤           | 预期结果           | 实际结果 | 状态 |
| ---- | -------- | ------------------ | ------------------ | -------- | ---- |
| F-01 | 新增待办 | 输入文字并点击新增 | 列表出现新任务     |          | 未测 |
| F-02 | 完成待办 | 点击完成按钮       | 任务显示完成样式   |          | 未测 |
| F-03 | 取消完成 | 再次点击完成按钮   | 任务恢复未完成状态 |          | 未测 |
| F-04 | 删除待办 | 点击删除按钮       | 任务从列表消失     |          | 未测 |
| F-05 | 筛选任务 | 点击筛选按钮       | 显示对应任务       |          | 未测 |
| F-06 | 数据保存 | 修改任务后刷新页面 | 数据按照需求保留   |          | 未测 |
| F-07 | 空状态   | 删除所有任务       | 显示空状态提示     |          | 未测 |
| F-08 | 错误输入 | 输入空内容         | 显示错误提示       |          | 未测 |
| F-09 | 移动端   | 使用手机宽度打开   | 页面布局正常       |          | 未测 |
| F-10 | 生产构建 | 执行构建命令       | 构建成功           |          | 未测 |

还需要打开以下文件，逐条核对项目要求：

```
REQUIREMENTS.md
MILESTONES.md
README.md
docs/knowledge/
```

如果需求中还有编辑任务、清空任务、排序、主题切换等功能，也要继续添加到测试表。

------

## 四、功能测试

### 4.1 测试新增任务

依次测试以下输入：

1. 正常文字，例如：

```
学习 JavaScript
```

1. 前后带空格：

```
  买牛奶
```

1. 空字符串。
2. 只有空格：

```
     
```

1. 很长的文字。
2. 中文。
3. 英文。
4. 数字。
5. Emoji：

```
完成作业 ✅
```

1. 特殊字符：

```
<script>alert(1)</script>
```

正常情况下，前后空格应该被清除，空内容不能添加。

核心实现示例：

```
function addTodo() {
  const text = inputValue.trim();

  if (!text) {
    errorMessage = '请输入待办内容';
    return;
  }

  const todo = {
    id: crypto.randomUUID(),
    title: text,
    completed: false,
    createdAt: new Date().toISOString()
  };

  todos.push(todo);

  inputValue = '';
  errorMessage = '';

  saveTodos(todos);
}
```

如果运行环境不支持 `crypto.randomUUID()`，可以使用：

```
const id = `${Date.now()}-${Math.random()
  .toString(16)
  .slice(2)}`;
```

### 4.2 测试完成任务

点击任务的完成按钮，检查：

- 当前任务是否显示完成样式。
- 是否出现删除线。
- 完成状态是否只影响当前任务。
- 其他任务是否保持不变。
- 完成数量是否正确更新。

实现示例：

```
function toggleTodo(id) {
  todos = todos.map(todo => {
    if (todo.id === id) {
      return {
        ...todo,
        completed: !todo.completed
      };
    }

    return todo;
  });

  saveTodos(todos);
}
```

### 4.3 测试取消完成

再次点击已经完成的任务。

预期结果：

- 删除线消失。
- 任务恢复未完成状态。
- 未完成任务数量增加。
- 已完成任务数量减少。

### 4.4 测试删除任务

分别删除：

- 第一条任务。
- 中间一条任务。
- 最后一条任务。
- 唯一一条任务。

实现示例：

```
function deleteTodo(id) {
  todos = todos.filter(todo => todo.id !== id);
  saveTodos(todos);
}
```

删除唯一一条任务后，页面应该显示类似内容：

```
暂无待办事项
```

如果需求要求确认删除，可以使用：

```
function confirmDelete(id) {
  const confirmed = window.confirm('确定要删除这个任务吗？');

  if (confirmed) {
    deleteTodo(id);
  }
}
```

### 4.5 测试筛选功能

常见筛选包括：

- 全部任务。
- 未完成任务。
- 已完成任务。

实现示例：

```
function getVisibleTodos(todos, filter) {
  if (filter === 'active') {
    return todos.filter(todo => !todo.completed);
  }

  if (filter === 'completed') {
    return todos.filter(todo => todo.completed);
  }

  return todos;
}
```

统计未完成任务数量：

```
const activeCount = todos.filter(
  todo => !todo.completed
).length;
```

需要测试：

- 所有任务都是未完成。
- 所有任务都是已完成。
- 一半完成、一半未完成。
- 筛选结果为空。
- 删除任务后筛选结果是否同步更新。

------

## 五、数据保存功能

如果项目要求刷新页面后保留数据，可以使用 `localStorage`。

### 5.1 保存数据

```
const STORAGE_KEY = 'my-todo-list.todos';

function saveTodos(todos) {
  try {
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify(todos)
    );
  } catch (error) {
    console.error('保存待办失败：', error);
  }
}
```

### 5.2 读取数据

```
function loadTodos() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);

    if (!raw) {
      return [];
    }

    const data = JSON.parse(raw);

    if (!Array.isArray(data)) {
      return [];
    }

    return data;
  } catch (error) {
    console.error('读取待办失败：', error);
    return [];
  }
}
```

### 5.3 项目启动时加载数据

```
let todos = loadTodos();
```

如果使用 React，可以在组件加载时读取：

```
useEffect(() => {
  const savedTodos = loadTodos();
  setTodos(savedTodos);
}, []);
```

### 5.4 测试刷新保存

按照下面的顺序测试：

1. 新增一个任务。
2. 刷新浏览器。
3. 检查任务是否仍然存在。
4. 完成一个任务。
5. 刷新浏览器。
6. 检查完成状态是否保留。
7. 删除一个任务。
8. 刷新浏览器。
9. 检查删除结果是否保留。

### 5.5 测试损坏数据

打开浏览器开发者工具，在 Console 中执行：

```
localStorage.setItem(
  'my-todo-list.todos',
  '{bad json'
);

location.reload();
```

程序不能白屏，应该恢复为空数组或显示错误提示。

------

## 六、边界测试

### 6.1 输入边界

需要测试：

- 空字符串。
- 只有空格。
- 一个字符。
- 特别长的文字。
- 中文。
- 英文。
- 数字。
- Emoji。
- 特殊符号。
- HTML 标签。
- 脚本字符串。

显示用户输入时，应该使用文本内容，而不是直接插入 HTML。

推荐：

```
element.textContent = todo.title;
```

不推荐：

```
element.innerHTML = todo.title;
```

直接使用 `innerHTML` 显示用户输入可能造成 XSS 安全问题。

### 6.2 数据边界

测试以下情况：

- 0 条任务。
- 1 条任务。
- 10 条任务。
- 100 条任务。
- 全部未完成。
- 全部已完成。
- 一半完成。
- 筛选结果为空。
- localStorage 没有数据。
- localStorage 中的数据格式错误。

### 6.3 快速重复操作

快速点击新增按钮，检查：

- 是否产生重复任务。
- 是否产生多个相同 ID。
- 页面是否卡顿。
- 计数是否正确。

如果新增操作是异步的，可以在保存期间禁用按钮：

```
<button
  disabled={isSaving}
  onClick={handleSubmit}
>
  {isSaving ? '保存中...' : '新增'}
</button>
```

------

## 七、浏览器开发者工具

按 `F12` 或右键选择“检查”。

### 7.1 Console 控制台

重点查看：

- 红色错误。
- 黄色警告。
- undefined 错误。
- 网络请求失败。
- JSON 解析失败。

M7 交付前，应尽量清除所有未处理的红色错误。

### 7.2 Network 网络

检查接口请求：

- 请求地址是否正确。
- 请求方法是否正确。
- 请求参数是否正确。
- 返回状态码是否正确。
- 返回数据格式是否正确。

常见状态码：

| 状态码 | 含义         |
| ------ | ------------ |
| 200    | 请求成功     |
| 201    | 创建成功     |
| 400    | 请求参数错误 |
| 401    | 没有登录     |
| 403    | 没有权限     |
| 404    | 地址不存在   |
| 500    | 服务器错误   |

### 7.3 Application 应用

可以查看：

- Local Storage。
- Session Storage。
- Cookie。
- 浏览器缓存。

### 7.4 Elements 元素

可以检查：

- 元素是否存在。
- CSS 是否生效。
- 元素宽度是否正确。
- 是否出现溢出。
- 是否存在多余的 margin 或 padding。

------

## 八、响应式布局检查

使用浏览器开发者工具的设备模拟器，检查以下宽度：

- 手机：375px。
- 平板：768px。
- 桌面：1440px。

检查内容：

- 页面不能出现不必要的横向滚动条。
- 输入框不能超出屏幕。
- 按钮不能被挤出屏幕。
- 文字不能被遮挡。
- 任务内容过长时不能破坏布局。
- 手机端按钮应该容易点击。
- Tab 键可以操作页面。
- 输入框有清晰的提示。

基础 CSS 示例：

```
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  min-width: 320px;
}

.todo-container {
  width: min(100% - 32px, 720px);
  margin: 0 auto;
}

.todo-form {
  display: flex;
  gap: 8px;
}

.todo-form input {
  flex: 1;
  min-width: 0;
}

@media (max-width: 480px) {
  .todo-form {
    display: grid;
    gap: 8px;
  }

  .todo-form button {
    width: 100%;
  }
}
```

------

## 九、代码质量优化

### 9.1 删除无用代码

检查并删除：

- 没有使用的变量。
- 没有使用的组件。
- 没有使用的 CSS。
- 没有使用的 import。
- 调试用的 `console.log`。
- 临时测试数据。

### 9.2 使用清晰的变量名

不推荐：

```
const x = [];
const a = true;
```

推荐：

```
const todos = [];
const isCompleted = true;
```

### 9.3 拆分功能

可以按照下面的结构组织：

```
src/
├── components/
│   ├── TodoForm.jsx
│   ├── TodoList.jsx
│   ├── TodoItem.jsx
│   └── TodoFilter.jsx
├── utils/
│   └── storage.js
├── App.jsx
└── main.jsx
```

功能职责：

| 文件         | 职责           |
| ------------ | -------------- |
| `TodoForm`   | 输入和新增任务 |
| `TodoList`   | 显示任务列表   |
| `TodoItem`   | 显示单个任务   |
| `TodoFilter` | 筛选任务       |
| `storage.js` | 保存和读取数据 |
| `App`        | 管理整体状态   |

### 9.4 不要过度优化

待办事项通常不会有几百万条数据。

不要为了“看起来高级”而过早加入：

- 复杂缓存。
- 虚拟列表。
- 多层状态管理。
- 不必要的第三方库。
- 复杂的后端结构。

先保证功能正确、代码易读、项目稳定。

------

## 十、自动化测试

先查看 `package.json` 是否配置测试脚本。

常见命令：

```
npm test
```

或者：

```
npm run test
```

运行覆盖率测试：

```
npm run test:coverage
```

推荐优先测试业务函数。

示例：

```
describe('getVisibleTodos', () => {
  const todos = [
    {
      id: '1',
      title: '学习',
      completed: false
    },
    {
      id: '2',
      title: '购物',
      completed: true
    }
  ];

  it('应该返回未完成任务', () => {
    const result = getVisibleTodos(
      todos,
      'active'
    );

    expect(result).toHaveLength(1);
    expect(result[0].id).toBe('1');
  });

  it('应该返回已完成任务', () => {
    const result = getVisibleTodos(
      todos,
      'completed'
    );

    expect(result).toHaveLength(1);
    expect(result[0].id).toBe('2');
  });

  it('应该返回全部任务', () => {
    const result = getVisibleTodos(
      todos,
      'all'
    );

    expect(result).toHaveLength(2);
  });
});
```

如果项目没有测试框架，不建议在 M7 最后阶段大范围修改技术栈。可以先完成手工测试，再为最重要的纯函数补充测试。

------

## 十一、构建生产版本

开发环境能够运行，不代表交付版本一定没有问题。

执行：

```
npm run build
```

如果构建成功，通常会生成：

```
dist/
```

然后启动预览：

```
npm run preview
```

在预览环境重新测试：

- 新增任务。
- 完成任务。
- 删除任务。
- 筛选任务。
- 刷新页面。
- 手机宽度。
- 空状态。
- 直接访问页面。

如果构建失败：

1. 先查看第一条真正的错误。
2. 检查 import 路径。
3. 检查文件名大小写。
4. 检查环境变量。
5. 检查是否使用了浏览器不支持的 Node.js API。
6. 修复后重新执行构建。

------

## 十二、README 交付说明

README 至少应该包含以下内容：

```
# 项目名称

## 项目简介

说明这个项目解决什么问题，有哪些主要功能。

## 环境要求

- Node.js 版本
- npm、pnpm 或 yarn 版本

## 安装依赖

```bash
npm install
```

## 启动项目

```
npm run dev
```

## 构建项目

```
npm run build
```

## 预览生产版本

```
npm run preview
```

## 功能清单

- 新增待办
- 完成待办
- 取消完成
- 删除待办
- 筛选待办
- 数据保存

## 测试说明

说明执行过哪些测试，测试结果是什么。

## 已知限制

例如：

数据只保存在当前浏览器中，清除浏览器数据后，任务记录可能丢失。

```
命令和版本号必须根据项目实际情况修改。

---

## 十三、Git 交付前检查

执行：

```bash
git status
```

查看有哪些文件发生变化。

执行：

```
git diff --check
```

检查空格和格式问题。

执行：

```
git diff
```

检查具体修改内容。

确认以下内容：

- 只包含本次 M7 相关修改。
- 没有提交密码。
- 没有提交 Token。
- 没有提交 `.env` 私密配置。
- 没有提交 `node_modules`。
- 没有提交临时日志。
- README 内容完整。
- M7 指导手册已放在 `docs/M7_GUIDE.md`。

如果项目允许提交：

```
git add docs/M7_GUIDE.md README.md
git commit -m "docs: add M7 testing optimization and delivery guide"
```

如果还修改了代码，可以根据实际情况添加对应文件。

------

## 十四、常见问题处理

### 14.1 页面白屏

打开浏览器控制台，查看第一条红色错误。

常见原因：

- 文件路径错误。
- 变量没有定义。
- JSON 解析失败。
- 组件没有正确返回内容。
- import 名称错误。
- 组件名称大小写错误。

### 14.2 刷新后任务消失

检查：

- 新增任务后是否调用保存函数。
- 完成任务后是否调用保存函数。
- 删除任务后是否调用保存函数。
- 页面启动时是否调用读取函数。
- 保存和读取使用的 key 是否一致。

例如下面两个 key 不一致，会导致读取失败：

```
localStorage.setItem(
  'todo-list',
  JSON.stringify(todos)
);

localStorage.getItem(
  'my-todo-list.todos'
);
```

### 14.3 点击新增没有反应

检查：

- 按钮是否绑定点击事件。
- 输入框的值是否正确读取。
- 输入内容是否被 `trim()` 后变为空。
- 表单是否刷新了页面。
- 控制台是否出现错误。
- 按钮是否被 `disabled`。

### 14.4 计数不更新

不要只修改旧数组，而要更新状态。

不推荐：

```
todos.push(newTodo);
```

如果使用 React，推荐：

```
setTodos(previousTodos => [
  ...previousTodos,
  newTodo
]);
```

### 14.5 手机页面溢出

检查：

- 是否使用固定宽度。
- 是否存在过大的 `min-width`。
- 长文字是否无法换行。
- 按钮是否超出父元素。
- 是否存在图片或元素宽度超过屏幕。

可以使用：

```
max-width: 100%;
overflow-wrap: anywhere;
```

------

## 十五、M7 最终验收清单

完成一项就打勾：

- 

  已阅读 

  ```
  REQUIREMENTS.md
  ```

  。

- 

  已阅读 

  ```
  MILESTONES.md
  ```

  。

- 

  已把需求转换成测试表。

- 

  依赖安装成功。

- 

  开发环境启动成功。

- 

  新增正常任务通过。

- 

  空输入处理正确。

- 

  纯空格输入处理正确。

- 

  完成任务通过。

- 

  取消完成通过。

- 

  删除第一条任务通过。

- 

  删除中间任务通过。

- 

  删除最后一条任务通过。

- 

  删除唯一任务后显示空状态。

- 

  全部任务筛选通过。

- 

  未完成任务筛选通过。

- 

  已完成任务筛选通过。

- 

  中文输入通过。

- 

  Emoji 输入通过。

- 

  长文本输入通过。

- 

  特殊字符输入通过。

- 

  刷新页面后数据行为正确。

- 

  损坏的本地数据不会导致白屏。

- 

  控制台没有未处理的红色错误。

- 

  Network 请求正常。

- 

  手机宽度检查通过。

- 

  平板宽度检查通过。

- 

  桌面宽度检查通过。

- 

  Tab 键操作正常。

- 

  Enter 键操作符合需求。

- 

  自动化测试通过。

- 

  生产构建成功。

- 

  preview 环境测试通过。

- 

  README 已完善。

- 

  Git 差异检查通过。

- 

  没有提交密码和 Token。

- 

  文件已保存到 

  ```
  docs/M7_GUIDE.md
  ```

  。

------

## 十六、最终交付内容

最终建议交付以下内容：

1. 项目源代码。
2. `README.md`。
3. `docs/M7_GUIDE.md`。
4. `M7-测试记录.md`。
5. 主页面截图。
6. 空状态截图。
7. 筛选状态截图。
8. 移动端页面截图。
9. 测试结果。
10. 构建结果。
11. 已知限制说明。

最终交付说明可以写成：

> 本项目已按照需求完成 M7 测试、优化与交付检查。已覆盖正常流程、空输入、边界数据、刷新保存、响应式布局、自动化测试和生产构建。测试过程中发现的问题已经修复，剩余限制已在 README 中说明。
>
> ```
> 
> ```

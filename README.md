# UniFit

UniFit 是一套面向学校、家长和校服供应方的智能校服订购系统。它不是只把校服搬到线上销售，而是围绕“校服尺码难选、团体订购难管、库存补货难预判”这几个真实问题，做了一条从智能推荐、下单履约、穿着反馈到运营校准的数据闭环。

本仓库是可公开分享版本，已移除运行时数据和敏感配置，仅保留源码、测试脚本和脱敏后的数据库结构快照。

## 核心亮点

- **多维打分**。系统会把身高、体重、胸围、腰围、臀围、肩宽全部纳入考量，每个维度有独立权重（身高 1.3、体重 1.2、胸围 1.0……），最终给每个候选尺码算出一个加权得分。推荐结果不是简单的"身高多少穿多大"，而是会告诉你哪些维度命中了、哪些卡在边界、为什么推这个码。
- **把握评估**。系统从资料完整度、维度命中数、最优和次优的分差、是否有维度卡边界等角度，综合算出一个 0–100 的匹配把握分。如果把握分低于 60，前端会主动提示"建议参考尺码表手动确认"，避免盲目信任推荐。
- **群体校准**。每一条"偏大 / 合身 / 偏小"的真实反馈都会被按商品、学校、全局三个层级汇总。系统用时间衰减加权算出各尺码的偏向比例，生成分数偏移量，在后续推荐时自动微调排序。如果某个学校的 M 码普遍偏小，系统会逐步把 L 码往前推。
- **偏好学习**。用户可以直接设置"我喜欢穿宽松的"，也可以什么都不设——系统会从历史反馈里自动学出偏好方向（偏松 / 合身 / 偏修身），按服装品类分别记录。学出来的偏好会转成分数偏移，渐进地影响推荐排序。
- **A/B 实验**。内置了基于 SHA-256 的稳定分桶机制：同一个用户在同一轮实验里永远落在同一个桶。A 组走基础推荐，B 组走校准推荐，后台可以对比两组的采纳率、下单率、反馈覆盖率和合身率，用数据决定校准策略是否上线。
- **链路追踪**。用户在商品详情页拿到推荐结果时，系统会生成一条推荐日志；加入购物车和生成订单项时，这条日志 ID 会一路带下去。后续提交尺码反馈时，系统可以回溯到当初推荐了什么、用了哪些校准参数、分桶落在哪个实验组，形成完整的因果链路。
- **行为漏斗**。后台把用户行为拆成"浏览→详情→推荐→加购→下单"五个阶段，按 session 粒度统计每个阶段的到达人数和转化率。还能按推荐入口（商品详情、心愿单、订单反馈等）交叉对比，找出哪个入口转化最高、哪些商品在购物车里放了超过 48 小时没下单。
- **补货预测**。系统按学校、年级、商品、品类、尺码多维度统计历史销量，用日均销量 × 预测天数算出未来需求，再叠加安全库存和供应商交期，直接给出"建议补多少件"。数据稀疏时会标注原因码，提醒运营人员不要盲目采信。
- **库存联动**。下单时预留库存、发货时出库、退款时释放——每一步都会生成带前后快照的库存流水记录。这样即使订单经历了多次状态变更，库存数据也不会跑偏，事后也有据可查。

## 功能概览

**学生 / 家长端**

- 按学校、年级和分类浏览校服商品
- 查看商品详情、图片、价格、尺码和库存状态
- 基于身体测量资料获取智能尺码推荐、匹配把握、推荐理由和备选尺码
- 在商品详情、购物车、订单反馈等入口复用推荐结果
- 将推荐尺码加入购物车并提交订单，保留推荐日志关联
- 查看订单状态和历史订单
- 对已购商品提交合身度、问题部位、穿着偏好和图片评价
- 管理个人测量资料和尺码偏好，让后续推荐更贴合个人习惯

**管理端**

- 管理学校、年级、尺码、校服商品和图文内容
- 管理 SKU、库存流水、低库存预警和补货建议
- 处理订单履约、发货、收货、退款和状态日志
- 查看用户评价、尺码反馈和推荐采纳情况
- 分析推荐量、合身率、低匹配把握热点、反馈分布和 A/B 实验效果
- 管理校准参数的启用、停用、版本、审计和影响范围
- 通过行为漏斗、入口对比、流失分布和滞留购物车定位转化问题
- 查看供需分析、销售趋势、库存预测和导出报表
- 管理后台账号、登录鉴权和操作数据

## 技术栈

- 后端：Java 21、Spring Boot 3.4、Spring Security、MyBatis / MyBatis-Plus、JWT
- 前端：Vue 3、Vite、Vue Router、Pinia、Element Plus、ECharts
- 数据库：MySQL 8+
- 测试：Maven Test、Vite Build、Playwright smoke tests

## 仓库结构

```text
.
├─ School-uniform-intelligent-ordering-system-usersystem/
│  ├─ src/                 # 用户端 Spring Boot API，默认端口 9090
│  ├─ user-vue/            # 用户端 Vue 应用
│  ├─ sql/                 # 用户端相关 SQL 与迁移材料
│  └─ dev.ps1              # 用户端一键开发启动脚本
├─ School-uniform-intelligent-ordering-system-managementor/
│  ├─ admin-boot/          # 管理端 Spring Boot 启动模块，默认端口 9091
│  ├─ admin-auth/          # 管理端认证模块
│  ├─ admin-base-data/     # 基础资料模块
│  ├─ admin-business/      # 订单、库存、上传等业务模块
│  ├─ admin-analytics/     # 分析统计模块
│  ├─ admin-web/           # 管理端 Vue 应用，默认端口 5173
│  └─ dev.ps1              # 管理端一键开发启动脚本
├─ scripts/                # 本地验证和开发辅助脚本
├─ tests/                  # Playwright 与端到端测试
├─ suios.sql               # 公开版 schema-only 数据库快照
└─ package.json            # 仓库级验证命令
```

## 运行前准备

请先安装：

- JDK 21
- Node.js 20 或更新版本
- npm
- MySQL 8 或兼容版本
- PowerShell

创建 MySQL 数据库后导入结构：

```powershell
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS suios DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p suios < suios.sql
```

`suios.sql` 只包含表结构，不包含用户、订单、地址、日志、密码哈希或其他运行时数据。导入后需要自行创建测试数据或通过管理端录入基础资料。

## 环境变量

后端支持通过环境变量覆盖默认开发配置。建议至少配置数据库账号和 JWT 密钥：

```powershell
$env:USERSYSTEM_DB_URL="jdbc:mysql://localhost:3306/suios?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8"
$env:USERSYSTEM_DB_USERNAME="your_user"
$env:USERSYSTEM_DB_PASSWORD="your_password"
$env:USERSYSTEM_JWT_SECRET="replace-with-a-long-random-secret"

$env:ADMIN_DB_URL="jdbc:mysql://localhost:3306/suios?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:ADMIN_DB_USERNAME="your_user"
$env:ADMIN_DB_PASSWORD="your_password"
$env:ADMIN_JWT_SECRET="replace-with-another-long-random-secret"
```

可选上传目录：

```powershell
$env:USERSYSTEM_UPLOAD_DIR="D:/suios/reviews"
$env:ADMIN_UPLOAD_PATH="D:/suios/uploads"
$env:ADMIN_UPLOAD_LEGACY_PATH="D:/ruoyi/uploadPath/upload"
```

默认配置中的 `dev-only-*` 密钥和密码只用于本地开发，不适合生产环境。

## 本地启动

### 启动用户端

```powershell
cd School-uniform-intelligent-ordering-system-usersystem
npm install
npm run dev
```

脚本会启动：

- 用户端后端：`http://localhost:9090`
- 用户端前端：`http://localhost:3000`

也可以分别启动：

```powershell
cd School-uniform-intelligent-ordering-system-usersystem
.\mvnw.cmd spring-boot:run

cd user-vue
npm install
npm run dev
```

### 启动管理端

```powershell
cd School-uniform-intelligent-ordering-system-managementor
npm install
npm run dev
```

脚本会启动：

- 管理端后端：`http://localhost:9091`
- 管理端前端：`http://localhost:5173`

也可以分别启动：

```powershell
cd School-uniform-intelligent-ordering-system-managementor
..\School-uniform-intelligent-ordering-system-usersystem\mvnw.cmd -f .\pom.xml spring-boot:run -pl admin-boot -am

cd admin-web
npm install
npm run dev
```

## 构建与验证

从仓库根目录安装验证依赖：

```powershell
npm install
```

运行快速验证：

```powershell
npm run verify
```

该命令会执行后端测试、前端构建、选择器检查、Playwright mock smoke tests，以及用户端和管理端开发控制台冒烟测试。

如果已经准备好完整 MySQL 测试库和端到端测试所需账号，可以运行：

```powershell
npm run verify:full
```

也可以单独验证前端：

```powershell
cd School-uniform-intelligent-ordering-system-usersystem\user-vue
npm run build

cd ..\..\School-uniform-intelligent-ordering-system-managementor\admin-web
npm run build
```

## 公开版说明

本仓库适合用于代码审阅、学习、二次开发和演示环境搭建。公开版不包含：

- 真实用户、学生、地址、订单、登录日志或评价数据
- 生产数据库导出
- 生产 `.env` 文件
- 上传文件、商品图片和用户图片
- 生产 JWT 密钥、数据库密码或第三方服务凭据

请不要向公开仓库提交真实数据、生产密钥、上传目录、构建产物、依赖目录或日志文件。

## 开发建议

- 先导入 `suios.sql`，再通过管理端补充学校、年级、尺码、商品和库存数据。
- 用户端和管理端默认连接同一个 `suios` 数据库，便于在本地联调完整订购流程。
- 开发环境可以使用默认端口；如果端口被占用，请先停止占用进程或调整前端 Vite 配置。
- 生产部署前必须更换所有默认密钥和账号，并按实际环境配置 HTTPS、跨域、日志、备份和访问控制。

## License

本公开导出版本未附带明确开源许可证。除非仓库所有者另行添加许可证文件，否则请在获得授权后再用于商业分发或生产部署。

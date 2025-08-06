# VividBatis 插件使用说明

VividBatis 是一个 IntelliJ IDEA 插件，旨在帮助开发者在开发过程中预览 MyBatis 动态 SQL 的生成结果，无需运行程序即可查看最终的 SQL 语句。

## 功能特性

1. **SQL 语句标识**：在 MyBatis mapper XML 文件中，为 `select`、`insert`、`update`、`delete` 标签提供可视化图标
2. **参数分析**：自动分析 `parameterType` 并生成默认的 JSON 参数模板
3. **动态 SQL 处理**：支持 `<if>`、`<foreach>`、`<where>`、`<set>`、`<choose>` 等动态标签
4. **SQL 片段引用**：支持 `<include>` 标签引用 `<sql>` 片段
5. **实时预览**：根据输入参数实时生成最终的 SQL 语句

## 使用方法

### 1. 打开 Mapper XML 文件

在项目中打开任何 MyBatis mapper XML 文件，例如：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.UserMapper">
    
    <select id="selectUserById" parameterType="java.lang.Long" resultType="User">
        SELECT * FROM users WHERE id = #{id}
    </select>
    
    <select id="selectUsersByCondition" parameterType="com.example.UserQuery" resultType="User">
        SELECT * FROM users
        <where>
            <if test="name != null">
                AND name = #{name}
            </if>
            <if test="age != null">
                AND age = #{age}
            </if>
        </where>
    </select>
    
</mapper>
```

### 2. 点击图标

在每个 SQL 语句标签（select、insert、update、delete）的左侧会出现一个数据表图标，点击该图标。

### 3. 查看工具窗口

点击图标后，VividBatis 工具窗口会自动打开（位于 IDE 右侧），显示：

- **语句信息**：命名空间、语句 ID 和类型
- **参数编辑器**：JSON 格式的参数输入框，包含根据 `parameterType` 生成的默认值
- **生成按钮**：点击生成最终 SQL
- **SQL 预览区**：显示生成的最终 SQL 语句

### 4. 编辑参数

在参数编辑器中修改 JSON 参数值，例如：

```json
{
  "name": "张三",
  "age": 25
}
```

### 5. 生成 SQL

点击"生成 SQL"按钮，在 SQL 预览区查看最终生成的 SQL：

```sql
SELECT * FROM users WHERE name = '张三' AND age = 25
```

## 支持的 MyBatis 功能

### 动态标签

- `<if test="condition">`: 条件判断
- `<where>`: 自动处理 WHERE 子句
- `<set>`: 自动处理 UPDATE 的 SET 子句
- `<foreach>`: 循环处理集合
- `<choose>/<when>/<otherwise>`: 条件选择

### 参数绑定

- `#{param}`: 预编译参数（会被替换为具体值并加引号）
- `${param}`: 直接替换参数（不加引号）

### SQL 片段

- `<sql id="fragment">`: SQL 片段定义
- `<include refid="fragment"/>`: 引用 SQL 片段

## 参数类型支持

### 基本类型
- `int`, `long`, `double`, `float`, `boolean` 等基本类型及其包装类
- `java.lang.String`
- `java.util.Date`, `java.time.LocalDateTime` 等时间类型
- `java.math.BigDecimal`

### 复杂类型
- Java Bean 对象（自动分析字段并生成 JSON 模板）
- `java.util.Map`

### 集合类型
- `java.util.List`（在 foreach 标签中使用）

## 注意事项

1. 插件会尽力解析动态 SQL，但复杂的条件表达式可能需要手动调整
2. 确保项目中的 Java 类在 classpath 中可以找到
3. 对于复杂的嵌套对象，生成的默认值可能需要手动调整
4. 插件主要用于开发阶段的 SQL 预览，生成的 SQL 可能与实际运行时略有差异

## 示例项目结构

```
src/
├── main/
│   ├── java/
│   │   └── com/example/
│   │       ├── User.java
│   │       ├── UserQuery.java
│   │       └── UserMapper.java
│   └── resources/
│       └── mapper/
│           └── UserMapper.xml
```

其中 `UserQuery.java` 示例：

```java
public class UserQuery {
    private String name;
    private Integer age;
    private List<String> statuses;
    
    // getters and setters...
}
```

生成的默认参数 JSON：

```json
{
  "name": "示例字符串",
  "age": 1,
  "statuses": ["示例值"]
}
``` 
# 什么是 ORM 框架？举一个例子

**ORM（Object-Relational Mapping），对象关系映射**

作用：**把 Java 对象和数据库表之间的转换自动化，让你用操作对象的方式操作数据库，而不用手写大量 SQL 和手动组装结果**

~~~Java
//没有 ORM 时
String sql = "SELECT id, username, age FROM user WHERE id = ?";
PreparedStatement ps = conn.prepareStatement(sql);
ps.setLong(1, 1L);
ResultSet rs = ps.executeQuery();
User user = null;
if (rs.next()) {
    user = new User();
    user.setId(rs.getLong("id"));
    user.setUsername(rs.getString("username"));
    user.setAge(rs.getInt("age"));
}

/*问题：
SQL 和 Java 代码混在一起
每个字段都要手动 getXxx / setXxx
表结构一变，所有映射代码都要改
不同数据库 SQL 方言不同，换库要改代码
*/

//有了 ORM 之后
User user = userRepository.findById(1L);
//或
User user = userMapper.selectById(1L);
/*
框架帮你做了：拼 SQL、执行、把结果集的列映射到对象属性。你只需要操作对象。
*/
~~~


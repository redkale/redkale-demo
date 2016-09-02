/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.base;

import java.io.*;
import static java.lang.Character.toUpperCase;
import java.sql.*;
import java.util.*;

/**
 *
 * @author zhangjx
 */
public class ClassCreator {

    private static final String currentpkg = ClassCreator.class.getPackage().getName();

    private static final String jdbc_url = "jdbc:mysql://localhost:3306/center?autoReconnect=true&amp;characterEncoding=utf8";//数据库url

    private static final String jdbc_user = "root"; //数据库用户名

    private static final String jdbc_pwd = ""; //数据库密码

    public static void main(String[] args) throws Exception {

        String pkg = currentpkg.substring(0, currentpkg.lastIndexOf('.') + 1) + "xxx";  //与base同级的包名

        final String entityClass = "UserDetail";//类名

        final String superEntityClass = "";//父类名

        loadEntity(pkg, entityClass, superEntityClass); //Entity内容
        
    }

    private static void loadEntity(String pkg, String classname, String superclassname) throws Exception {
        String entityBody = createEntityContent(pkg, classname, superclassname); //源码内容
        final File entityFile = new File("src/" + pkg.replace('.', '/') + "/" + classname + ".java");
        if (entityFile.isFile()) throw new RuntimeException(classname + ".java 已经存在");
        FileOutputStream out = new FileOutputStream(entityFile);
        out.write(entityBody.getBytes("UTF-8"));
        out.close();
    }

    private static String createEntityContent(String pkg, String classname, String superclassname) throws Exception {
        com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource source = new com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource();
        source.setUrl(jdbc_url);  //数据库url
        source.setUser(jdbc_user); //数据库账号
        source.setPassword(jdbc_pwd); //数据库密码
        Connection conn = source.getConnection();
        DatabaseMetaData meta = conn.getMetaData();
        final Set<String> columns = new HashSet<>();
        if (superclassname != null && !superclassname.isEmpty()) {
            ResultSet rs = meta.getColumns(null, "%", superclassname.toLowerCase(), null);
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
            rs.close();
        }
        ResultSet rs = meta.getColumns(null, "%", classname.toLowerCase(), null);
//       ResultSetMetaData rsd = rs.getMetaData();
//       for(int i =1 ; i<=rsd.getColumnCount();i++) {
//           System.out.println(rsd.getColumnName(i));
//       }     
        StringBuilder sb = new StringBuilder();

        sb.append("package " + pkg + ";" + "\r\n\r\n");

        //sb.append("import org.redkale.convert.*;\r\n");
        sb.append("import javax.persistence.*;\r\n");
        sb.append("import org.redkale.util.*;\r\n");
        sb.append("import " + currentpkg + ".BaseEntity;\r\n");

        sb.append("\r\n/**\r\n"
            + " *\r\n"
            + " * @author " + System.getProperty("user.name") + "\r\n"
            + " */\r\n");
        //if (classname.contains("Info")) sb.append("@Cacheable\r\n");
        sb.append("public class " + classname + " extends "
            + (superclassname != null && !superclassname.isEmpty() ? superclassname : "BaseEntity") + " {\r\n\r\n");
        boolean idable = false;
        List<StringBuilder> list = new ArrayList<>();
        while (rs.next()) {
            boolean incre = rs.getBoolean("IS_AUTOINCREMENT");
            String column = rs.getString("COLUMN_NAME");
            String type = rs.getString("TYPE_NAME");
            String remark = rs.getString("REMARKS");
            if (!idable) {
                idable = true;
                sb.append("    @Id");
                if (incre) sb.append("\r\n    @GeneratedValue");
            } else if (columns.contains(column)) continue; //跳过被继承的重复字段
            sb.append("\r\n");

            sb.append("    @Comment(\"" + remark.replace('"', '\'') + "\")\r\n");
            if ("createtime".equals(column)) sb.append("    @Column(updatable = false)\r\n");
            String ctype = "NULL";
            if ("INT".equalsIgnoreCase(type)) {
                ctype = "int";
            } else if ("BIGINT".equalsIgnoreCase(type)) {
                ctype = "long";
            } else if ("SMALLINT".equalsIgnoreCase(type)) {
                ctype = "short";
            } else if ("FLOAT".equalsIgnoreCase(type)) {
                ctype = "float";
            } else if ("DECIMAL".equalsIgnoreCase(type)) {
                ctype = "float";
            } else if ("DOUBLE".equalsIgnoreCase(type)) {
                ctype = "double";
            } else if ("VARCHAR".equalsIgnoreCase(type)) {
                ctype = "String";
            } else if (type.contains("TEXT")) {
                ctype = "String";
            } else if (type.contains("BLOB")) {
                ctype = "byte[]";
            }
            sb.append("    private " + ctype + " " + column);
            if ("String".equals(ctype)) sb.append(" = \"\"");
            sb.append("; //" + remark + "\r\n");

            char[] chs2 = column.toCharArray();
            chs2[0] = toUpperCase(chs2[0]);
            String sgname = new String(chs2);

            StringBuilder setter = new StringBuilder();
            setter.append("\r\n    public void set" + sgname + "(" + ctype + " " + column + ") {\r\n");
            setter.append("        this." + column + " = " + column + ";\r\n");
            setter.append("    }\r\n");
            list.add(setter);

            StringBuilder getter = new StringBuilder();
            getter.append("\r\n    public " + ctype + " get" + sgname + "() {\r\n");
            getter.append("        return this." + column + ";\r\n");
            getter.append("    }\r\n");
            list.add(getter);
        }
        for (StringBuilder item : list) {
            sb.append(item);
        }
        sb.append("}\r\n");
        return sb.toString();
    }
}

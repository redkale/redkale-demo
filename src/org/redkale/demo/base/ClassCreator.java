/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.base;

import static java.lang.Character.toUpperCase;
import java.sql.*;
import java.util.*;

/**
 *
 * @author zhangjx
 */
public class ClassCreator {

    public static void main(String[] args) throws Exception {
        create("org.redkale.demo.xxx", "userdetail");
    }

    private static void create(String pkg, String tablename) throws Exception {
        com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource source = new com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource();
        source.setUrl("jdbc:mysql://localhost:3306/center?autoReconnect=true&amp;characterEncoding=utf8");  //数据库url
        source.setUser("root"); //数据库账号
        source.setPassword("1234"); //数据库密码
        Connection conn = source.getConnection();
        DatabaseMetaData meta = conn.getMetaData();
        ResultSet rs = meta.getColumns(null, "%", tablename, null);
//       ResultSetMetaData rsd = rs.getMetaData();
//       for(int i =1 ; i<=rsd.getColumnCount();i++) {
//           System.out.println(rsd.getColumnName(i));
//       }     
        StringBuilder sb = new StringBuilder();

        sb.append("package " + pkg + ";" + "\r\n\r\n");

        sb.append("import org.redkale.util.*;\r\n");
        sb.append("import javax.persistence.*;\r\n");
        sb.append("import org.redkale.convert.*;\r\n");
        sb.append("import " + pkg.substring(0, pkg.lastIndexOf('.')) + ".base.BaseEntity;\r\n");
        char[] chs = tablename.toCharArray();
        chs[0] = toUpperCase(chs[0]);

        String classname = new String(chs).replace("info", "Info").replace("record", "Record").replaceAll("his$", "His");
        sb.append("\r\n/**\r\n"
            + " *\r\n"
            + " * @author " + System.getProperty("user.name") + "\r\n"
            + " */\r\n");
        if (classname.contains("Info")) sb.append("@Cacheable\r\n");
        sb.append("public class " + classname + " extends BaseEntity {\r\n");
        boolean idable = false;
        List<StringBuilder> list = new ArrayList<>();
        while (rs.next()) {
            boolean incre = rs.getBoolean("IS_AUTOINCREMENT");
            String column = rs.getString("COLUMN_NAME");
            String type = rs.getString("TYPE_NAME");
            String remark = rs.getString("REMARKS");
            sb.append("\r\n");
            if (!idable) {
                idable = true;
                sb.append("    @Id\r\n");
                if (incre) sb.append("    @GeneratedValue\r\n");
            }
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
        System.out.println(sb);
    }
}

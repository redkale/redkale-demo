/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.redkale.demo.base;

import java.io.*;
import java.util.*;
import org.redkale.service.Service;
import org.redkale.source.*;
import static org.redkale.source.AbstractDataSource.*;
import org.redkale.util.AnyValue.DefaultAnyValue;

/**
 *
 * @author zhangjx
 */
public class AutoClassCreator {

    private static final String currentpkg = AutoClassCreator.class.getPackage().getName();

    private static final String jdbc_url = "jdbc:mysql://localhost:3306/redemo_platf?autoReconnect=true&amp;characterEncoding=utf8";//数据库url

    private static final String jdbc_user = "root"; //数据库用户名

    private static final String jdbc_pwd = ""; //数据库密码

    public static void main(String[] args) throws Exception {

        String pkg = currentpkg.substring(0, currentpkg.lastIndexOf('.') + 1) + "user";  //与base同级的包名

        final String entityClass = "UserDetail";//类名

        final String superEntityClass = "";//父类名

        loadEntity(pkg, entityClass, superEntityClass); //Entity内容

    }

    private static void loadEntity(String pkg, String classname, String superclassname) throws Exception {
        String entityBody = createEntityContent(pkg, classname, superclassname); //源码内容
        final File entityFile = new File("src/" + pkg.replace('.', '/') + "/" + classname + ".java");
        if (entityFile.isFile()) {
            throw new RuntimeException(classname + ".java 已经存在");
        }
        FileOutputStream out = new FileOutputStream(entityFile);
        out.write(entityBody.getBytes("UTF-8"));
        out.close();
    }

    private static String createEntityContent(String pkg, String classname, String superclassname) throws Exception {
        DefaultAnyValue prop = new DefaultAnyValue();
        prop.addValue(DATA_SOURCE_URL, jdbc_url);
        prop.addValue(DATA_SOURCE_USER, jdbc_user);
        prop.addValue(DATA_SOURCE_PASSWORD, jdbc_pwd);
        DataSqlSource source = new DataJdbcSource();
        ((Service) source).init(prop);

        final StringBuilder sb = new StringBuilder();
        final StringBuilder tostring = new StringBuilder();
        final StringBuilder tableComment = new StringBuilder();
        final Map<String, String> uniques = new HashMap<>();
        final Map<String, String> indexs = new HashMap<>();
        final List<String> columns = new ArrayList<>();
        final Set<String> superColumns = new HashSet<>();
        source.nativeQuery("SHOW CREATE TABLE " + classname.toLowerCase(), (DataResultSet tcs) -> {
            try {
                tcs.next();
                final String createsql = (String) tcs.getObject(2);
                for (String str : createsql.split("\n")) {
                    str = str.trim();
                    if (str.startsWith("`")) {
                        str = str.substring(str.indexOf('`') + 1);
                        columns.add(str.substring(0, str.indexOf('`')));
                    } else if (str.startsWith("UNIQUE KEY ")) {
                        str = str.substring(str.indexOf('`') + 1);
                        uniques.put(str.substring(0, str.indexOf('`')), str.substring(str.indexOf('(') + 1, str.indexOf(')')));
                    } else if (str.startsWith("KEY ")) {
                        str = str.substring(str.indexOf('`') + 1);
                        indexs.put(str.substring(0, str.indexOf('`')), str.substring(str.indexOf('(') + 1, str.indexOf(')')));
                    }
                }
                int pos = createsql.indexOf("COMMENT='");
                if (pos > 0) {
                    tableComment.append(createsql.substring(pos + "COMMENT='".length(), createsql.lastIndexOf('\'')));
                } else {
                    tableComment.append("");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        });

        if (superclassname != null && !superclassname.isEmpty()) {
            source.nativeQuery("SELECT * FROM information_schema.columns WHERE  table_name = '" + superclassname.toLowerCase() + "'", (DataResultSet rs) -> {
                try {
                    while (rs.next()) {
                        superColumns.add((String) rs.getObject("COLUMN_NAME"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "";
            });
        }
        source.nativeQuery("SELECT * FROM information_schema.columns WHERE  table_name = '" + classname.toLowerCase() + "'", (DataResultSet rs) -> {
            try {
                sb.append("package " + pkg + ";" + "\r\n\r\n");
                sb.append("import org.redkale.persistence.*;\r\n");
                //sb.append("import org.redkale.util.*;\r\n");
                if (superclassname == null || superclassname.isEmpty()) {
                    try {
                        Class.forName("org.redkale.demo.base.BaseEntity");
                        sb.append("import org.redkale.demo.base.BaseEntity;\r\n");
                    } catch (Throwable t) {
                        sb.append("import org.redkale.convert.json.*;\r\n");
                        tostring.append("\r\n    @Override\r\n    public String toString() {\r\n");
                        tostring.append("        return JsonConvert.root().convertTo(this);\r\n");
                        tostring.append("    }\r\n");
                    }
                }
                sb.append("\r\n/**\r\n"
                    + " *\r\n"
                    + " * @author " + System.getProperty("user.name") + "\r\n"
                    + " */\r\n");
                //if (classname.contains("Info")) sb.append("@Cacheable\r\n");
                sb.append("@Table(comment = \"" + tableComment + "\"");
                if (!uniques.isEmpty()) {
                    sb.append("\r\n        , uniqueConstraints = {");
                    boolean first = true;
                    for (Map.Entry<String, String> en : uniques.entrySet()) {
                        if (!first) {
                            sb.append(", ");
                        }
                        sb.append("@UniqueConstraint(name = \"" + en.getKey() + "\", columnNames = {" + en.getValue().replace('`', '"') + "})");
                        first = false;
                    }
                    sb.append("}");
                }
                if (!indexs.isEmpty()) {
                    sb.append("\r\n        , indexes = {");
                    boolean first = true;
                    for (Map.Entry<String, String> en : indexs.entrySet()) {
                        if (!first) {
                            sb.append(", ");
                        }
                        sb.append("@Index(name = \"" + en.getKey() + "\", columnList = \"" + en.getValue().replace("`", "") + "\")");
                        first = false;
                    }
                    sb.append("}");
                }
                sb.append(")\r\n");
                sb.append("public class " + classname
                    + (superclassname != null && !superclassname.isEmpty() ? (" extends " + superclassname) : (tostring.length() == 0 ? " extends BaseEntity" : " implements java.io.Serializable")) + " {\r\n\r\n");
                Map<String, StringBuilder> columnMap = new HashMap<>();
                Map<String, StringBuilder> getsetMap = new HashMap<>();
                while (rs.next()) {
                    String column = (String) rs.getObject("COLUMN_NAME");
                    String type = ((String) rs.getObject("DATA_TYPE")).toUpperCase();
                    String remark = (String) rs.getObject("COLUMN_COMMENT");
                    String def = (String) rs.getObject("COLUMN_DEFAULT");
                    String key = (String) rs.getObject("COLUMN_KEY");
                    StringBuilder fieldsb = new StringBuilder();
                    if (key != null && key.contains("PRI")) {
                        fieldsb.append("    @Id");
                    } else if (superColumns.contains(column)) {  //跳过被继承的重复字段
                        continue;
                    }
                    fieldsb.append("\r\n");

                    int length = 0;
                    int precision = 0;
                    int scale = 0;
                    String ctype = "NULL";
                    String precisionstr = (String) rs.getObject("NUMERIC_PRECISION");
                    String scalestr = (String) rs.getObject("NUMERIC_SCALE");
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
                        precision = precisionstr == null ? 0 : Integer.parseInt(precisionstr);
                        scale = scalestr == null ? 0 : Integer.parseInt(scalestr);
                    } else if ("DOUBLE".equalsIgnoreCase(type)) {
                        ctype = "double";
                        precision = precisionstr == null ? 0 : Integer.parseInt(precisionstr);
                        scale = scalestr == null ? 0 : Integer.parseInt(scalestr);
                    } else if ("VARCHAR".equalsIgnoreCase(type)) {
                        ctype = "String";
                        String maxsize = (String) rs.getObject("CHARACTER_MAXIMUM_LENGTH");
                        length = maxsize == null ? 0 : Integer.parseInt(maxsize);
                    } else if (type.contains("TEXT")) {
                        ctype = "String";
                    } else if (type.contains("BLOB")) {
                        ctype = "byte[]";
                    }
                    fieldsb.append("    @Column(");
                    if ("createTime".equals(column)) {
                        fieldsb.append("updatable = false, ");
                    }
                    if (length > 0) {
                        fieldsb.append("length = ").append(length).append(", ");
                    }
                    if (precision > 0) {
                        fieldsb.append("precision = ").append(precision).append(", ");
                    }
                    if (scale > 0) {
                        fieldsb.append("scale = ").append(scale).append(", ");
                    }
                    fieldsb.append("comment = \"" + remark.replace('"', '\'') + "\")\r\n");

                    fieldsb.append("    private " + ctype + " " + column);
                    if (def != null && !"0".equals(def)) {
                        String d = def.replace('\'', '\"');
                        fieldsb.append(" = ").append(d.isEmpty() ? "\"\"" : d.toString());
                        if ("float".equals(ctype)) {
                            fieldsb.append("f");
                        }
                    } else if ("String".equals(ctype)) {
                        fieldsb.append(" = \"\"");
                    }
                    fieldsb.append(";\r\n");

                    char[] chs2 = column.toCharArray();
                    chs2[0] = Character.toUpperCase(chs2[0]);
                    String sgname = new String(chs2);

                    StringBuilder setgetsb = new StringBuilder();
                    setgetsb.append("\r\n    public void set" + sgname + "(" + ctype + " " + column + ") {\r\n");
                    setgetsb.append("        this." + column + " = " + column + ";\r\n");
                    setgetsb.append("    }\r\n");

                    setgetsb.append("\r\n    public " + ctype + " get" + sgname + "() {\r\n");
                    setgetsb.append("        return this." + column + ";\r\n");
                    setgetsb.append("    }\r\n");
                    columnMap.put(column, fieldsb);
                    getsetMap.put(column, setgetsb);
                }
                List<StringBuilder> list = new ArrayList<>();
                for (String column : columns) {
                    if (superColumns.contains(column)) {
                        continue;
                    }
                    list.add(columnMap.get(column));
                }
                for (String column : columns) {
                    if (superColumns.contains(column)) {
                        continue;
                    }
                    list.add(getsetMap.get(column));
                }
                for (StringBuilder item : list) {
                    sb.append(item);
                }
                sb.append(tostring);
                sb.append("}\r\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        });

        return sb.toString();
    }
}

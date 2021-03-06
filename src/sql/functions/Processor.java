package sql.functions;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;
import sql.elements.Column;
import sql.elements.Data;
import sql.elements.Database;
import sql.elements.Line;
import sql.elements.Mysql;
import sql.elements.Order;
import sql.elements.Table;
import sql.exceptions.CommandDeniedException;
import sql.exceptions.DataInvalidException;
import sql.exceptions.IsExistedException;
import sql.exceptions.NotAlterException;
import sql.exceptions.NotFoundException;
import sql.exceptions.NotNullException;
import sql.exceptions.TooLongException;
import sql.exceptions.UnknownSequenceException;
import sql.exceptions.WrongCommandException;

public class Processor {

    static Mysql sql = Mysql.getInstance();
    Database database;
    ObjectInputStream reader;
    ObjectOutputStream writer;

    public Processor(ObjectInputStream reader, ObjectOutputStream writer) {
        this.reader = reader;
        this.writer = writer;
    }

    static boolean notCompare(@NotNull String a, String b) {
        return !a.equalsIgnoreCase(b);
    }

    private String getLine() throws IOException {
        writer.writeObject("waiting");
        try {
            return (String) reader.readObject();
        } catch (ClassNotFoundException ignored) {
        }
        return "";
    }

    @NotNull
    private String[] removeNull(@NotNull String[] s) {
        ArrayList<String> ss = new ArrayList<>();
        for (String str : s) {
            if (!str.equals("")) {
                ss.add(str);
            }
        }
        return ss.toArray(new String[0]);
    }

    public String process() {
        String cmd = "";
        try {
            cmd = this.getLine();
            if (!cmd.equals("")) {
                String[] sp = cmd.split(" ");
                sp = removeNull(sp);
                sp[0] = sp[0].toUpperCase();
                switch (sp[0]) {
                    case "ALTER":
                        alter(sp);
                        break;
                    case "SELECT":
                        select(cmd);
                        break;
                    case "DELETE":
                        delete(sp);
                        break;
                    case "DROP":
                        drop(sp);
                        break;
                    case "UPDATE":
                        update(sp);
                        break;
                    case "INSERT":
                        insert(sp);
                        break;
                    case "CREATE":
                        create(sp);
                        break;
                    case "CREATEINDEX":
                        createIndex(sp);
                        break;
                    case "ADD":
                        add(sp);
                        break;
                    case "SAVE":
                        sql.save();
                        break;
                    default:
                        throw new WrongCommandException("超出指令范围");
                }
                writer.writeObject("done\n");
            }
        } catch (Exception e) {
            try {
                writer.writeObject(e.getMessage() + "\n");
                e.printStackTrace();
            } catch (Exception ignored) {
            }
        }
        return cmd;
    }

    //更改
    private void alter(@NotNull String[] s)
        throws WrongCommandException, NotAlterException, NotFoundException, IsExistedException {
        //ALTER TABLE table_name (MODIFY NAME = new_tbname)
        //ALTER DATABASE database_name (MODIFY NAME = new_dbname)
        //alter table 表名 rename column 原列名 to 新列名
        if ((!s[1].equalsIgnoreCase("TABLE") && !s[1].equalsIgnoreCase("DATABASE"))) {
            throw new WrongCommandException("ATLER:请对TABLE和DATABASE操作");
        }
        if (s.length != 3 && s.length != 7 && s.length != 8) {
            throw new WrongCommandException("ALTER:指令长度不合法，请注意空格位置");
        }
        if (s.length == 7 && (!s[3].equalsIgnoreCase("MODIFY") || !s[5].equals("=") || !s[4]
            .equalsIgnoreCase("NAME"))) {
            throw new WrongCommandException("ALTER:请注意空格位置");
        }
        if (s[1].equalsIgnoreCase("TABLE")) {
            if (database == null) {
                throw new NotAlterException();
            } else {
                database.alterTable(s[2]);
            }
            if (s.length == 7) {
                database.changeTableName(s[2], s[6]);
            }
            if (s.length == 8) {
                if (!s[3].equalsIgnoreCase("RENAME") || !s[4].equalsIgnoreCase("COLUMN") || !s[6]
                    .equalsIgnoreCase("TO")) {
                    throw new WrongCommandException("修改列名指令出错");
                }
                Table table = database.choosingTable;
                table.changeColumnName(s[5], s[7]);
            }
        }
        if (s[1].equalsIgnoreCase("DATABASE")) {
            database = sql.getDatabase(s[2]);
            if (database == null) {
                throw new NotFoundException("database", s[2]);
            }
            if (s.length == 7) {
                database.changeName(s[6]);
            }
        }
    }

    //向表中添加列
    private void add(@NotNull String[] s)
        throws WrongCommandException, NotFoundException, IsExistedException {
        ////ALTER TABLE table_name
        //ADD column_name datatype
        if ((s.length != 3 && s.length != 5)) {
            throw new WrongCommandException("ADD:指令长度不合法，请注意空格位置");
        }
        if ((s.length == 5 && (!s[3].equalsIgnoreCase("NOT")
            || !s[4].equalsIgnoreCase("NULL")))) {
            throw new WrongCommandException("ADD:请检查指令NOT NULL");
        }
        Column col = new Column(s[1], s[2], s.length != 5);
        database.choosingTable.addColumns(new ArrayList<>(Collections.singletonList(col)));
    }

    public void select(String s)
        throws WrongCommandException, NotAlterException, UnknownSequenceException, DataInvalidException, NotFoundException {
        //SELECT 列名称 FROM 表名称 WHERE 列 运算符= 值 ORDER BY 列名 ASC/DESC,列名 ASC/DESC
        //规定指令中ASC和DESC不可省略
        //例：
        //SELECT Company,OrderNumber/* FROM Orders WHERE 列 = 值
        //SELECT Company,OrderNumber FROM Orders ORDER BY Company DESC,OrderNumber ASC
        //SELECT Company,OrderNumber FROM Orders WHERE 列 = 值 ORDER BY Company DESC,OrderNumber ASC
        ArrayList<Column> myWhere = null;
        ArrayList<Line> lines = new ArrayList<>();
        if (database == null) {
            throw new NotAlterException();
        }
        String[] sp = s
            .split("SELECT | FROM | WHERE | ORDER BY |select | from | where | order by ");
        sp = removeNull(sp);
        Table table = database.getTable(sp[1]);
        String[] sp1 = s.split(" ");
        sp1 = removeNull(sp1);
        boolean hasORDER = false, hasWHERE = false;
        for (String value : sp1) {
            if (value.equalsIgnoreCase("ORDER")) {
                hasORDER = true;
            }
            if (value.equalsIgnoreCase("WHERE")) {
                hasWHERE = true;
            }
            if (hasORDER && hasWHERE) {
                break;
            }
        }
        if (sp[0].equals("*")) {
            //ArrayList<Line> selectAll(String table, Order[] where, Order[] orderby)
            if (sp.length == 3) {//WHERE和ORDER BY只有一个
                if (hasORDER && hasWHERE) {
                    throw new WrongCommandException("SELECT:1");
                }
                if (hasORDER) {
                    //SELECT * FROM 表名 ORDER BY Company DESC,OrderNumber ASC
                    ArrayList<Order> orderby = new ArrayList<>();
                    String[] orderbys = sp[2].split("[ ,]");
                    orderbys = removeNull(orderbys);
                    if (orderbys.length % 2 != 0) {
                        throw new WrongCommandException("SELECT2");
                    }
                    for (int i = 0; i < orderbys.length - 1; i += 2) {
                        String ord = "1";
                        if (orderbys[i + 1].equalsIgnoreCase("DESC")) {
                            ord = "-1";
                        }
                        orderby.add(new Order(table, orderbys[i], ord));
                    }
                    lines = database.selectAll(sp[1], null, orderby);
                }
                if (hasWHERE) {
                    //SELECT * FROM Orders WHERE 列 = 值
                    String[] wheres = sp[2].split("[ =]");
                    wheres = removeNull(wheres);
                    ArrayList<Order> where = new ArrayList<>();
                    if (wheres.length % 2 != 0) {
                        throw new WrongCommandException("SELECT3");
                    }
                    for (int i = 0; i < wheres.length; i += 2) {
                        where.add(new Order(table, wheres[i], wheres[i + 1]));
                    }
                    lines = database.selectAll(sp[1], where, null);
                }
            } else if (sp.length == 4) {//WHERE和ORDER BY都有
                if (!hasWHERE || !hasORDER) {
                    throw new WrongCommandException("SELECT*");
                }
                ArrayList<Order> orderby = new ArrayList<>();

                String[] orderbys = sp[3].split("[ ,]");
                orderbys = removeNull(orderbys);
                if (orderbys.length % 2 != 0) {
                    throw new WrongCommandException("SELECT4");
                }
                for (int i = 0; i < orderbys.length - 1; i += 2) {
                    String ord = "1";
                    if (orderbys[i + 1].equalsIgnoreCase("DESC")) {
                        ord = "-1";
                    }
                    orderby.add(new Order(table, orderbys[i], new Data(ord)));
                }
                String[] wheres = sp[2].split("[ =]");
                wheres = removeNull(wheres);
                ArrayList<Order> where = new ArrayList<>();
                if (wheres.length % 2 != 0) {
                    throw new WrongCommandException("SELECT5");
                }
                for (int i = 0; i < wheres.length; i += 2) {
                    where.add(new Order(table, wheres[i], wheres[i + 1]));
                }
                lines = database.selectAll(sp[1], where, orderby);
            } else if (sp.length == 2) {
                if (hasORDER || hasWHERE) {
                    throw new WrongCommandException("SELECT6");
                }
                lines = database.selectAll(sp[1], null, null);
            } else {
                throw new WrongCommandException("SELECT7");
            }
        } else {
            //ArrayList<Line> select(String table, Column[] columns, Order[] where, Order[] orderBy)
            // SELECT Company,OrderNumber FROM Orders WHERE 列 = 值 ORDER BY Company DESC,OrderNumber ASC
            String[] colName = sp[0].split(",");
            colName = removeNull(colName);
            ArrayList<Column> cols = new ArrayList<>();
            for (String value : colName) {
                cols.add(table.getColumn(value));
            }
            myWhere = cols;
            if (sp.length == 3) {//WHERE和ORDER BY只有一个
                if (hasORDER && hasWHERE) {
                    throw new WrongCommandException("SELECT8");
                }
                if (hasORDER) {
                    //SELECT * FROM 表名 ORDER BY Company DESC,OrderNumber ASC
                    ArrayList<Order> orderby = new ArrayList<>();
                    String[] orderbys = sp[2].split("[ ,]");
                    orderbys = removeNull(orderbys);
                    if (orderbys.length % 2 != 0) {
                        throw new WrongCommandException("SELECT9");
                    }
                    for (int i = 0; i < orderbys.length - 1; i += 2) {
                        String ord = "1";
                        if (orderbys[i + 1].equalsIgnoreCase("DESC")) {
                            ord = "-1";
                        }
                        orderby.add(new Order(table, orderbys[i], ord));
                    }
                    lines = database
                        .select(sp[1], cols, null, orderby);
                }
                if (hasWHERE) {
                    //SELECT * FROM Orders WHERE 列 = 值
                    String[] wheres = sp[2].split("[ =]");
                    wheres = removeNull(wheres);
                    ArrayList<Order> where = new ArrayList<>();
                    if (wheres.length % 2 != 0) {
                        throw new WrongCommandException("SELECT10");
                    }
                    for (int i = 0; i < wheres.length; i += 2) {
                        where.add(new Order(table, wheres[i], wheres[i + 1]));
                    }
                    lines = database
                        .select(sp[1], cols, where, null);
                }
            } else if (sp.length == 4) {//WHERE和ORDER BY都有
                if (!hasORDER || !hasWHERE) {
                    throw new WrongCommandException("SELECT11");
                }
                ArrayList<Order> orderby = new ArrayList<>();
                String[] orderbys = sp[3].split("[ ,]");
                orderbys = removeNull(orderbys);
                if (orderbys.length % 2 != 0) {
                    throw new WrongCommandException("SELECT12");
                }
                for (int i = 0; i < orderbys.length - 1; i += 2) {
                    String ord = "1";
                    if (orderbys[i + 1].equalsIgnoreCase("DESC")) {
                        ord = "-1";
                    }
                    orderby.add(new Order(table, orderbys[i], ord));
                }
                String[] wheres = sp[2].split("[ =]");
                wheres = removeNull(wheres);
                ArrayList<Order> where = new ArrayList<>();
                if (wheres.length % 2 != 0) {
                    throw new WrongCommandException("SELECT13");
                }
                for (int i = 0; i < wheres.length; i += 2) {
                    where.add(new Order(table, wheres[i], wheres[i + 1]));
                }
                lines = database
                    .select(sp[1], cols, where, orderby);
            } else if (sp.length == 2) {
                if (hasORDER || hasWHERE) {
                    throw new WrongCommandException("SELECT16");
                }
                lines = database.select(sp[1], cols, null, null);
            } else {
                throw new WrongCommandException("SELECT17");
            }
        }
        printLines(lines, table, myWhere);
        //ArrayList<String> colNames = table.getColumnNames();
    }

    private void printLines(@NotNull ArrayList<Line> lines, @NotNull Table table,
        ArrayList<Column> where) {
        ArrayList<String> strings = table.getColumnNames(where);
        StringBuilder stringBuilder = new StringBuilder();
        for (Line line : lines) {
            for (String string : strings) {
                stringBuilder.append("\t" + string);
                stringBuilder.append(":");
                stringBuilder
                    .append(line.data.get(table.getColumn(string).id).getValue());
                stringBuilder.append("; ");
            }
            stringBuilder.append("\n");
        }
        try {
            writer.writeObject(stringBuilder.toString());
        } catch (Exception ignored) {
        }
    }

    //删除表中的行  DELETE FROM 表名称 WHERE 列名称 = 值
    private void delete(@NotNull String[] s)
        throws NotAlterException, NotFoundException, WrongCommandException, DataInvalidException {
        if (s.length != 7 || notCompare(s[1], "FROM") || notCompare(s[3], "WHERE") || !s[5]
            .equals("=")) {
            throw new WrongCommandException("DELETE");
        }
        if (database == null) {
            throw new NotAlterException();
        }
        Table table = database.getTable(s[2]);
        ArrayList<Order> orders = new ArrayList<>();
        for (int i = 4; i < s.length; i++) {
            if (s[i].equals("=")) {
                orders.add(new Order(table, s[i - 1], s[i + 1]));
            }
        }
        table.deleteLine(orders);
    }

    private void update(String[] s)
        throws NotAlterException, WrongCommandException, DataInvalidException, NotFoundException {
        //UPDATE 表名称 SET 列名称 = 新值 WHERE 列名称 = 某值
        if (database == null) {
            throw new NotAlterException();
        }
        if (s.length != 10) {
            throw new WrongCommandException("DELETE");
        }
        if (!s[2].equalsIgnoreCase("SET") || !s[4].equals("=") || !s[8].equals("=") || !s[6]
            .equalsIgnoreCase("WHERE")) {
            throw new WrongCommandException("DELETE");
        }
        Table table = database.getTable(s[1]);
        ArrayList<Order> search = new ArrayList<>(
            Collections.singletonList(new Order(table, s[7], s[9])));
        ArrayList<Order> update = new ArrayList<>(
            Collections.singletonList(new Order(table, s[3], s[5])));
        table.update(update, search);
    }

    //删除库、表、列
    private void drop(@NotNull String[] s)
        throws WrongCommandException, NotAlterException, NotFoundException, CommandDeniedException {
        //DROP TABLE 表名称
        //DROP DATABASE 数据库名称
        //ALTER TABLE table_name
        //DROP COLUMN column_name
        if (s.length != 3 || (!s[1].equalsIgnoreCase("TABLE") && !s[1].equalsIgnoreCase("DATABASE")
            && !s[1].equalsIgnoreCase("COLUMN"))) {
            throw new WrongCommandException("DROP");
        }
        if (s[1].equalsIgnoreCase("DATABASE")) {
            sql.deleteDatabase(s[2]);
        }
        if (s[1].equalsIgnoreCase("TABLE")) {
            if (database == null) {
                throw new NotAlterException();
            }
            database.deleteTable(s[2]);
        }
        if (s[1].equalsIgnoreCase("COLUMN")) {
            if (database.choosingTable == null) {
                throw new NotAlterException();
            }
            database.choosingTable.deleteColumn(s[2]);
        }
    }

    //插入行
    private void insert(String[] s)
        throws NotAlterException, WrongCommandException, DataInvalidException, TooLongException, NotFoundException, NotNullException {
        //INSERT INTO 语句用于向表格中插入新的行。
        //INSERT INTO 表名称 VALUES 值1,值2,....
        //INSERT INTO 表名称 列1,列2,... VALUES 值1,值2,....//(指定列)
        if (database == null) {
            throw new NotAlterException();
        }
        if (s.length != 5 && s.length != 6) {
            throw new WrongCommandException("INSERT");
        }
        Table table = database.getTable(s[2]);
        ArrayList<Order> orders = new ArrayList<>();
        if (s.length == 5) {
            if (!s[1].equalsIgnoreCase("INTO") || !s[3].equalsIgnoreCase("VALUES")) {
                throw new WrongCommandException("INSERT");
            } else {
                ArrayList<String> colNames = table.getColumnNames(null);
                String[] values = s[4].split(",");
                values = removeNull(values);
                if (colNames.size() != values.length) {
                    throw new WrongCommandException("INSERT");
                }
                for (int i = 0; i < colNames.size(); i++) {
                    orders.add(new Order(table, colNames.get(i), values[i]));
                }
            }
        }
        if (s.length == 6) {
            if (!s[1].equalsIgnoreCase("INTO") || !s[4].equalsIgnoreCase("VALUES")) {
                throw new WrongCommandException("INSERT");
            } else {
                String[] colNames = s[3].split(",");
                colNames = removeNull(colNames);
                String[] values = s[5].split(",");
                values = removeNull(values);
                if (colNames.length != values.length) {
                    throw new WrongCommandException("INSERT");
                }
                for (int i = 0; i < values.length; i++) {
                    orders.add(new Order(table, colNames[i], values[i]));
                }
            }
        }
        table.insertByOrders(orders);
    }

    //建库、表
    private void create(@NotNull String[] s)
        throws WrongCommandException, NotAlterException, IsExistedException, NotFoundException, IOException {
        //CREATE DATABASE database_name
        //CREATE TABLE 表名称
        //(
        //列名称1 数据类型 NOT NULL
        //列名称2 数据类型 String/Number/Integer/CardID/Data/Time
        //列名称3 数据类型
        //....
        //)
        if (s.length != 3 || (!s[1].equalsIgnoreCase("TABLE") && !s[1]
            .equalsIgnoreCase("DATABASE"))) {
            throw new WrongCommandException("CREATE");
        }
        if (s[1].equalsIgnoreCase("DATABASE")) {//创建新的数据库
            sql.newDatabase(s[2]);
        }
        if (s[1].equalsIgnoreCase(("TABLE"))) {//创建新表
            if (database == null) {
                throw new NotAlterException();
            }
            ArrayList<Column> cols = new ArrayList<>();
            this.getLine();
            String str = this.getLine();
            while (!str.equals(")")) {
                String[] sp = str.split(" ");
                sp = removeNull(sp);
                if (sp.length != 4 && sp.length != 2) {
                    throw new WrongCommandException("CREATE");
                }
                if (sp.length == 4 && (!sp[2].equalsIgnoreCase("NOT") || !sp[3]
                    .equalsIgnoreCase("NULL"))) {
                    throw new WrongCommandException("CREATE");
                }

                boolean canNull = (sp.length != 4);
                cols.add(new Column(sp[0], sp[1], canNull));
                str = this.getLine();
            }
            // TODO: 2019/12/21 处理index[]
            database.newTable(s[2], cols, null, false);
        }
    }

    private void createIndex(@NotNull String[] sp)
        throws WrongCommandException, NotFoundException, IsExistedException {
        //CREATEINDEX UNIQUE index_name ON table_name column_name
        //CREATEINDEX index_name ON table_name column_name
        if (sp.length != 5 && sp.length != 6) {
            throw new WrongCommandException("CREATEINDEX:1");
        }
        if (sp.length == 5) {
            //CREATEINDEX index_name ON table_name column_name
            if (!sp[2].equalsIgnoreCase("ON")) {
                throw new WrongCommandException("CREATEINDEX:2");
            }
            Table table = database.getTable(sp[3]);
            String[] colNames = sp[4].split(",");
            colNames = removeNull(colNames);
            ArrayList<String> cols = new ArrayList<>(Arrays.asList(colNames));
            table.setIndexByStrings("default", sp[1], cols);
        } else {
            if (!sp[1].equalsIgnoreCase("UNIQUE") || !sp[3].equalsIgnoreCase("ON")) {
                throw new WrongCommandException("CREATEINDEX:3");
            }
            Table table = database.getTable(sp[4]);
            String[] colNames = sp[5].split(",");
            colNames = removeNull(colNames);
            ArrayList<String> cols = new ArrayList<>(Arrays.asList(colNames));
            table.setIndexByStrings("unique", sp[2], cols);
        }
    }

    public String getUser() {
        return sql.getUserUsing();
    }
}

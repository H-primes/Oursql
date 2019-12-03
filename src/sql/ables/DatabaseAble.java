package sql.ables;

import java.util.ArrayList;
import sql.elements.Column;
import sql.elements.Line;
import sql.elements.Order;
import sql.exceptions.IsExistedException;
import sql.exceptions.NotFoundException;

public interface DatabaseAble {

    ArrayList<Line> select(String table, Column[] columns, Order[] where, Order[] orderBy)
        throws Exception;

    void changeTableName(String oldOne, String newOne) throws NotFoundException, IsExistedException;

    void newTable(String name, Order[] columns, Order index) throws IsExistedException;

    void deleteTable(String name) throws NotFoundException;
}

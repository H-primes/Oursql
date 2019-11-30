package sql.element;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import sql.ables.TableAble;
import sql.exceptions.IsExistedException;
import sql.exceptions.NotFoundException;

import java.util.*;

public class Table implements TableAble {
    public String name;
    private int index_count = 0;
    private int column_count = 0;
    private ArrayList<Column> columnList = new ArrayList<>();
    private ArrayList<Line> data;
    @Contract(pure = true)
    Table(String name) {
        this.name = name;
        this.data = new ArrayList<>();
    }

    /**
     * get the column list
     * @return ArrayList<Column>
     */
    public ArrayList<Column> getColumnList() {
        return columnList;
    }

    Column getColumn(String name) {
        for(Column x: columnList) {
            if(x.name.equals(name)) return x;
        }
        return null;
    }

    public void insertList(Column[] str){
        this.columnList.addAll(Arrays.asList(str));
    }

    private void insert(@NotNull Data[] str) throws Exception {
        if(str.length != this.columnList.size()) {
            throw new Exception("Length is not correct. Expect " + this.columnList.size() + " , found " + str.length + ".");
        }
        Line new_data = new Line();
        new_data.index = index_count++;
        new_data.data.addAll(Arrays.asList(str));
        this.data.add(new_data);
    }

    @Override
    public void addColumn(Column column) throws IsExistedException {
        Column x = this.getColumn(column.name);
        if(x != null) throw new IsExistedException("column", column.name);
        this.columnList.add(column);
    }

    @Override
    public void deleteColumn(String name) throws NotFoundException {
        Column x = this.getColumn(name);
        if(x == null) throw new NotFoundException("column", name);
        this.columnList.remove(x);
    }

    public void insert(@NotNull Order[] orders) {
        Line new_data = new Line();
        new_data.index = index_count++;
        for(Order x: orders) {
            new_data.data.add(x.column.id, x.value);
        }
        data.add(new_data);
    }
    @NotNull
    private ArrayList<Integer> selectWhereToIndex(@NotNull Order[] where) {
        ArrayList<Integer> result = new ArrayList<>();
        int length = data.size();
        for (int i = 0; i < length; i++) {
            Line x = data.get(i);
            boolean is_equal = true;
            for (Order y : where) {
                int index = y.column.id;
                if(!y.value.getStringValue().equalsIgnoreCase(x.data.get(index).getStringValue())) {
                    is_equal = false;
                    break;
                }
            }
            if(is_equal) {
                result.add(i);
            }
        }
        return result;
    }

    ArrayList<Line> selectPrivate(Column[] columns, Order[] where, Order[] order_by) throws Exception {
        ArrayList<Integer> result = selectWhereToIndex(where);
        ArrayList<Line> array = new ArrayList<>();
        for(int i: result) {
            Line tmp = data.get(i), add = new Line();
            for(Column j : columns) {
                add.data.add(tmp.data.get(j.id));
            }
            if(order_by.length != 0) {
                for (Order j : order_by) {
                    String s = tmp.data.get(j.column.id).getStringValue();
                    int len = s.length();
                    for (int ch = 0; ch < len; ch++) {
                        if (j.value.getStringValue().equals("1")) tmp.cmp += s.charAt(ch);
                        else if (j.value.getStringValue().equals("-1")) tmp.cmp += (char) (65536 - s.charAt(ch));
                        else throw new Exception("Unknown sequence.");
                    }
                }
            }
            array.add(add);
        }
        if(order_by.length != 0) {
            Collections.sort(array);
            for(int j = 1; j < array.size(); j++) {
                if(array.get(j - 1).equals(array.get(j))) {
                    array.remove(j - 1);
                }
            }
        }
        return array;
    }
    public void update(@NotNull Order[] set, @NotNull Order[] where) {
        ArrayList<Integer> result= selectWhereToIndex(where);
        for (int x: result) {
            for (Order y : set) {
                this.data.get(x).data.get(y.column.id).setString(y.value.getStringValue());
            }
        }
    }

    @Override
    public void deleteLine(Order[] search) throws NotFoundException {
        ArrayList<Integer> result = selectWhereToIndex(search);
        for(int x: result) {
            this.data.remove(x);
        }
    }

    @Override
    public void setIndex(int type, String[] columnInOrder) throws NotFoundException, IsExistedException {
        
    }

    public void delete(@NotNull Order[] where) throws Exception {
        ArrayList<Integer> result = selectWhereToIndex(where);
        if(result.isEmpty()) throw new Exception("Data not found.");
        for(int x:result) {
            data.remove(x);
        }
    }
    public void addColumn(@NotNull String line) throws Exception {
        String[] arr = line.split(" ");
        if(arr.length < 2) throw new Exception("Massage is too short in adding columns.");
        int max_length = 256;
        boolean is_main_key = false, can_null = false;
        for(int i = 2; i < arr.length; i++) {
            try{
                max_length = Integer.parseInt(arr[i]);
            } catch (Exception e) {
                if(arr[i-1].equalsIgnoreCase("NOT")
                        && arr[i].equalsIgnoreCase("NULL")){
                    can_null = true;
                }
                if(arr[i-1].equalsIgnoreCase("PRIMARY")
                        && arr[i].equalsIgnoreCase("KEY")) {
                    is_main_key = true;
                }
            }
        }
        addColumn(arr[0], arr[1], max_length, is_main_key, can_null);
    }
    private void addColumn(String name, String type, int max_length, boolean is_main_key, boolean can_null) {
        this.columnList.add(new Column(column_count++, name, type, max_length, is_main_key, can_null));
    }
}


package sql.elements;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import sql.ables.OuterAble;
import sql.exceptions.CannotDeleteException;
import sql.exceptions.IsExistedException;
import sql.exceptions.NotFoundException;

public class Mysql implements OuterAble, Serializable {

    transient private static final String defaultUsername = "root";
    transient private static final String defaultPassword = "123456";
    transient private static final String IOFile = "data.db";
    private static HashMap<String, String> passwordList = new HashMap<>();
    transient private static Mysql instance = null;
    transient private String userUsing = null;
    ArrayList<Database> databases = new ArrayList<>();

    private Mysql() {
        passwordList.put(defaultUsername, defaultPassword);
    }

    public static Mysql getInstance() {
        if (instance == null) {
            instance = new Mysql();
        }
        return instance;
    }

    public String getUserUsing() {
        return userUsing;
    }

    @Override
    public void load(File file) throws IOException, ClassNotFoundException {
        FileInputStream fileInputStream = new FileInputStream(IOFile);
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        instance = (Mysql) objectInputStream.readObject();
        objectInputStream.close();
    }

    @Override
    public void save(File file) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(IOFile);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject(instance);
        objectOutputStream.close();
    }

    /**
     * to login. Default user: root 123456
     *
     * @param name     username
     * @param password password
     * @return boolean is successfully login
     */
    @Override
    public boolean login(String name, String password) {
        String passwords = passwordList.get(name);
        if (name == null || password == null) {
            return false;
        }
        if (password.equals(passwords)) {
            userUsing = name;
            return true;
        }
        return false;
    }

    @Override
    public boolean changePassword(String oldOne, String newOne) {
        if (userUsing == null) {
            return false;
        }
        return passwordList.replace(userUsing, oldOne, newOne);
    }

    @Override
    public boolean addUser(String name, String password) throws IsExistedException {
        if (userUsing == null) {
            return false;
        }
        if (passwordList.get(name) != null) {
            throw new IsExistedException("user", name);
        }
        passwordList.put(name, password);
        return true;
    }

    @Override
    public boolean deleteUser(String name) throws NotFoundException, CannotDeleteException {
        if (userUsing == null) {
            return false;
        }
        if (passwordList.get(name) == null) {
            throw new NotFoundException("user", name);
        }
        if (name.equals("root")) {
            throw new CannotDeleteException("user", "the root account cannot be deleted.");
        }
        passwordList.remove(name);
        return true;
    }
}

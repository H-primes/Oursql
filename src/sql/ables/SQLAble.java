package sql.ables;

import sql.exceptions.CannotDeleteException;
import sql.exceptions.IsExistedException;
import sql.exceptions.NotFoundException;

import java.io.File;
import java.io.IOException;

public interface SQLAble {
    default void load(String path) throws IOException, ClassNotFoundException {
        this.load(new File(path));
    }
    default void save(String path) throws IOException {
        this.save(new File(path));
    }
    void load(File file) throws IOException, ClassNotFoundException;
    void save(File file) throws IOException;
    boolean login(String name, String password) throws NotFoundException;
    boolean changePassword(String oldOne, String newOne);
    boolean addUser(String name, String password) throws IsExistedException;
    boolean deleteUser(String name) throws NotFoundException, CannotDeleteException;
}

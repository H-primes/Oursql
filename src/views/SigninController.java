package views;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import sql.elements.Mysql;
import sql.exceptions.IsExistedException;
import sql.exceptions.NotFoundException;

public class SigninController {

    @FXML
    public Button button2;
    private MainApp mainapp;
    private Mysql mysql = Mysql.getInstance();
    @FXML
    private TextField account;
    @FXML
    private TextField password;
    @FXML
    private Button button1;
    private String accountStr = null;
    private String passwordStr = null;

    public SigninController() throws NotFoundException, IsExistedException {
    }

    public void setMainApp(MainApp mainapp) {
        this.mainapp = mainapp;
    }

    public void Init() {
    }

    @FXML
    public void clickSignin(MouseEvent event) throws Exception {
        this.getAccount();
        this.getPassword();
        if (!mysql.login(accountStr, passwordStr)) {
            mainapp.showWrong();
        } else {
            mainapp.showSqlView();
        }
    }

    @FXML
    public void clickForget(MouseEvent event) throws Exception {
        mainapp.showForgetView();
    }

    @FXML
    public void getAccount() {
        accountStr = account.getText();
    }

    @FXML
    public void getPassword() {
        passwordStr = password.getText();
    }


}

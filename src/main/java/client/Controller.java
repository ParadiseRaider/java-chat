package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller {
    @FXML
    TextArea textArea;

    @FXML
    TextField textField;

    @FXML
    Button btn1;

    @FXML
    StackPane upperPanel;

    @FXML
    HBox bottomPanel;

    @FXML
    TextField loginField;

    @FXML
    PasswordField passField;

    @FXML
    TextField nickField;

    @FXML
    ListView<String> clientsList;

    Socket socket;
    DataInputStream in;
    DataOutputStream out;

    final String IP_ADRESS = "localhost";
    final int PORT = 8189;
    private boolean isAuthorized;

    private void setAuthorized(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;
        if (!isAuthorized) {
            upperPanel.setVisible(true);
            upperPanel.setManaged(true);
            bottomPanel.setVisible(false);
            bottomPanel.setManaged(false);
            clientsList.setVisible(false);
            clientsList.setManaged(false);
        } else {
            upperPanel.setVisible(false);
            upperPanel.setManaged(false);
            bottomPanel.setVisible(true);
            bottomPanel.setManaged(true);
            clientsList.setVisible(true);
            clientsList.setManaged(true);
        }
    }

    public void tryToAuth(ActionEvent actionEvent) {
        if (socket==null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF("/auth " + loginField.getText()+" "+ passField.getText());
            loginField.clear();
            passField.clear();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void tryToReg() {
        if (socket==null || socket.isClosed()) {
            connect();
        }

        try {
            Pattern p1 = Pattern.compile("^\\S+$");
            Pattern ps = Pattern.compile("^[a-zA-Z]\\S+$");
            Matcher m_login = p1.matcher(loginField.getText());
            Matcher m_pass = ps.matcher(passField.getText());
            Matcher m_nick = p1.matcher(nickField.getText());
            if (m_login.matches() && m_pass.matches() && m_nick.matches()) {
                out.writeUTF("/regin " + loginField.getText() + " " + passField.getText() + " " + nickField.getText());
                loginField.clear();
                passField.clear();
                nickField.clear();
            } else {
                out.writeUTF("/err");
                loginField.clear();
                passField.clear();
                nickField.clear();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void connect() {
        try {
            socket = new Socket(IP_ADRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            String str = in.readUTF();
                            if (str.equalsIgnoreCase("/authok")) {
                                setAuthorized(true);
                                break;
                            } else {
                                textArea.appendText(str+"\n");
                            }
                        }

                        while (true) {
                            String str = in.readUTF();
                            if (str.startsWith("/")) {
                                if (str.equalsIgnoreCase("/clientClose")) break;
                                if (str.startsWith("/clear")) textArea.clear();
                                if (str.startsWith("/clientslist")) {
                                    String[] tokens = str.split(" ");
                                    Platform.runLater(() -> {
                                        clientsList.getItems().clear();
                                        for (int i = 1; i < tokens.length; i++) {
                                            clientsList.getItems().add(tokens[i]);
                                        }
                                    });
                                }
                            } else {
                                textArea.appendText(str + "\n");
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        setAuthorized(false);
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg() {
        try {
            out.writeUTF(textField.getText());
            textField.clear();
            textField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closing() {
        try {
            if (socket!=null) {
                out.writeUTF("/end");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

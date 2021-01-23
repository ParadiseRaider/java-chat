package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ClientHandler {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private MainServ serv;
    private String nick;
    private String login;
    private List<String> blacklist;

    public ClientHandler(MainServ serv, Socket socket) {
        try {
            this.serv = serv;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.blacklist = new ArrayList<>();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            String str = in.readUTF();
                            if (str.startsWith("/err")) {
                                sendMsg("Ошибка ввода данных.\nВсе данные должны быть без пробелов\nПароль должен начинаться с латинской буквы");
                                MainServ.slog.error("Не удалось зарегестрироваться, некорректный ввод данных");
                            }
                            if (str.startsWith("/regin")) {
                                String[] tokens = str.split(" ");
                                String currentNick = AuthService.getNickByLogin(tokens[1]);
                                if (currentNick==null) {
                                    AuthService.registerPerson(tokens[1],tokens[2],tokens[3]);
                                    nick = tokens[3];
                                    login = tokens[1];
                                    serv.clientsOn(ClientHandler.this);
                                    MainServ.slog.info("Зарегестрировался новый пользователь: "+nick);
                                    sendMsg("/authok");
                                    break;
                                } else {
                                    MainServ.slog.warn("Ошибка регистрации, пользователь уже существует");
                                    sendMsg("Такой пользователь уже есть");
                                }
                            }
                            if (str.startsWith("/auth")) {
                                String[] tokens = str.split(" ");
                                String currentNick = AuthService.getNickByLoginAndPass(tokens[1], tokens[2]);
                                if (currentNick != null) {
                                    if (!serv.isConnect(tokens[1])) {
                                        sendMsg("/authok");
                                        nick = currentNick;
                                        login = tokens[1];
                                        serv.clientsOn(ClientHandler.this);
                                        blacklist = AuthService.LoadBlackList(nick);
                                        sendMsg(AuthService.loadHistoryMsg());
                                        MainServ.slog.info("Пользователь: "+nick+" подключился");
                                        break;
                                    } else {
                                        sendMsg("данный пользователь уже есть в сети");
                                        MainServ.slog.warn("Ошибка попытка зайти под пользователем который уже есть в чате");
                                    }
                                } else {
                                    sendMsg("неверный логин/пароль");
                                    MainServ.slog.warn("Ошибка не верный ввод пароля/логина");
                                }
                            }
                        }

                        while (true) {
                            String str = in.readUTF();
                            if (str.startsWith("/")) {
                                if (str.startsWith("/w")) {
                                    String[] tokens = str.split(" ");
                                    serv.personalMsg(nick, tokens[1], str);
                                }
                                if (str.equalsIgnoreCase("/end")) {
                                    sendMsg("/clear");
                                    sendMsg("/clientClose");
                                    break;
                                }
                                if (str.startsWith("/blacklist")) {
                                    String[] tokens = str.split(" ");
                                    if (AuthService.addBlackList(nick, tokens[1])) {
                                        blacklist.add(tokens[1]);
                                        sendMsg("Вы добавили пользователя " + tokens[1] + " в черный список");
                                        MainServ.slog.info("Пользователь: "+nick+" добавил в черный список "+tokens[1]);
                                    } else {
                                        sendMsg("Ошибка добавления в черный список. \nДанного пользователя нет в БД или вы пытаетесь добавить сами себя.");
                                    }
                                }
                            } else {
                                long thistime = System.currentTimeMillis();
                                SimpleDateFormat dateFormat = new SimpleDateFormat("[HH:mm:ss]");
                                String datemsg = dateFormat.format(new Date(thistime));
                                String resmsg = datemsg + " " + nick + ": " + str;
                                AuthService.historyMsg(thistime, resmsg);
                                serv.broadcastMsg(ClientHandler.this, resmsg);
                                MainServ.slog.info("Отправлено сообщение "+resmsg);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } finally {
                        System.out.println(socket+" отключился");
                        MainServ.slog.info("Клиент "+socket+" отключился");
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        serv.clientDisconnet(ClientHandler.this);
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkBlacklist(String nick) {
        return blacklist.contains(nick);
    }

    public String getLogin() {
        return login;
    }

    public String getNick() {
        return nick;
    }
}

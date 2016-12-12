import javax.swing.*;
import java.awt.*;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import javax.imageio.ImageIO;

/**
 * Created by apple on 16/11/26.
 */
public class Game extends JPanel implements MouseListener{

    //game
    private int gameState = 0;  //1: you turn, 2: opponent's turn, 3: end
    private boolean youWin;
    private int[][] map;
    private int line = -1;
    private int num = 0;

    //size
    private final int blockX = 88;
    private final int blockY = 92;
    private final int leftTopX = 27;
    private final int leftTopY = 99;

    //socket
    private ServerSocket server;
    private Socket socket;
    private BufferedReader is;
    private PrintWriter os;

    //ui
    private Image background, logo;
    private Image circle, cross;
    private Image hline, vline, diagonal1, diagonal2;
    private String message = "";

    //thread
    connectThread connectThread;

    public Game(int port){
        super();
        setSize(320, 480);
        loadImage();
        initGameData();
        addMouseListener(this);
        message = "Please wait...";
        connectThread = new connectThread(port);
        connectThread.start();
    }

    @Override
    public void mouseClicked(MouseEvent e){
        int x = e.getX();
        int y = e.getY();
        if(gameState == 3){
            reset();
            return;
        }
        if(x < leftTopX || x > leftTopX + 3 * blockX ||
                y < leftTopY || y > leftTopY + 3 * blockY) {
            return;
        }
        int ix = (y - leftTopY) / blockY;
        int iy = (x - leftTopX) / blockX;
        if(map[ix][iy] != 0)
            return;
        if(gameState == 1){
            map[ix][iy] = 1;
            gameState = 2;
            num++;
            message = "Opponent's Turn";
            os.println(ix);
            os.println(iy);
            os.flush();
            repaint();
            judgeGame();
        }
    }

    @Override
    protected void paintComponent(Graphics g){
        g.drawImage(background, 0, 0, null);
        g.drawImage(logo, background.getWidth(null)/2 - logo.getWidth(null)/2, 10, null);

        for(int i = 0; i < 3; ++i)
            for(int j = 0; j < 3; ++j)
            {
                if(map[i][j] == 1){
                    g.drawImage(circle, leftTopX + j * blockX + 9,
                            leftTopY + i * blockY + 9, null);
                }
                else if(map[i][j] == 2){
                    g.drawImage(cross, leftTopX + j * blockX + 9,
                            leftTopY + i * blockY + 9, null);
                }
            }

        if(!message.isEmpty()){
            g.setFont(new Font("Times New Roman", 0, 45));
            g.drawString(message, this.getWidth()/2 - g.getFontMetrics().stringWidth(message)/2, 442);
        }

        if(line != -1){
            switch(line){
                case 1: g.drawImage(hline,  leftTopX + 5, leftTopY + 30, null); break;
                case 2: g.drawImage(hline, leftTopX + 5, leftTopY + blockY + 30, null); break;
                case 3: g.drawImage(hline, leftTopX + 5, leftTopY + blockY*2 + 30, null); break;
                case 4: g.drawImage(vline, leftTopX + 30, leftTopY + 13, null); break;
                case 5: g.drawImage(vline, leftTopX + blockX + 30, leftTopY + 13, null); break;
                case 6: g.drawImage(vline, leftTopX + blockX*2 + 30, leftTopY + 13, null); break;
                case 7: g.drawImage(diagonal1, leftTopX + 32, leftTopY + 44, null); break;
                case 8: g.drawImage(diagonal2, leftTopX + 30, leftTopY + 40, null); break;
            }
        }
    }

    private void loadImage(){
        background = getImage("/res/background.jpg");
        logo = getImage("/res/logo.png");
        circle = getImage("/res/circle.png");
        cross = getImage("/res/cross.png");
        hline = getImage("/res/hline.png");
        vline = getImage("/res/vline.png");
        diagonal1 = getImage("/res/diagonal1.png");
        diagonal2 = getImage("/res/diagonal2.png");
    }

    private void initGameData(){
        map = new int[3][3];
        for(int i  = 0; i < 3; ++i)
            for(int j = 0; j < 3; ++j)
                map[i][j] = 0;
    }

    private BufferedImage getImage(String imageName){
        BufferedImage image;
        try{
            image = ImageIO.read(Game.class.getResource(imageName));
            return image;
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

    private void judgeGame(){
        if(GameEnd()){
            EndSet();
            repaint();
        }
        else{
            if(num == 9){
                DrawSet();
                repaint();
            }
        }
    }

    private boolean GameEnd(){
        if(FindThree(1, 2, 3))      {line = 1; return true;}
        else if(FindThree(4, 5, 6)) {line = 2; return true;}
        else if(FindThree(7, 8, 9)) {line = 3; return true;}
        else if(FindThree(1, 4, 7)) {line = 4; return true;}
        else if(FindThree(2, 5, 8)) {line = 5; return true;}
        else if(FindThree(3, 6, 9)) {line = 6; return true;}
        else if(FindThree(1, 5, 9)) {line = 7; return true;}
        else if(FindThree(3, 5, 7)) {line = 8; return true;}
        else {return false;}
    }

    private boolean FindThree(int x, int y, int z){
        if(getMap(x) != 0 &&
                getMap(x) == getMap(y) && getMap(x) == getMap(z)){
            if(getMap(x) == 1)
                youWin = true;
            else
                youWin = false;
            return true;
        }
        return false;
    }

    private int getMap(int x){
        int i = (x - 1) / 3;
        int j = (x - 1) % 3;
        return map[i][j];
    }

    private void EndSet(){
        gameState = 3;
        if(youWin){
            message = "Win!";
            os.println("youLose");
            os.println(line);
            os.flush();
        }
        else{
            message = "Lose";
            os.println("youWin");
            os.println(line);
            os.flush();
        }
    }

    private void DrawSet(){
        gameState = 3;
        message = "Draw";
        os.println("Draw");
        os.println("");
        os.flush();
    }

    private void reset(){
        line = -1;
        num = 0;
        for(int i  = 0; i < 3; ++i)
            for(int j = 0; j < 3; ++j)
                map[i][j] = 0;

        os.println("reset");
        connectThread.RandomFirst();

        repaint();
    }

    private class connectThread extends Thread{

        private int m_port;

        public connectThread(int port){
            m_port = port;
        }

        @Override
        public void run(){
            try {
                server = new ServerSocket(m_port);
                socket = server.accept();
                is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                os = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                RandomFirst();
                repaint();
                while(true){
                    try{
                        String s1 = is.readLine();
                        String s2 = is.readLine();
                        if(s1 == null && s2 == null)
                            continue;
                        int ix = Integer.parseInt(s1);
                        int iy = Integer.parseInt(s2);
                        if(ix >= 0 && ix <= 2 && iy >= 0 && iy <= 2
                                && gameState == 2){
                            if(map[ix][iy] == 0){
                                map[ix][iy] = 2;
                                num++;
                                gameState = 1;
                                message = "Your Turn";
                                judgeGame();
                                repaint();
                            }
                        }
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        private void RandomFirst(){
            int random = (int)(1 + Math.random() * 10);
            if(random % 2 == 0){
                gameState = 1;
                message = "Your Turn";
                os.println(0);
            }
            else {
                gameState = 2;
                message = "Opponent's Turn";
                os.println(1);
            }
            os.flush();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e){}
    @Override
    public void mouseExited(MouseEvent e){}
    @Override
    public void mousePressed(MouseEvent e){}
    @Override
    public void mouseReleased(MouseEvent e){}

}


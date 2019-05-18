package um.project.titanlander.Debugger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Simulator {

    public static void main(String[] args) {
        new Simulator();
    }

    //---
    private LandingModule landingModule = new LandingModule(new Vector2(200, 700), new Vector2(-10, -3), LandingModule.ControllerMode.OPEN);
    private UI ui = new UI();

    public Simulator() {
        update();
    }

    public void update() {
        int i = 0;
        while (true) {
            landingModule.updateVelocity();
            landingModule.updatePosition();
            landingModule.updateController();
            System.out.println(landingModule);
            try {
                Thread.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ui.repaint();
            System.out.println(i++);
        }
    }

    public class UI extends JPanel {

        private JFrame frame = new JFrame();

        private BufferedImage background;

        {
            try {
                background = ImageIO.read(new File("src/main/resources/background.png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public UI() {
            this.frame.add(this);
            this.frame.setSize(1280, 720);
            this.frame.setVisible(true);
            this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            //g.drawImage(background, 0, 0, null);

            g.setColor(Color.GREEN);
            g.drawRect(0, 0, 1280, 720);

            {
                g.setColor(Color.BLUE);
                g.drawLine(0, 1280, frame.getWidth(), 1280);
            }

            {
                Vector2 screen = toScreenCoordinates(landingModule.getPosition());
                g.setColor(Color.RED);
                g.drawRect((int) (screen.getX()-(landingModule.getWidth()/2D)), (int) (screen.getY()-(landingModule.getHeight()/2D)), (int) landingModule.getWidth(), (int) landingModule.getHeight());


                Vector2 shadow = toScreenCoordinates(landingModule.getRealPositions());
                g.setColor(Color.PINK);
                g.drawRect((int) (shadow.getX()-(landingModule.getWidth()/2D)), (int) (shadow.getY()-(landingModule.getHeight()/2D)), (int) landingModule.getWidth(), (int) landingModule.getHeight());

                {
                    if(landingModule.leftThruster.isBurning()) {
                        Vector2 left = screen.add(new Vector2(-20, 0));
                        g.drawLine((int) screen.getX(), (int) screen.getY(), (int) left.getX(), (int) left.getY());
                    }
                    if(landingModule.rightThruster.isBurning()) {
                        Vector2 left = screen.add(new Vector2(20, 0));
                        g.drawLine((int) screen.getX(), (int) screen.getY(), (int) left.getX(), (int) left.getY());
                    }

                    if(landingModule.downThruster.isBurning()) {
                        Vector2 left = screen.add(new Vector2(0, 20));
                        g.drawLine((int) screen.getX(), (int) screen.getY(), (int) left.getX(), (int) left.getY());
                    }
                }

            }

            {
                g.setColor(Color.CYAN);
                Vector2 screen = toScreenCoordinates(new Vector2(0, 0));
                g.drawOval((int) screen.getX(), (int) screen.getX(), 2, 2);
            }

        }

        public Vector2 toScreenCoordinates(Vector2 vec) {
            return new Vector2((vec.getX() + 640.0), (720 - vec.getY()));
        }

    }

}

package um.project.titanlander.Debugger;

import javax.swing.*;
import java.awt.*;

public class Simulator {

    public static void main(String[] args) {
        new Simulator();
    }

    //---
    private LandingModule landingModule = new LandingModule(new Vector2(300, 720), new Vector2(-10, -10), LandingModule.ControllerMode.OPEN);
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
            ui.repaint();
            try {
                System.out.println(i++);
                Thread.sleep(120);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public class UI extends JPanel {

        private JFrame frame = new JFrame();

        public UI() {
            this.frame.add(this);
            this.frame.setSize(1280, 720);
            this.frame.setVisible(true);
            this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }

        @Override
        public void paintComponent(Graphics g) {

            g.setColor(Color.BLACK);
            g.fillRect(0, 0, frame.getWidth(), frame.getHeight());

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

package um.project.titanlander.Debugger;

import org.knowm.xchart.*;
import um.project.titanlander.Debugger.lander.ControllerMode;
import um.project.titanlander.Debugger.lander.LandingModule;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class Simulator {

    public static void main(String[] args) {
        new Simulator();
    }

    //---
    private DialChart dialChart = new DialChartBuilder().title("Speed").height(400).width(400).build();
    private DialChart rotationChart = new DialChartBuilder().title("rotation").height(400).width(400).build();
    private XYChart radarChart = new XYChartBuilder().title("horizontal").height(400).width(400).build();
    private XYChart height = new XYChartBuilder().title("height").height(400).width(400).build();

    private LandingModule landingModule = new LandingModule(new Vector3(200, 5e5 /*5.48433e4*/, -100), new Vector3(-10, -3, -10), ControllerMode.CLOSED);
    private UI ui = new UI();
    JFrame wrapper;

    {
        dialChart.addSeries("speed", 1);
        radarChart.addSeries("horz", new double[] { 1 });
        height.addSeries("height", new double[] {1});
        rotationChart.addSeries("rotation", 1);

        rotationChart.getStyler().setArcAngle(360);

        wrapper = new SwingWrapper(Arrays.asList(radarChart, dialChart, height, rotationChart)).displayChartMatrix();
    }


    public Simulator() {
        update();
    }

    public void update() {
        int i = 0;
        double max = 0;

        Map<Vector3, Vector3> pos = new LinkedHashMap<>();

        while (true) {
            landingModule.updateVelocity();
            landingModule.updatePosition();
            landingModule.updateController();
            System.out.println(landingModule.toString());

            //---
            pos.put(landingModule.getRealPositions(), landingModule.getPosition());
            radarChart.updateXYSeries("horz",
                    pos.keySet().stream().mapToDouble(Vector3::getX).toArray(),
                    pos.keySet().stream().mapToDouble(Vector3::getZ).toArray(),
                    pos.entrySet().stream().mapToDouble(k -> k.getKey().sub(k.getValue()).length()).toArray());

            height.updateXYSeries("height",
                    pos.keySet().stream().mapToDouble(Vector3::getX).toArray(),
                    pos.keySet().stream().mapToDouble(Vector3::getY).toArray(),
                    pos.entrySet().stream().mapToDouble(k -> k.getKey().sub(k.getValue()).length()).toArray());

            rotationChart.getSeriesMap().get("rotation").setValue(Math.toDegrees(landingModule.getRotation()) + 180);

            max = Math.max(max, landingModule.getRealVelocity().length());
            dialChart.getSeriesMap().get("speed").setValue(landingModule.getRealVelocity().length() / max);

            wrapper.repaint();
            ui.repaint();

            try {
                Thread.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
                Vector3 screen = toScreenCoordinates(landingModule.getPosition());
                g.setColor(Color.RED);
                g.drawRect((int) (screen.getX()-(landingModule.getHeight()/2D)), (int) (screen.getY()-(landingModule.getHeight()/2D)), (int) landingModule.getHeight(), (int) landingModule.getHeight());


                Vector3 shadow = toScreenCoordinates(landingModule.getRealPositions());
                g.setColor(Color.PINK);
                g.drawRect((int) (shadow.getX()-(landingModule.getHeight()/2D)), (int) (shadow.getY()-(landingModule.getHeight()/2D)), (int) landingModule.getHeight(), (int) landingModule.getHeight());

                {
                    if(landingModule.leftThruster.isBurning()) {
                        Vector3 left = screen.add(new Vector3(-20, 0, 0));
                        g.drawLine((int) screen.getX(), (int) screen.getY(), (int) left.getX(), (int) left.getY());
                    }
                    if(landingModule.rightThruster.isBurning()) {
                        Vector3 left = screen.add(new Vector3(20, 0, 0));
                        g.drawLine((int) screen.getX(), (int) screen.getY(), (int) left.getX(), (int) left.getY());
                    }

                    if(landingModule.downThruster.isBurning()) {
                        Vector3 left = screen.add(new Vector3(0, 20, 0));
                        g.drawLine((int) screen.getX(), (int) screen.getY(), (int) left.getX(), (int) left.getY());
                    }
                }

            }

            {
                g.setColor(Color.CYAN);
                Vector3 screen = toScreenCoordinates(new Vector3(0, 0, 0));
                g.drawOval((int) screen.getX(), (int) screen.getX(), 2, 2);
            }

        }

        public Vector3 toScreenCoordinates(Vector3 vec) {
            return new Vector3((vec.getX() + 640.0), (720 - vec.getY()), vec.getZ());
        }

    }

}

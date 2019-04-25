package um.project.titanlander.Debugger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class LandingModule {

    private final static double TIME_STEP = 0.1;
    private final static double GRAVITY = 1.352E-2; //0.01352m/s^2

    private Vector2 velocity;
    private Vector2 position; // actual position
    private Vector2 realPositions; // openloop controller

    private DataLogger dataLogger = new DataLogger();

    private double theta = 0; // rotation
    private double thetaVelocity = Math.PI/32;

    private double mass = 300;
    private double radius = 2.5;
    private double width=3;
    private double height=3;

    public Thruster downThruster = new Thruster(Direction.Y_POS, 400);
    public Thruster leftThruster = new Thruster(Direction.X_POS, 200);
    public Thruster rightThruster = new Thruster(Direction.X_NEG, 200);

    private ControllerMode controllerMode;

    public LandingModule(Vector2 position, Vector2 velocity, ControllerMode controllerMode) {
        this.velocity = velocity;
        this.position = position;
        this.controllerMode = controllerMode;

        if(controllerMode == ControllerMode.OPEN) {
            this.realPositions = position;
        }
    }

    public double getHeight() {
        return height;
    }

    public double getWidth() {
        return width;
    }

    public void setTheta(double theta) {
        this.theta = theta % (2*Math.PI);
    }

    double min = Double.MAX_VALUE;
    public void updateController() {

        if(this.controllerMode == ControllerMode.OPEN) {

            final double distanceY = Math.abs(this.getPosition().getY());
            final double distanceX = Math.abs(this.getPosition().getX());

            if(distanceY < 0.01) {
                System.out.println("down: " + downThruster.fuelUsed);
                System.out.println("left: " + leftThruster.fuelUsed);
                System.out.println("right: " + rightThruster.fuelUsed);

                StringBuffer buffer = new StringBuffer();
                buffer.append("height,left,down,right,px,windStrength\n");
                for(Map.Entry<Double, Map<String, Double>> entry : dataLogger.getData().entrySet()) {
                    buffer.append(String.format("%.4f,%.4f,%.4f,%.4f,%.4f,%.4f\n", entry.getKey(), entry.getValue().get("left"), entry.getValue().get("down"), entry.getValue().get("right"), entry.getValue().get("position"), entry.getValue().get("windStrength")));
                }
                try {
                    Files.write(Paths.get("test.csv"), buffer.toString().getBytes(), StandardOpenOption.WRITE);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return;
            }

            // --- Y-burn
            final double signedYForce = this.getVelocity().getY() * mass;

            final double breakingTime = distanceY / Math.abs(this.getVelocity().getY());
            double toZero = Math.abs(this.getVelocity().getY()) / (this.downThruster.getForce(mass));
            //System.out.println(breakingTime + " < " + toZero);

            if((signedYForce < 0 && breakingTime <= toZero) && distanceY > 0.5) {
                downThruster.burn(breakingTime);
                System.out.println("landing burn");
            }

            //Horizontal Translation

            if(distanceX >= 0) {
                final double xF = this.getVelocity().getX() * mass;
                double timeToX = distanceX / Math.abs(this.getVelocity().getX());

                if (xF > 1E-6) { //moving right
                    // Do we overstep in the next update?
                    if (this.getPosition().add(this.velocity.mul(TIME_STEP)).getX() > 0) {
                        rightThruster.burn(Math.abs(xF / (this.rightThruster.getForce(mass))));
                        System.out.println("A");
                    } else if (this.getVelocity().getX() / (rightThruster.getForce(mass)) <= TIME_STEP) {
                        final double maxThrust = (rightThruster.getForce(mass));
                        final double thrustDelta = maxThrust - Math.abs(xF);
                        leftThruster.burn(Math.abs((thrustDelta) / leftThruster.getForce(mass)));
                        System.out.println("B");
                    }
                    // Are we too slow to get to x=0 in time? -> Accelerate
                    else if(timeToX <= (Math.abs(this.getVelocity().getX())/ rightThruster.getForce(mass))) {
                        rightThruster.burn( rightThruster.getForce(mass) / Math.abs(((distanceX / timeToX) / timeToX) - Math.abs(this.getVelocity().getX())));
                        System.out.println("D");
                    }
                    // Are we going too fast? ->  Decelerate
                    else if(timeToX >= breakingTime) {
                        System.out.println("B1");
                        leftThruster.burn(leftThruster.getForce(mass) / Math.abs(((distanceX / breakingTime) / breakingTime) - Math.abs(this.getVelocity().getX())));
                    }
                } else if (xF < -1E-6) { //moving left
                    if (this.getPosition().add(this.velocity.mul(TIME_STEP)).getX() < 0) {
                        leftThruster.burn(Math.abs(xF / (this.leftThruster.getForce(mass))));
                        System.out.println("E");
                    } else if (Math.abs(this.getVelocity().getX()) / (leftThruster.getForce(mass)) <=  TIME_STEP) {
                        final double maxThrust = (leftThruster.getForce(mass));
                        final double thrustDelta = maxThrust - Math.abs(xF);
                        rightThruster.burn(Math.abs((thrustDelta) / rightThruster.getForce(mass)));
                        System.out.println("F");
                    } else if(timeToX <= (Math.abs(this.getVelocity().getX())/ leftThruster.getForce(mass))) {
                        leftThruster.burn( leftThruster.getForce(mass) / Math.abs(((distanceX / timeToX) / timeToX) - Math.abs(this.getVelocity().getX())));
                        System.out.println("H");
                    } else if(timeToX >= breakingTime) {
                        System.out.println("B2");
                        rightThruster.burn(rightThruster.getForce(mass) / Math.abs(((distanceX / breakingTime) / breakingTime) - Math.abs(this.getVelocity().getX())));
                    }
                    //System.out.println("a: " + (distanceX / Math.abs(this.getVelocity().getX())) + " - " + Math.abs(this.getVelocity().getX()) / (this.leftThruster.getForce() / mass));
                } else {
                    if (this.getPosition().getX() < 0) {
                        leftThruster.burn(leftThruster.getForce(mass) / Math.abs(((distanceX / timeToX) / timeToX) - Math.abs(this.getVelocity().getX())));
                    } else if (this.getPosition().getX() > 0) {
                        rightThruster.burn(rightThruster.getForce(mass) / Math.abs(((distanceX / breakingTime) / breakingTime) - Math.abs(this.getVelocity().getX())));
                    }
                }
            }

        }

    }

    public void updatePosition() {

        this.position = this.position.add(this.velocity.mul(TIME_STEP));
        if(this.position.getY() < 0) {
            this.position = new Vector2(this.position.getX(), 0);
        }

    }

    public void updateVelocity() {

        //--- Gravity
        //v+1 = v + (Gv*m)/deltaY
        this.velocity = this.velocity.add(new Vector2(0, GRAVITY).mul(mass).div(Math.pow(1287850-this.getPosition().getY(), 2)).mul(-1));

        //--- Thrusters
        final double key = this.getPosition().getY();
        dataLogger.add(key, "position", position.getX());
        {
            Vector2 thrust = downThruster.getThrust(mass);
            dataLogger.add(key, "down", thrust.length());
            this.velocity = this.velocity.add(thrust);

        }
        {
            Vector2 thrust = leftThruster.getThrust(mass);
            dataLogger.add(key, "left", -thrust.length());
            this.velocity = this.velocity.add(thrust);

        }
        {
            Vector2 thrust = rightThruster.getThrust(mass);
            dataLogger.add(key, "right", thrust.length());
            this.velocity = this.velocity.add(thrust);

        }

        //--- Rotation
        this.setTheta(this.theta + this.thetaVelocity);

        if(this.position.getY() <= 0) {
            this.velocity = new Vector2(0, 0);
            this.position = new Vector2(this.position.getX(), 0);
        }

        Vector2 v = wind(getPosition(), mass);
        dataLogger.add(key, "windStrength", v.getX());
        if(!Double.isNaN(v.getX())) {
            this.velocity = this.velocity.add(v);
        } else
            System.out.println(v);


        downThruster.update();
        leftThruster.update();
        rightThruster.update();
    }

    public Vector2 getPosition() {
        return this.position;
    }

    public Vector2 getVelocity() {
        return this.velocity;
    }

    @Override
    public String toString() {
        return String.format("Module[p=%s,v=%s,θ=%.4f,θ'=%.4f]", this.position, this.velocity, Math.toDegrees(this.theta), Math.toDegrees(this.thetaVelocity));
    }

    public class Thruster {

        private Direction direction;
        private double force;
        private double timeToBurn = 0;
        public double fuelUsed = 0;

        public Thruster(Direction direction, double force) {
            this.direction = direction;
            this.force = force;
        }

        public double getForce(double mass) {
            return force / mass;
        }

        /**
         * Change in velocity.
         * @param seconds
         * @return
         */
        public void burn(double seconds) {
            this.timeToBurn = Math.min(seconds, TIME_STEP);
            //System.out.println(direction + " -> " + seconds);
        }

        public Vector2 getThrust(double mass) {
            Vector2 v = new Vector2(0, 0);
            if(timeToBurn > 0) {
                v = direction.direction().mul(force).mul(Math.min(TIME_STEP, timeToBurn)).div(mass);
            }
            return v;
        }

        public void update() {
            if(timeToBurn != 0) {
                timeToBurn -= TIME_STEP;
                timeToBurn = Math.max(timeToBurn, 0);
            }
        }

        public boolean isBurning() {
            return this.timeToBurn > 0;
        }

    }

    public enum Direction {

        X_NEG(new Vector2(-1, 0)),
        X_POS(new Vector2(1, 0)),
        Y_POS(new Vector2(0, 1));

        private Vector2 dir;
        Direction(Vector2 dir) {
            this.dir = dir;
        }

        public Vector2 direction() {
            return dir;
        }

    }

    public enum ControllerMode {
        OPEN
    }

    public static Vector2 wind(Vector2 pos, double mass) {
        final double speed = 2D * (Math.log(pos.getY() / 0.15D)/Math.log(20D/0.15D));
        final Vector2 dir = new Vector2(1 * Math.sin(pos.getY()/100D), 0);
        return dir.mul(speed).div(mass);
    }

}

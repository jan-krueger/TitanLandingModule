package um.project.titanlander.Debugger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class LandingModule {

    private final static double TIME_STEP = 0.05;
    private final static double GRAVITY = 1.352E-2; //0.01352m/s^2

    private Vector2 velocity;
    private Vector2 position;

    private Vector2 realPositions;
    private Vector2 realVelocity;

    private DataLogger dataLogger = new DataLogger();

    private double theta = -Math.PI/2D; // rotation
    private double thetaVelocity = Math.PI / 300;

    private double mass = 300;
    private double radius = 2.5;
    private double width=3;
    private double height=3;

    public Thruster downThruster = new Thruster(Direction.Y_POS, 400);
    public Thruster leftThruster = new Thruster(Direction.X_POS, 200);
    public Thruster rightThruster = new Thruster(Direction.X_NEG, 200);

    public Thruster leftRotation = new Thruster(Direction.X_NEG, rightThruster.force);
    public Thruster rightRotation = new Thruster(Direction.X_POS, leftThruster.force);

    private ControllerMode controllerMode;

    public LandingModule(Vector2 position, Vector2 velocity, ControllerMode controllerMode) {
        this.realVelocity = velocity;
        this.realPositions = position;
        this.controllerMode = controllerMode;

        this.velocity = velocity.copy();
        this.position = position.copy();
    }

    public Vector2 getRealPositions() {
        return realPositions;
    }

    public Vector2 getRealVelocity() {
        return realVelocity;
    }

    public double getHeight() {
        return height;
    }

    public double getWidth() {
        return width;
    }

    public void setTheta(double theta) {
        this.theta = Math.atan2(Math.sin(theta), Math.cos(theta));
    }

    public void setThetaVelocity(double thetaVelocity) {
        this.thetaVelocity = Math.atan2(Math.sin(thetaVelocity), Math.cos(thetaVelocity));
    }

    public void updateController() {

        final double distanceY = Math.abs(this.getPosition().getY());
        final double distanceX = Math.abs(this.getPosition().getX());

        if(distanceY < 1E-3) {
            StringBuilder buffer = new StringBuilder();
            buffer.append("height,left,down,right,px,x,windStrength\n");
            for(Map.Entry<Double, Map<String, Double>> entry : dataLogger.getData().entrySet()) {
                buffer.append(String.format("%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f\n", entry.getKey(), entry.getValue().get("left"), entry.getValue().get("down"), entry.getValue().get("right"), entry.getValue().get("realPosition"), entry.getValue().get("position"), entry.getValue().get("windStrength")));
            }
            try {
                Files.write(Paths.get("test.csv"), buffer.toString().getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }

            return;
        }

        double yToZero = Math.abs(this.getVelocity().getY()) / (this.downThruster.getForce(mass));

        // --- Rotation
        if(Math.abs(theta) > 1E-4) {
            final double timeTo0 = Math.abs(theta / thetaVelocity);
            final double breakingTime = Math.abs(thetaVelocity / leftRotation.getAngularForce(mass).getX()); // assumes both thrusters are equally strong

            if(Double.isFinite(timeTo0) && timeTo0 <= breakingTime) {
                if(thetaVelocity > 0) {
                    leftRotation.burn(breakingTime);
                } else {
                    rightRotation.burn(breakingTime);
                }
            } else if(timeTo0 >= yToZero) {
                if(theta > 0) {
                    leftRotation.burn(timeTo0);
                } else {
                    rightRotation.burn(timeTo0);
                }
            }
        }

        if(Math.abs(theta) > 0.17) {
            return;
        }

        // --- Y-burn
        final double signedYForce = this.getVelocity().getY() * mass;

        final double yBreakingTime = distanceY / Math.abs(this.getVelocity().getY());
        //System.out.println(breakingTime + " < " + toZero);

        if((signedYForce < 0 && yBreakingTime <= yToZero) && distanceY > 0.5) {
            downThruster.burn(yBreakingTime);
            System.out.println("landing burn");
        }

        //Horizontal Translation
        if(distanceX >= 0) {
            final double xF = this.getVelocity().getX() * mass;
            final double xV = this.getVelocity().getX();
            double timeToX = (distanceX / Math.abs(this.getVelocity().getX()));

            final double velocityLimit = 1E-3;

            if (xV > velocityLimit) { //moving right
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
                else if(timeToX >= yBreakingTime) {
                    System.out.println("B1");
                    leftThruster.burn(leftThruster.getForce(mass) / Math.abs(((distanceX / yBreakingTime) / yBreakingTime) - Math.abs(this.getVelocity().getX())));
                }
            } else if (xV < -velocityLimit) { //moving left
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
                } else if(timeToX >= yBreakingTime) {
                    System.out.println("B2");
                    rightThruster.burn(rightThruster.getForce(mass) / Math.abs(((distanceX / yBreakingTime) / yBreakingTime) - Math.abs(this.getVelocity().getX())));
                }
                //System.out.println("a: " + (distanceX / Math.abs(this.getVelocity().getX())) + " - " + Math.abs(this.getVelocity().getX()) / (this.leftThruster.getForce() / mass));
            } else if(distanceX > 0.1) {
                if (this.getPosition().getX() < 0) {
                    leftThruster.burn(leftThruster.getForce(mass) / Math.abs(((distanceX / timeToX) / timeToX) - Math.abs(this.getVelocity().getX())));
                } else if (this.getPosition().getX() > 0) {
                    rightThruster.burn(rightThruster.getForce(mass) / Math.abs(((distanceX / yBreakingTime) / yBreakingTime) - Math.abs(this.getVelocity().getX())));
                }
            }
        }

    }

    public void updatePosition() {

        this.position = this.position.add(this.velocity.mul(TIME_STEP));
        if(this.position.getY() < 0) {
            this.position = new Vector2(this.position.getX(), 0);
        }

        this.realPositions = this.realPositions.add(this.realVelocity.mul(TIME_STEP));
        if(this.realPositions.getY() < 0) {
            this.realPositions = new Vector2(this.realPositions.getX(), 0);
        }
    }

    public void updateVelocity() {

        //--- Thrusters
        final double key = this.getPosition().getY();
        dataLogger.add(key, "realPosition", realPositions.getX());
        dataLogger.add(key, "position", position.getX());

        Vector2 thrustTotal = new Vector2(0, 0);
        {
            Vector2 thrust = applyRotation(downThruster.getThrust(mass));
            dataLogger.add(key, "down", thrust.length());
            thrustTotal = thrustTotal.add(thrust);
        }
        {
            Vector2 thrust = applyRotation(leftThruster.getThrust(mass));
            dataLogger.add(key, "left", -thrust.length());
            thrustTotal = thrustTotal.add(thrust);
        }
        {
            Vector2 thrust = applyRotation(rightThruster.getThrust(mass));
            dataLogger.add(key, "right", thrust.length());
            thrustTotal = thrustTotal.add(thrust);
        }

        Vector2 t = thrustTotal.copy();
        thrustTotal = applyRotation(thrustTotal);
        this.realVelocity = this.realVelocity.add(thrustTotal);
        this.velocity = this.velocity.add(thrustTotal);

        //--- Rotation
        {
            double totalRotation = 0;
            totalRotation += leftRotation.getAngularThrust(mass).getX();
            totalRotation += rightRotation.getAngularThrust(mass).getX();
            this.thetaVelocity += totalRotation;
            System.out.println("t-velocity:" + this.thetaVelocity);
        }

        this.setTheta(this.theta + this.thetaVelocity);

        //--- Gravity
        //v+1 = v + (Gv*m)/deltaY
        Vector2 gravity = new Vector2(0, GRAVITY).mul(mass).div(Math.pow(1287850D-this.getPosition().getY(), 2)).mul(-1);
        this.realVelocity = this.realVelocity.add(gravity);
        this.velocity = this.velocity.add(gravity);


        Vector2 v = wind(getPosition(), mass);
        dataLogger.add(key, "windStrength", v.getX());
        if(!Double.isNaN(v.getX()) && false) {
            if(this.controllerMode == ControllerMode.CLOSED) {
                this.velocity = this.velocity.add(v);
            }
            this.realVelocity = this.realVelocity.add(v);
        }

        leftRotation.update();
        rightRotation.update();
        downThruster.update();
        leftThruster.update();
        rightThruster.update();
    }

    private Vector2 applyRotation(Vector2 force) {
        return new Vector2(force.getX() * Math.cos(theta) - force.getY() * Math.sin(theta),
                force.getX() * Math.sin(theta) + force.getY() * Math.cos(theta));
    }

    public Vector2 getPosition() {
        return this.position;
    }

    public Vector2 getVelocity() {
        return this.velocity;
    }

    @Override
    public String toString() {
        return String.format("Module\n\tr:[p=%s,v=%s]\n\ta:[p=%s,v=%s,%.2f]", this.realPositions, this.realVelocity, this.position, this.velocity, this.theta);
    }

    public class Thruster {

        private Direction direction;
        private double force;
        private double timeToBurn = 0;

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

        public Vector2 getAngularForce(double mass) {
            return direction.direction().mul(force).div(mass).div(Math.sqrt(2)).mul(1D / 12D);
        }

        public Vector2 getAngularThrust(double mass) {
            Vector2 t = new Vector2();
            if(timeToBurn > 0) {
                t = direction.direction().mul(force).div(mass).div(Math.sqrt(2)).mul(1D / 12D).mul(Math.min(TIME_STEP, timeToBurn));
            }
            return t;
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
        OPEN,
        CLOSED
    }

    public static Vector2 wind(Vector2 pos, double mass) {
        final double speed = 2D * (Math.log(pos.getY() / 0.15D)/Math.log(20D/0.15D));
        final Vector2 dir = new Vector2(0.1 * Math.sin(pos.getY()/100D), 0).normalise();
        return dir.mul(speed).div(mass);
    }

}

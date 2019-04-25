package um.project.titanlander.Debugger;

public class LandingModule {

    private final static double TIME_STEP = 1;
    private final static double GRAVITY = 1.352E-2; //0.01352m/s^2

    private Vector2 velocity;
    private Vector2 position; // actual position
    private Vector2 realPositions; // openloop controller

    private double theta = 0; // rotation
    private double thetaVelocity = Math.PI/32;

    private double mass = 300;
    private double radius = 2.5;
    private double width=3;
    private double height=3;

    public Thruster downThruster = new Thruster(Direction.Y_POS, 100);
    public Thruster leftThruster = new Thruster(Direction.X_POS, 100);
    public Thruster rightThruster = new Thruster(Direction.X_NEG, 100);

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

    public void updateController() {

        if(this.controllerMode == ControllerMode.OPEN) {
            final double distanceY = Math.abs(this.getPosition().getY());
            final double distanceX = Math.abs(this.getPosition().getX());

            final double MAX_BURN_TIME = TIME_STEP;

            // --- Y-burn
            final double signedYForce = this.getVelocity().getY() * mass;

            double breakingTime = distanceY / Math.abs(this.getVelocity().getY());
            double toZero = Math.abs(this.getVelocity().getY()) / (this.downThruster.getForce(mass));
            //System.out.println(breakingTime + " < " + toZero);

            /*if((signedYForce < 0 && breakingTime <= toZero)) {
                downThruster.burn(breakingTime);
                System.out.println("landing burn");
            }*/

            double timeToY = distanceY / Math.abs(this.getVelocity().getY());
            if(timeToY >= breakingTime) {
                downThruster.burn(downThruster.getForce(mass) / Math.abs(((distanceY / breakingTime) / breakingTime) - Math.abs(this.getVelocity().getY())));
            }

            //Horizontal Translation
            if(distanceX >= 0) {
                final double xF = this.getVelocity().getX() * mass;
                if (xF > 0) { //moving right
                    if (this.getPosition().add(this.velocity.mul(MAX_BURN_TIME)).getX() > 0) {
                        rightThruster.burn(Math.abs(xF / (this.rightThruster.getForce(mass))));
                        System.out.println("A");
                    } else if (this.getVelocity().getX() / (rightThruster.getForce(mass)) <= MAX_BURN_TIME) {
                        final double maxThrust = (rightThruster.getForce(mass));
                        final double thrustDelta = maxThrust - Math.abs(xF);
                        leftThruster.burn(Math.min(MAX_BURN_TIME, Math.abs((thrustDelta) / leftThruster.getForce(mass))));
                        System.out.println("B");
                    }else if(distanceX / Math.abs(this.getVelocity().getX()) < Math.abs(this.getVelocity().getX()) / (this.rightThruster.getForce(mass))) {
                        rightThruster.burn( Math.abs(this.getVelocity().getX()) / (this.rightThruster.getForce(mass)));
                        System.out.println("D");
                    } else {
                        double timeToX = distanceX / Math.abs(this.getVelocity().getX());
                        if(timeToX >= breakingTime) {
                            leftThruster.burn(leftThruster.getForce(mass) / Math.abs(((distanceX / breakingTime) / breakingTime) - Math.abs(this.getVelocity().getX())));
                        }
                    }
                } else if (xF < 0) { //moving left
                    if (this.getPosition().add(this.velocity.mul(MAX_BURN_TIME)).getX() < 0) {
                        leftThruster.burn(Math.abs(xF / (this.leftThruster.getForce(mass))));
                        System.out.println("E");
                    } else if (Math.abs(this.getVelocity().getX()) / (leftThruster.getForce(mass)) <=  MAX_BURN_TIME) {
                        final double maxThrust = (leftThruster.getForce(mass));
                        final double thrustDelta = maxThrust - Math.abs(xF);
                        rightThruster.burn(Math.abs((thrustDelta) / rightThruster.getForce(mass)));
                        System.out.println("F");
                    } else if(distanceX / Math.abs(this.getVelocity().getX()) < Math.abs(this.getVelocity().getX()) / (this.leftThruster.getForce(mass))) {
                        leftThruster.burn( Math.abs(this.getVelocity().getX()) / (this.leftThruster.getForce(mass)));
                        System.out.println("H");
                    } else {
                        double timeToX = distanceX / Math.abs(this.getVelocity().getX());
                        if(timeToX >= breakingTime) {
                            rightThruster.burn(rightThruster.getForce(mass) / Math.abs(((distanceX / breakingTime) / breakingTime) - Math.abs(this.getVelocity().getX())));
                        }
                    }
                    //System.out.println("a: " + (distanceX / Math.abs(this.getVelocity().getX())) + " - " + Math.abs(this.getVelocity().getX()) / (this.leftThruster.getForce() / mass));
                } else {
                    /*if (this.getPosition().getX() < 0) {
                        leftThruster.burn(TIME_STEP);
                    } else if (this.getPosition().getX() > 0) {
                        rightThruster.burn(TIME_STEP);
                    }*/
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
        this.velocity = this.velocity.add(downThruster.getThrust(mass));
        this.velocity = this.velocity.add(leftThruster.getThrust(mass));
        this.velocity = this.velocity.add(rightThruster.getThrust(mass));

        //--- Rotation
        this.setTheta(this.theta + this.thetaVelocity);

        if(this.position.getY() <= 0) {
            this.velocity = new Vector2(0, 0);
            this.position = new Vector2(this.position.getX(), 0);
        }

        Vector2 v = wind(getPosition(), mass);
        if(!Double.isNaN(v.getX())) {
            //this.velocity = this.velocity.add(v);
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
            if(timeToBurn > 0) {
                return direction.direction().mul(force).mul(Math.min(TIME_STEP, timeToBurn)).div(mass);
            }
            return new Vector2();
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
        final Vector2 dir = new Vector2(-10, 0);
        return dir.mul(speed).div(mass);
    }

}

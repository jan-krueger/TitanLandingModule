package um.project.titanlander.Debugger;

public class LandingModule {

    private final static double TIME_STEP = 1;
    private final static double GRAVITY = 1.352E-2;

    private Vector2 velocity;
    private Vector2 position; // actual position
    private Vector2 expectedPosition; // openloop controller

    private double theta = 0; // rotation
    private double thetaVelocity = Math.PI/32;

    private double mass = 100;
    private double radius = 2.5;
    private double width=3;
    private double height=3;

    public Thruster upThruster = new Thruster(Direction.Y_POS, 50);
    public Thruster leftThruster = new Thruster(Direction.X_POS, 10);
    public Thruster rightThruster = new Thruster(Direction.X_NEG, 10);

    private ControllerMode controllerMode;

    public LandingModule(Vector2 position, Vector2 velocity, ControllerMode controllerMode) {
        this.velocity = velocity;
        this.position = position;
        this.controllerMode = controllerMode;

        if(controllerMode == ControllerMode.OPEN) {
            this.expectedPosition = position;
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

            if(Math.abs(this.getPosition().getX()) > 1) { //error=1m
                // correct x
                double xDirection = -this.getVelocity().getX();
                double xForce = xDirection * mass;

                //vx*m+((T/m)*t)=0
                //1/t=(T/m)/m/vx
            }


        }

    }

    public void updatePosition() {

        this.position = this.position.add(this.velocity.mul(TIME_STEP));

        if(this.position.getY() <= 0) {
            this.velocity = new Vector2(0, 0);
        }

    }

    public void updateVelocity() {

        //--- Gravity
        this.velocity = this.velocity.add(new Vector2(0, GRAVITY).mul(mass).mul(-1D/this.getPosition().getY()));

        //--- Thrusters
        this.velocity = this.velocity.add(upThruster.getThrust().div(mass));
        this.velocity = this.velocity.add(leftThruster.getThrust().div(mass));
        this.velocity = this.velocity.add(rightThruster.getThrust().div(mass));

        //--- Rotation
        this.setTheta(this.theta + this.thetaVelocity);

        if(this.position.getY() <= 0) {
            this.velocity = new Vector2(0, 0);
            this.position = new Vector2(this.position.getX(), 0);
        }

        upThruster.update();
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

        public double getForce() {
            return force;
        }

        /**
         * Change in velocity.
         * @param seconds
         * @return
         */
        public void burn(int seconds) {
            this.timeToBurn = seconds;
        }

        public Vector2 getThrust() {
            if(timeToBurn > 0) {
                return direction.direction().mul(force).mul(TIME_STEP);
            }
            return new Vector2();
        }

        public void update() {
            timeToBurn -= TIME_STEP;
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

}

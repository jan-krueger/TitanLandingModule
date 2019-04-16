package um.project.titanlander.Debugger;

public class LandingModule {

    private final static double TIME_STEP = 1;
    private final static double GRAVITY = 1.352E-2;

    private Vector2 velocity;
    private Vector2 position;
    private double mass = 100;

    public Thruster upThruster = new Thruster(Direction.Y_POS, 0.5);
    public Thruster leftThruster = new Thruster(Direction.X_POS, 0.1);
    public Thruster rightThruster = new Thruster(Direction.X_NEG, 0.1);

    public LandingModule(Vector2 position, Vector2 velocity) {
        this.velocity = velocity;
        this.position = position;
    }

    public void updatePosition() {

        this.position = this.position.add(this.velocity.mul(TIME_STEP));

    }

    public void updateVelocity() {

        this.velocity = this.velocity.add(new Vector2(0, GRAVITY).mul(mass).mul(-1D/this.getPosition().getY()));
        this.velocity = this.velocity.add(upThruster.getThrust());
        this.velocity = this.velocity.add(leftThruster.getThrust());
        this.velocity = this.velocity.add(rightThruster.getThrust());

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

    public class Thruster {

        private Direction direction;
        private double force;
        private double timeToBurn = 0;

        public Thruster(Direction direction, double force) {
            this.direction = direction;
            this.force = force;
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
}

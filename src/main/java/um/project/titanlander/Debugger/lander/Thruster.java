package um.project.titanlander.Debugger.lander;

import um.project.titanlander.Debugger.Vector2;

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
        this.timeToBurn = Math.min(seconds, LandingModule.TIME_STEP);
    }

    public Vector2 getThrust(double mass) {
        Vector2 v = new Vector2(0, 0);
        if(timeToBurn > 0) {
            v = direction.direction().mul(force).mul(Math.min(LandingModule.TIME_STEP, timeToBurn)).div(mass);
        }
        return v;
    }

    public Vector2 getAngularForce(double mass) {
        return direction.direction().mul(force).div(mass).div(Math.sqrt(2)).mul(1D / 12D);
    }

    public Vector2 getAngularThrust(double mass) {
        Vector2 t = new Vector2();
        if(timeToBurn > 0) {
            t = direction.direction().mul(force).div(mass).div(Math.sqrt(2)).mul(1D / 12D).mul(Math.min(LandingModule.TIME_STEP, timeToBurn));
        }
        return t;
    }

    public void update() {
        if(timeToBurn != 0) {
            timeToBurn -= LandingModule.TIME_STEP;
            timeToBurn = Math.max(timeToBurn, 0);
        }
    }

    public boolean isBurning() {
        return this.timeToBurn > 0;
    }

}

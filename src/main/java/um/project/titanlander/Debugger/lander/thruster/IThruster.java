package um.project.titanlander.Debugger.lander.thruster;

import um.project.titanlander.Debugger.Vector2;
import um.project.titanlander.Debugger.lander.Direction;
import um.project.titanlander.Debugger.lander.LandingModule;

public abstract class IThruster<F,T> {

    private Direction direction;
    private double force;
    private double timeToBurn = 0;

    public IThruster(Direction direction, double force, double mass) {
        this.direction = direction;
        this.force = force;
    }

    public double getTimeToBurn() {
        return this.timeToBurn;
    }

    public Direction getDirection() {
        return direction;
    }

    public double getRawForce() {
        return force;
    }

    public final void burn(double seconds) {
        this.timeToBurn = Math.min(seconds, LandingModule.TIME_STEP);
    }

    public abstract F getForce(double mass);

    public abstract T getThrust(double mass);

    public final void update() {
        if(timeToBurn != 0) {
            timeToBurn -= LandingModule.TIME_STEP;
            timeToBurn = Math.max(timeToBurn, 0);
        }
    }

    public final boolean isBurning() {
        return this.timeToBurn > 0;
    }

}

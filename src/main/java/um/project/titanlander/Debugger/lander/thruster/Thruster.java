package um.project.titanlander.Debugger.lander.thruster;

import um.project.titanlander.Debugger.Vector2;
import um.project.titanlander.Debugger.lander.Direction;
import um.project.titanlander.Debugger.lander.LandingModule;

public class Thruster extends IThruster<Double, Vector2> {

    public Thruster(Direction direction, double force, double mass) {
        super(direction, force, mass);
    }

    @Override
    public Double getForce(double mass) {
        return getRawForce() / mass;
    }

    @Override
    public Vector2 getThrust(double mass) {
        Vector2 v = new Vector2(0, 0);
        if(isBurning()) {
            v = getDirection().direction().mul(getRawForce()).mul(Math.min(LandingModule.TIME_STEP, getTimeToBurn())).div(mass);
        }
        return v;
    }

}

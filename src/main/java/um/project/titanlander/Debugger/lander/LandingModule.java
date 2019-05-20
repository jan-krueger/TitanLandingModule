package um.project.titanlander.Debugger.lander;

import um.project.titanlander.Debugger.DataLogger;
import um.project.titanlander.Debugger.Vector3;
import um.project.titanlander.Debugger.lander.thruster.RotationThruster;
import um.project.titanlander.Debugger.lander.thruster.Thruster;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class LandingModule {

    public final static double TIME_STEP = 1;
    private final static double GRAVITY = 1.352E-2; //0.01352m/s^2

    private boolean isLanded = false;

    private Vector3 velocity;
    private Vector3 position;

    private Vector3 realPositions;
    private Vector3 realVelocity;

    private DataLogger dataLogger = new DataLogger();

    private double targetTheta = 0;
    private double theta = Math.PI/20; // rotation
    private double thetaVelocity = Math.PI/10 * TIME_STEP;

    private double mass = 300;
    private double height=3;

    public Thruster downThruster = new Thruster(Direction.Y_POS, 400, mass);
    public Thruster leftThruster = new Thruster(Direction.X_POS, 200, mass);
    public Thruster rightThruster = new Thruster(Direction.X_NEG, 200, mass);
    public Thruster frontThruster = new Thruster(Direction.Z_POS, 200, mass);
    public Thruster backThruster = new Thruster(Direction.Z_NEG, 200, mass);

    public RotationThruster leftRotation = new RotationThruster(Direction.X_NEG, 5.55361, mass, Math.sqrt(2), height);
    public RotationThruster rightRotation = new RotationThruster(Direction.X_POS, 5.55361, mass, Math.sqrt(2), height);

    private ControllerMode controllerMode;

    public LandingModule(Vector3 position, Vector3 velocity, ControllerMode controllerMode) {
        this.realVelocity = velocity;
        this.realPositions = position;
        this.controllerMode = controllerMode;

        this.velocity = velocity.copy();
        this.position = position.copy();
    }

    public Vector3 getRealPositions() {
        return realPositions;
    }

    public Vector3 getRealVelocity() {
        return realVelocity;
    }

    public double getHeight() {
        return height;
    }

    public void setTheta(double theta) {
        this.theta = Math.atan2(Math.sin(theta), Math.cos(theta));
    }


    public void updateController() {

        final double distanceY = Math.abs(this.getPosition().getY());

        if(distanceY < 1E-2) {
            this.isLanded = true;
            return;
        }

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


        // --- Rotation
        final double thetaToZero = Math.abs((theta-targetTheta) / thetaVelocity);
        final double halfToZero = Math.abs(Math.PI / leftRotation.getForce());
        if(Math.abs((theta-targetTheta)) > 1E-4) {
            final double thetaBreakingTime = Math.abs(thetaVelocity / leftRotation.getForce()); // assumes both thrusters are equally strong
            final double thetaDistance = Math.abs((theta-targetTheta));

            //--- Turning right
            final double rotationVelocityError = Math.toRadians(1E-3);
            final double rotationError = Math.toRadians(1E-3);
            if(thetaVelocity > rotationVelocityError) {

                // correct direction
                if((theta-targetTheta) < -rotationError) {
                    //Do we have to break?
                    if(thetaBreakingTime >= thetaToZero) {
                        leftRotation.burn(Math.abs(leftRotation.getForce() / ((thetaDistance / thetaBreakingTime) / thetaBreakingTime) - thetaVelocity));
                    }
                    //Do we have to accelerate?
                }
                // wrong direction
                else if((theta-targetTheta) > rotationError) {
                    //breaking
                    leftRotation.burn(Math.abs(thetaVelocity / leftRotation.getForce()));
                }

            } else if(thetaVelocity < -rotationVelocityError) {

                // correct direction
                if((theta-targetTheta) > rotationError) {
                    //Do we have to break?
                    if(thetaBreakingTime >= thetaToZero) {
                        rightRotation.burn(Math.abs(rightRotation.getForce() / ((thetaDistance / thetaBreakingTime) / thetaBreakingTime) - thetaVelocity));
                    }
                    //Do we have to accelerate?
                }
                // wrong direction
                else if((theta-targetTheta) < -rotationError) {
                    //breaking
                    rightRotation.burn(Math.abs(thetaVelocity / rightRotation.getForce()));
                }

            } else {

                if(Math.abs(thetaVelocity) < rotationVelocityError) {
                    if((theta-targetTheta) > rotationError) {
                        leftRotation.burn(Math.abs((theta-targetTheta) / leftRotation.getForce()));
                    } else if((theta-targetTheta) < -rotationError) {
                        rightRotation.burn(Math.abs((theta-targetTheta) / rightRotation.getForce()));
                    }
                }

            }
        }

        final double timeToY = (distanceY / Math.abs(this.getVelocity().getY()));
        final double yToZero = Math.abs(this.getVelocity().getY() / (this.downThruster.getForce())) ;
        final double timeToX = Math.abs(this.getPosition().getX()) / Math.abs(this.getVelocity().getX());
        final double timeToZ = Math.abs(this.getPosition().getZ()) / Math.abs(this.getVelocity().getZ());
        if((yToZero + halfToZero + timeToX + timeToZ) * 2 >= timeToY) {
            this.targetTheta = 0;
        }

        // TODO 5 degrees seems alright, we should do some calculations to figure out what is acceptable and not just some
        //  magic number
        if(Math.abs((theta-targetTheta)) > Math.toRadians(5)) {
            return;
        }

        // --- Y-burn
        final double signedYForce = this.getVelocity().getY() * mass;

        if((signedYForce < 0 && timeToY <= yToZero) && distanceY > 0.5) {
            if(Math.abs(theta) <= Math.toRadians(5)) {
                downThruster.burn(timeToY);
                System.out.println("landing burn");
            } else {
                targetTheta = 0;
            }
        } else if((yToZero + halfToZero + timeToX + timeToZ) < timeToY) {
            if(Math.abs(targetTheta - Math.PI) <= Math.toRadians(1)) {
                this.downThruster.burn(TIME_STEP);
            } else {
                //System.out.println("ROTATE");
                //this.targetTheta = Math.toRadians(179.5);
            }
        }

        //Horizontal Translation
        // Note: Depending on the orientation of the module, we have to fire opposite thrusters.
        if(Math.abs(theta) <= Math.toRadians(1)) {
            controlHorizontalAxis(Vector3.Component.X, leftThruster, rightThruster, Math.abs(getPosition().getX()), timeToY, this.getVelocity().getX() * mass, this.getVelocity().getX());
            controlHorizontalAxis(Vector3.Component.Z, frontThruster, backThruster, Math.abs(getPosition().getZ()), timeToY, this.getVelocity().getZ() * mass, this.getVelocity().getZ());
        } else if(Math.abs(theta-Math.PI) <= Math.toRadians(5)) {
            controlHorizontalAxis(Vector3.Component.X, rightThruster, leftThruster, Math.abs(getPosition().getX()), timeToY, this.getVelocity().getX() * mass, this.getVelocity().getX());
            controlHorizontalAxis(Vector3.Component.Z, backThruster, frontThruster, Math.abs(getPosition().getZ()), timeToY, this.getVelocity().getZ() * mass, this.getVelocity().getZ());
        }


    }

    private void controlHorizontalAxis(Vector3.Component axis, Thruster positiveThruster, Thruster negativeThruster, double distanceToAxis, double yBreakingTime,
                                       double aF, double aV) {

        if(distanceToAxis <= 0) {
            return;
        }

        final double velocityLimit = 1E-3;
        final double timeToA = (distanceToAxis / Math.abs(aV));

        if (aV > velocityLimit) { //moving right
            // Do we overstep in the next update?
            if (this.getPosition().add(this.velocity.mul(TIME_STEP)).get(axis) > 0) {
                negativeThruster.burn(Math.abs(aF / (negativeThruster.getForce())));
            } else if (this.getVelocity().get(axis) / (negativeThruster.getForce()) <= TIME_STEP) {
                final double maxThrust = (negativeThruster.getForce());
                final double thrustDelta = maxThrust - Math.abs(aF);
                positiveThruster.burn(Math.abs((thrustDelta) / positiveThruster.getForce()));
            }
            // Are we too slow to get to x=0 in time? -> Accelerate
            else if(timeToA <= (Math.abs(this.getVelocity().get(axis))/ negativeThruster.getForce())) {
                negativeThruster.burn( negativeThruster.getForce() / Math.abs(((distanceToAxis / timeToA) / timeToA) - Math.abs(this.getVelocity().get(axis))));
            }
            // Are we going too fast? ->  Decelerate
            else if(timeToA >= yBreakingTime) {
                positiveThruster.burn(positiveThruster.getForce() / Math.abs(((distanceToAxis / yBreakingTime) / yBreakingTime) - Math.abs(this.getVelocity().get(axis))));
            }
        } else if (aV < -velocityLimit) { //moving left
            if (this.getPosition().add(this.velocity.mul(TIME_STEP)).get(axis) < 0) {
                positiveThruster.burn(Math.abs(aF / (positiveThruster.getForce())));
            } else if (Math.abs(this.getVelocity().get(axis)) / (positiveThruster.getForce()) <=  TIME_STEP) {
                final double maxThrust = (positiveThruster.getForce());
                final double thrustDelta = maxThrust - Math.abs(aF);
                negativeThruster.burn(Math.abs((thrustDelta) / negativeThruster.getForce()));
            } else if(timeToA <= (Math.abs(this.getVelocity().get(axis))/ positiveThruster.getForce())) {
                positiveThruster.burn( positiveThruster.getForce() / Math.abs(((distanceToAxis / timeToA) / timeToA) - Math.abs(this.getVelocity().get(axis))));
            } else if(timeToA >= yBreakingTime) {
                negativeThruster.burn(negativeThruster.getForce() / Math.abs(((distanceToAxis / yBreakingTime) / yBreakingTime) - Math.abs(this.getVelocity().get(axis))));
            }
        } else if(distanceToAxis > 0.1) {
            if (this.getPosition().get(axis) < 0) {
                positiveThruster.burn(positiveThruster.getForce() / Math.abs(((distanceToAxis / timeToA) / timeToA) - Math.abs(this.getVelocity().get(axis))));
            } else if (this.getPosition().get(axis) > 0) {
                negativeThruster.burn(negativeThruster.getForce() / Math.abs(((distanceToAxis / yBreakingTime) / yBreakingTime) - Math.abs(this.getVelocity().get(axis))));
            }
        }
    }

    public void updatePosition() {

        if(this.isLanded) return;

        this.position = this.position.add(this.velocity.mul(TIME_STEP));
        if(this.position.getY() < 0) {
            this.position = new Vector3(this.position.getX(), 0, this.position.getZ());
        }

        this.realPositions = this.realPositions.add(this.realVelocity.mul(TIME_STEP));
        if(this.realPositions.getY() < 0) {
            this.realPositions = new Vector3(this.realPositions.getX(), 0, this.position.getZ());
        }

        this.setTheta(this.theta + this.thetaVelocity * TIME_STEP);

    }

    public void updateVelocity() {

        if(this.isLanded) {
            this.velocity = new Vector3();
            this.realVelocity = new Vector3();
            this.thetaVelocity = 0;
            return;
        }

        //--- Thrusters
        final double key = this.getPosition().getY();
        dataLogger.add(key, "realPositionX", realPositions.getX());
        dataLogger.add(key, "realPositionY", realPositions.getY());
        dataLogger.add(key, "realPositionZ", realPositions.getZ());
        dataLogger.add(key, "position", position.getX());

        Vector3 thrustTotal = new Vector3(0, 0, 0);
        {
            Vector3 thrust = applyRotation(downThruster.getThrust());
            dataLogger.add(key, "down", thrust.length());
            thrustTotal = thrustTotal.add(thrust);
        }
        {
            Vector3 thrust = applyRotation(leftThruster.getThrust());
            dataLogger.add(key, "left", -thrust.length());
            thrustTotal = thrustTotal.add(thrust);
        }
        {
            Vector3 thrust = applyRotation(rightThruster.getThrust());
            dataLogger.add(key, "right", thrust.length());
            thrustTotal = thrustTotal.add(thrust);
        }
        {
            Vector3 thrust = applyRotation(backThruster.getThrust());
            dataLogger.add(key, "back", -thrust.length());
            thrustTotal = thrustTotal.add(thrust);
        }
        {
            Vector3 thrust = applyRotation(frontThruster.getThrust());
            dataLogger.add(key, "front", thrust.length());
            thrustTotal = thrustTotal.add(thrust);
        }

        this.realVelocity = this.realVelocity.add(thrustTotal);
        this.velocity = this.velocity.add(thrustTotal);

        //--- Rotation
        {
            double totalRotation = 0;
            totalRotation += leftRotation.getThrust();
            totalRotation += rightRotation.getThrust();
            this.thetaVelocity += totalRotation;
        }


        //--- Gravity
        //v+1 = v + (Gv*m)/deltaY
        Vector3 gravity = new Vector3(0, GRAVITY, 0).mul(mass).div(Math.pow(1287850D-this.getPosition().getY(), 2)).mul(-1);
        this.realVelocity = this.realVelocity.add(gravity);
        this.velocity = this.velocity.add(gravity);


        Vector3 v = wind(getPosition(), mass);
        dataLogger.add(key, "windStrength", v.getX());
        if(!Double.isNaN(v.getX())) {
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
        frontThruster.update();
        backThruster.update();
    }

    private Vector3 applyRotation(Vector3 force) {
        return new Vector3(force.getX() * Math.cos(theta) - force.getY() * Math.sin(theta),
                force.getX() * Math.sin(theta) + force.getY() * Math.cos(theta), force.getZ() * Math.cos(theta) - force.getY() * Math.sin(theta));
    }

    public Vector3 getPosition() {
        return this.position;
    }

    public Vector3 getVelocity() {
        return this.velocity;
    }

    @Override
    public String toString() {
        return String.format("Module\n\tr:[p=%s,v=%s,θ=%.2f,θ'=%.2f]\n\ta:[p=%s,v=%s,θ=%.2f,θ'=%.2f]",
                this.realPositions, this.realVelocity, Math.toDegrees(this.theta), Math.toDegrees(this.thetaVelocity),
                this.position, this.velocity, Math.toDegrees(this.theta), Math.toDegrees(this.thetaVelocity));
    }

    public static Vector3 wind(Vector3 pos, double mass) {
        final double speed = 2D * (Math.log(pos.getY() / 0.15D)/Math.log(20D/0.15D));
        final Vector3 dir = new Vector3(0.1 * Math.sin(pos.getY()/100D), 0, 0).normalise();
        return dir.mul(speed).div(mass).mul(TIME_STEP);
    }

    public DataLogger getDataLogger() {
        return this.dataLogger;
    }

    public double getRotation() {
        return this.theta;
    }
}

package um.project.titanlander.Debugger;

public class Vector3 {

    private final double x;
    private final double y;
    private final double z;
    private final double length;

    public Vector3() {
        this(0, 0, 0);
    }

    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.length = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
    }

    public double get(Component component) {
        switch (component) {

            case X: return this.x;
            case Y: return this.y;
            case Z: return this.z;
        }

        throw new IllegalArgumentException();

    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public Vector3 add(Vector3 v) {
        return new Vector3(this.x + v.getX(), this.y + v.getY(), this.z + v.getZ());
    }

    public Vector3 sub(Vector3 v) {
        return new Vector3(this.x - v.getX(), this.y - v.getY(), this.z - v.getZ());
    }

    public Vector3 normalise() {
        return new Vector3(this.x / this.length, this.y / this.length, this.z / this.length);
    }

    public Vector3 mul(Vector3 v) {
        return new Vector3(this.x * v.getX(), this.y * v.getY(), this.z * v.getZ());
    }

    public Vector3 mul(double x) {
        return new Vector3(this.x * x, this.y * x, this.z * x);
    }

    public Vector3 div(double x) {
        return new Vector3(this.x / x, this.y / x, this.z / x);
    }

    public double length() {
        return this.length;
    }

    public Vector3 copy() {
        return new Vector3(this.x, this.y, this.z);
    }

    @Override
    public String toString() {
        return String.format("Vec2[x=%.4f,y=%.2f,z=%.2f]", this.x, this.y, this.z);
    }

    public enum Component {
        X,Y,Z
    }

}

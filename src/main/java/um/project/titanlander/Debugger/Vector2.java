package um.project.titanlander.Debugger;

public class Vector2 {

    private final double x;
    private final double y;
    private final double length;

    public Vector2() {
        this(0, 0);
    }

    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
        this.length = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return y;
    }

    public Vector2 add(Vector2 x) {
        return new Vector2(this.x + x.getX(), this.y + x.getY());
    }

    public Vector2 sub(Vector2 x) {
        return new Vector2(this.x - x.getX(), this.y - x.getY());
    }

    public Vector2 normalise() {
        return new Vector2(this.x / this.length, this.y / this.length);
    }

    public Vector2 mul(Vector2 x) {
        return new Vector2(this.x * x.getX(), this.y * x.getY());
    }

    public Vector2 mul(double x) {
        return new Vector2(this.x * x, this.y  * x);
    }

    public Vector2 div(double x) {
        return new Vector2(this.x / x, this.y / x);
    }

    public double length() {
        return this.length;
    }

    public Vector2 copy() {
        return new Vector2(this.x, this.y);
    }

    @Override
    public String toString() {
        return String.format("Vec2[x=%.4f,y=%.2f]", this.x, this.y);
    }
}

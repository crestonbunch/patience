package im.bunch.patience.geometry;

/**
 * Represents a point in 3d space
 *
 * @author Creston Bunch
 */
public class Point {

    public final double x, y, z;

    /**
     * Create a point with given x, y, and z position.
     * @param x
     * @param y
     * @param z
     */
    public Point(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Create a point in 2d space with only x and y. The z position gets set to 0.
     *
     * @param x
     * @param y
     */
    public Point(double x, double y) {
        this(x, y, 0);
    }

    /**
     * Calculate distance to another point.
     *
     * @param other
     * @return
     */
    public double distanceTo(Point other) {
        return Math.sqrt(
                Math.pow(this.x - other.x, 2)
                + Math.pow(this.y - other.y, 2)
                + Math.pow(this.z - other.z, 2)
        );
    }

    public String toString() {
        return "(" + Double.toString(this.x) + ","
                + Double.toString(this.y) + ","
                + Double.toString(this.z) + ")";
    }
}

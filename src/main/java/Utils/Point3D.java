package Utils;

public class Point3D {
    public double x, y, z;
    public Point3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    @Override
    public boolean equals(Object o) {
        if (o.getClass() != Point3D.class) return false;
        Point3D p = (Point3D) o;
        return p.x == this.x && p.y == this.y && p.z == this.z;
    }
    @Override
    public String toString() {
        return String.format("(%.1f, %.1f, %.1f)", x, y, z);
    }

    public static Point3D findMiddlePoint(Point3D a, Point3D b, double distanceX) {
        // gives the point at distanceX from point A in the direction of A to B.

        // direction vector (v = B - A)
        // (vx, vy, vz)
        double vx = b.x - a.x;
        double vy = b.y - a.y;
        double vz = b.z - a.z;

        // magnitude of the vector
        // |v| = sqrt(vx^2 + vy^2 + vz^2)
        double magnitude = Math.sqrt(vx * vx + vy * vy + vz * vz);

        // if A and B are the same point (magnitude is zero)
        if (magnitude == 0) return a;

        //unit vector (u = v / |v|)
        // a unit vector has a length of 1 and the same direction as v
        double ux = vx / magnitude;
        double uy = vy / magnitude;
        double uz = vz / magnitude;

        // scale the unit vector and add it to A
        // P = A + (distanceX * u)
        double px = a.x + (distanceX * ux);
        double py = a.y + (distanceX * uy);
        double pz = a.z + (distanceX * uz);

        return new Point3D(px, py, pz);
    }
}

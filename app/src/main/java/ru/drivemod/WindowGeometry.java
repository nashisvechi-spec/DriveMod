package ru.drivemod;

final class WindowGeometry {
    static boolean separated(int[] a, int[] b, int width, int height) {
        if (a == null || b == null || width <= 0 || height <= 0) return false;
        long screen = (long) width * height;
        long areaA = area(a), areaB = area(b);
        if (areaA < screen * .12 || areaB < screen * .12) return false;
        long intersection = Math.max(0, Math.min(a[2], b[2]) - Math.max(a[0], b[0]))
                * (long) Math.max(0, Math.min(a[3], b[3]) - Math.max(a[1], b[1]));
        return intersection < Math.min(areaA, areaB) * .06 && areaA + areaB > screen * .45;
    }
    static long area(int[] rect) { return (long) Math.max(0, rect[2] - rect[0]) * Math.max(0, rect[3] - rect[1]); }
}

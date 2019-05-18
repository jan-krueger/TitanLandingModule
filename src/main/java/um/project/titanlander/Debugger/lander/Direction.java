package um.project.titanlander.Debugger.lander;

import um.project.titanlander.Debugger.Vector2;

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


package org.boxutil.units.builtin.legacy.array;

public class TriIndex {
    public Stack3i[] index = new Stack3i[3];

    public TriIndex(String p1, String p2, String p3) {
        String[] splitString1 = p1.split("/");
        String[] splitString2 = p2.split("/");
        String[] splitString3 = p3.split("/");
        this.index[0] = new Stack3i(Integer.parseInt(splitString1[0]) - 1, Integer.parseInt(splitString1[2]) - 1, Integer.parseInt(splitString1[1]) - 1);
        this.index[1] = new Stack3i(Integer.parseInt(splitString2[0]) - 1, Integer.parseInt(splitString2[2]) - 1, Integer.parseInt(splitString2[1]) - 1);
        this.index[2] = new Stack3i(Integer.parseInt(splitString3[0]) - 1, Integer.parseInt(splitString3[2]) - 1, Integer.parseInt(splitString3[1]) - 1);
    }

    public Stack3i[] getIndex() {
        return this.index;
    }
}

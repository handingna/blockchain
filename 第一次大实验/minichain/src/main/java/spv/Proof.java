package spv;

import java.util.List;

public class Proof {
    public enum Orientation{
        LEFT, RIGHT;
    }
    public static class Node{
        private final String txHash;
        private final Orientation orientation;

        public Node(String txHash, Orientation orientation){
            this.orientation=orientation;
            this.txHash=txHash;
        }
        public String getTxHash(){return txHash;}

        public Orientation getOrientation() {
            return orientation;
        }
    }

    private final String txHash;
    private final String merkleRootHash;

    private final int height;
    private final List<Node> path;

    public Proof(String txHash, String merkleRootHash, int height, List<Node> path){
        this.height=height;
        this.txHash=txHash;
        this.path=path;
        this.merkleRootHash=merkleRootHash;
    }

    public int getHeight() {
        return height;
    }

    public String getTxHash() {
        return txHash;
    }

    public String getMerkleRootHash() {
        return merkleRootHash;
    }

    public List<Node> getPath() {
        return path;
    }

}

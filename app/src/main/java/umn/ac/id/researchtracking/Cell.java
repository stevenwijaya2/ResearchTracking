package umn.ac.id.researchtracking;

public class Cell {
    private String cellid;
    private int rss;

    public Cell(){}
    public Cell(String cellid,int rss){
        this.cellid = cellid;
        this.rss = rss;
    }

    public String getCellid() {
        return cellid;
    }

    public int getRss() {
        return rss;
    }

    public void setCellid(String cellid) {
        this.cellid = cellid;
    }

    public void setRss(int rss) {
        this.rss = rss;
    }
}




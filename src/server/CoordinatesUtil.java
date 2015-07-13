package server;

import java.io.Serializable;

/**
 * @author Srinivasan Venkatraman / Ashwini Ravi
 * 
 */
public class CoordinatesUtil implements Serializable,Comparable<CoordinatesUtil>{

	private static final long serialVersionUID = -4033062113728014563L;
	
	private int rowCoordinate;
	 
	private int columnCoordinate;
	 
	 public CoordinatesUtil(int rowCoordinate, int columnCoordinate){		 
		 this.rowCoordinate = rowCoordinate;		 
		 this.columnCoordinate = columnCoordinate;
	 }

	@Override
	public int compareTo(CoordinatesUtil o) {		
		if(this.getRowCoordinate() != o.getRowCoordinate()){			
			return this.getRowCoordinate() - o.getRowCoordinate();
		}else{			
			return this.getColumnCoordinate() - o.getColumnCoordinate();
		}
	}
	
	
	public boolean equals(Object o){		
		if(o instanceof CoordinatesUtil){
			CoordinatesUtil c = (CoordinatesUtil) o;
			if(this.getRowCoordinate() == c.getRowCoordinate() && this.getColumnCoordinate() == c.getColumnCoordinate()){
				return true;
			}
		}
		return false;	
	}

	public int getRowCoordinate() {
		return rowCoordinate;
	}

	public void setRowCoordinate(int rowCoordinate) {
		this.rowCoordinate = rowCoordinate;
	}

	public int getColumnCoordinate() {
		return columnCoordinate;
	}

	public void setColumnCoordinate(int columnCoordinate) {
		this.columnCoordinate = columnCoordinate;
	}
	

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package RNA_Scope_Utils;

/**
 *
 * @author phm
 */
public class Dot {
    private int index;
    private double volDot;
    private double intDot;
    private int zMin;
    private int zMax;
    private double zCenter;
   
	
	public Dot(int index, double volDot, double intDot, int zMin, int zMax, double zCenter) {
            this.index = index;
            this.volDot = volDot;
            this.intDot = intDot;
            this.zMin = zMin;
            this.zMax = zMax;
            this.zCenter = zCenter;
	}
        
        public void setIndex(int index) {
		this.index = index;
	}
        
        public void setVolDot(double volDot) {
		this.volDot = volDot;
	}
                
        public void setZmin(int zMin) {
		this.zMin = zMin;
	}
        
	public void setZmax(int zMax) {
		this.zMax = zMax;
	}
        
        public void setZCenter(double zCenter) {
		this.zCenter = zCenter;
	}
        
        public int getIndex() {
            return index;
        }
        
        public double getVolDot() {
            return volDot;
        }
        
        public double getIntDot() {
            return intDot; 
        }
        
	public int getZmin() {
		return zMin;
	}

	public int getZmax() {
		return zMax;
	}

        public double getZCenter() {
		return zCenter;
	}
}

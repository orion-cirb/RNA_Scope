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
public class Nucleus {
    // index
    private int index;
   
    // volume in pixels
    private double nucVol;
    
    // Reference channel
    // nuc integrated intensity in gene reference channel
    private double nucGeneRefInt;
    // nuc mean background intensity in gene reference channel
    private double nucGeneRefBgInt;
    // dots volume in pixels
    private double geneRefDotsVol;
    // integrated intensity of total gene reference dots
    private double geneRefDotsInt;
   
    
    // Gene X channel
   
    // nuc integrated intensity in gene X channel
    private double nucGeneXInt;
    // nuc mean background intensity in gene X channel
    private double nucGeneXBgInt;
    // dots volume in pixels
    private double geneXDotsVol;
    // integrated intensity of total gene X dots
    private double geneXDotsInt;
    
    // Gene Y channel
   
    // nuc integrated intensity in gene Y channel
    private double nucGeneYInt;
    // nuc mean background intensity in gene Y channel
    private double nucGeneYBgInt;
    // dots volume in pixels
    private double geneYDotsVol;
    // integrated intensity of total gene y dots
    private double geneYDotsInt;
    
    //number of dots in nuc based on nuc intensity channel
    private int nbGeneRefDotsNucInt;
    private int nbGeneXDotsNucInt;
    private int nbGeneYDotsNucInt;
    
    // number of dots in nuc based on dots segmentation intensity
    private int nbGeneRefDotsSegInt;
    private int nbGeneXDotsSegInt;
    private int nbGeneYDotsSegInt;
	
	public Nucleus(int index, double nucVol, double nucGeneRefInt, double nucGeneRefBgInt, double geneRefDotsVol, double geneRefDotsInt,
                int nbGeneRefDotsNucInt, int nbGeneRefDotsSegInt, double nucGeneXInt, double nucGeneXBgInt, double geneXDotsVol, double geneXDotsInt, 
                int nbGeneXDotsNucInt, int nbGeneXDotsSegInt, double nucGeneYInt, double nucGeneYBgInt, double geneYDotsVol, double geneYDotsInt,
                int nbGeneYDotsNucInt, int nbGeneYDotsSegInt) {
            this.index = index;
            this.nucVol = nucVol;
            this.nucGeneRefInt = nucGeneRefInt;
            this.nucGeneRefBgInt = nucGeneRefBgInt;
            this.geneRefDotsVol = geneRefDotsVol;
            this.geneRefDotsInt = geneRefDotsInt;
            this.nbGeneRefDotsNucInt = nbGeneRefDotsNucInt;
            this.nbGeneRefDotsSegInt = nbGeneRefDotsSegInt;
            this.nucGeneXInt = nucGeneXInt;
            this.nucGeneXBgInt = nucGeneXBgInt;
            this.geneXDotsVol = geneXDotsVol;
            this.geneXDotsInt = geneXDotsInt;
            this.nbGeneXDotsNucInt = nbGeneXDotsNucInt;
            this.nbGeneXDotsSegInt = nbGeneXDotsSegInt;
            this.nucGeneYInt = nucGeneYInt;
            this.nucGeneYBgInt = nucGeneYBgInt;
            this.geneYDotsVol = geneYDotsVol;
            this.geneYDotsInt = geneYDotsInt;
            this.nbGeneYDotsNucInt = nbGeneYDotsNucInt;
            this.nbGeneYDotsSegInt = nbGeneYDotsSegInt;
	}
        
        public void setIndex(int index) {
            this.index = index;
	}
        
        public void setNucVol(double nucVol) {
            this.nucVol = nucVol;
	}
        
        public void setNucGeneRefInt(double nucGeneRefInt) {
            this.nucGeneRefInt = nucGeneRefInt;
	}
        
        public void setNucGeneRefBgInt(double nucGeneRefBgInt) {
            this.nucGeneRefBgInt = nucGeneRefBgInt;
	}
        
        public void setGeneRefDotsVol(double geneRefDotsVol) {
            this.geneRefDotsVol = geneRefDotsVol;
        }
        
        public void setGeneRefDotsInt(double geneRefDotsInt) {
            this.geneRefDotsInt = geneRefDotsInt;
        }
        
        public void setnbGeneRefDotsNucInt(int nbGeneRefDotsNucInt) {
            this.nbGeneRefDotsNucInt = nbGeneRefDotsNucInt;
        }
        
        public void setnbGeneRefDotsSegInt(int nbGeneRefDotsSegInt) {
            this.nbGeneRefDotsNucInt = nbGeneRefDotsNucInt;
        }
        
        public void setNucGeneXInt(double nucGeneXInt) {
            this.nucGeneXInt = nucGeneXInt;
	}
        
        public void setNucGeneXBgInt(double nucGeneXBgInt) {
            this.nucGeneXBgInt = nucGeneXBgInt;
	}
        
        public void setGeneXDotsVol(double geneXDotsVol) {
            this.geneXDotsVol = geneXDotsVol;
        }
        
        public void setGeneXDotsInt(double geneXDotsInt) {
            this.geneXDotsInt = geneXDotsInt;
        }

        public void setnbGeneXDotsNucInt(int nbGeneXDotsNucInt) {
            this.nbGeneXDotsNucInt = nbGeneXDotsNucInt;
        }
        
        public void setnbGeneXDotsSegInt(int nbGeneXDotsSegInt) {
            this.nbGeneXDotsNucInt = nbGeneXDotsNucInt;
        }
        
        public void setNucGeneYInt(double nucGeneYInt) {
            this.nucGeneYInt = nucGeneYInt;
	}
        
        public void setNucGeneYBgInt(double nucGeneYBgInt) {
            this.nucGeneYBgInt = nucGeneYBgInt;
	}
        
        public void setGeneYDotsVol(double geneYDotsVol) {
            this.geneYDotsVol = geneYDotsVol;
        }
        
        public void setGeneYDotsInt(double geneYDotsInt) {
            this.geneYDotsInt = geneYDotsInt;
        }

        public void setnbGeneYDotsNucInt(int nbGeneYDotsNucInt) {
            this.nbGeneYDotsNucInt = nbGeneYDotsNucInt;
        }
        
        public void setnbGeneYDotsSegInt(int nbGeneYDotsSegInt) {
            this.nbGeneYDotsNucInt = nbGeneYDotsNucInt;
        }
        
        public int getIndex() {
            return index;
        }
        
        public double getNucVol() {
            return nucVol;
        }
        
        public double getNucGeneRefInt() {
            return nucGeneRefInt;
	}
        
        public double getNucGeneRefBgInt() {
            return nucGeneRefBgInt;
	}
        
        public double getGeneRefDotsVol() {
            return geneRefDotsVol;
        }
        
        public double getGeneRefDotsInt() {
            return geneRefDotsInt;
        }
        
        public int getnbGeneRefDotsNucInt() {
            return nbGeneRefDotsNucInt;
        }
        
        public int getnbGeneRefDotsSegInt() {
            return nbGeneRefDotsSegInt;
        }
        
        public double getNucGeneXInt() {
            return nucGeneXInt;
	}
        
        public double getNucGeneXBgInt() {
            return nucGeneXBgInt;
	}
        
        public double getGeneXDotsVol() {
            return geneXDotsVol;
        }
        
        public double getGeneXDotsInt() {
            return geneXDotsInt;
        }
        
        public int getnbGeneXDotsNucInt() {
            return nbGeneXDotsNucInt;
        }
        
        public int getnbGeneXDotsSegInt() {
            return nbGeneXDotsSegInt;
        }
        
        public double getNucGeneYInt() {
            return nucGeneYInt;
	}
        
        public double getNucGeneYBgInt() {
            return nucGeneYBgInt;
	}
        
        public double getGeneYDotsVol() {
            return geneYDotsVol;
        }
        
        public double getGeneYDotsInt() {
            return geneYDotsInt;
        }
        
        public int getnbGeneYDotsNucInt() {
            return nbGeneYDotsNucInt;
        }
        
        public int getnbGeneYDotsSegInt() {
            return nbGeneYDotsSegInt;
        }
}

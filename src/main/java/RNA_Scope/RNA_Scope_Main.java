package RNA_Scope;

/*
 * Find gene
 * 
 * Author Philippe Mailly
 */
import RNA_Scope_Utils.Cell;
import ij.*;
import ij.plugin.PlugIn;
import java.io.BufferedWriter;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;


public class RNA_Scope_Main implements PlugIn {

    public RNA_Scope_Main instance;
    
    
    // parameters
    
    public  String outDirResults = "";
    public  String rootName = "";
    public  final double pixDepth = 0.5;
    public  Cell nucleus = new Cell(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    public  BufferedWriter output_detail_Analyze;
    public  boolean localImages = false;
    public  String imagesFolder;
    public  List<String> imagesFiles = new ArrayList<>();
    public boolean dialogCancel = false;
    public List<String> channels = new ArrayList<>();
    public String autoBackground = "";
    public String nucThMethod = "Huang", geneThMethod = "Isodata";
    public double singleDotIntGeneRef = 0, singleDotIntGeneX = 0;
    public double minNucVol = 50, maxNucVol = 900;
    public double calibBgGeneRef = 0, calibBgGeneX = 0;
    public double minGeneVol = 0.01, maxGeneVol = 50;
    public double DOGMin = 2, DOGMax = 4;
    public int roiBgSize = 100;
    public float nucDil = 3;
    public int removeSlice = 0;
    public boolean nucNumber = true;
    public String nucSegMethod = "StarDist";
    public String geneSegMethod = "StarDist";
       
    /**
     * 
     * @param arg
     */
    @Override
    public void run(String arg) {
        
        RNA_Scope_JDialog dialog = new RNA_Scope_JDialog(new Frame(), true);
        dialog.show();
        if (dialogCancel){
            IJ.showStatus(" Pluging canceled");
            return;
        }

        /* 
        * Images on local machine
        */

        if (localImages) {
            new RNA_Scope_Local().run("");
        }
        
        /*
        Images on OMERO server
        */

        else {
            new RNA_Scope_Omero().run("");     
        }

        IJ.showStatus("Process done");
    }
}
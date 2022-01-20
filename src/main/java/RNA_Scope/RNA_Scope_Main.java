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
    
     public static RNA_Scope_Main instance;
    // parameters
    
    public  static String outDirResults = "";
    public  static String rootName = "";
    public  static final double pixDepth = 0.5;
    public  static Cell nucleus = new Cell(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    public  static BufferedWriter output_detail_Analyze;
    public  static boolean localImages = false;
    public  static String imagesFolder;
    public  static List<String> imagesFiles = new ArrayList<>();
    public static List<String> channels = new ArrayList<>();
    public static String autoBackground = "";
    public static String nucThMethod = "Huang", geneThMethod = "Isodata";
    public static double singleDotIntGeneRef = 0, singleDotIntGeneX = 0;
    public static double minNucVol = 50, maxNucVol = 900;
    public static double calibBgGeneRef = 0, calibBgGeneX = 0;
    public static double minGeneVol = 0.01, maxGeneVol = 50;
    public static double DOGMin = 2, DOGMax = 4;
    public static int roiBgSize = 100;
    public static float nucDil = 3;
    public static int removeSlice = 0;
    public static boolean nucNumber = true;
    public static String nucSegMethod = "StarDist";
    public static String geneSegMethod = "StarDist";
    public static String stardistModelNucleus = "";
    public static String stardistModelGenes = "";
       
    /**
     * 
     * @param arg
     */
    @Override
    public void run(String arg) {
        
        RNA_Scope_JDialog dialog = new RNA_Scope_JDialog(new Frame(), true);
        dialog.show();
        if (dialog.dialogCancel){
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
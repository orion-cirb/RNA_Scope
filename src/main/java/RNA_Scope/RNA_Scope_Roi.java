/*
 * Measure integrated intensities in Rois
 * For Gene reference and gene x
 */
package RNA_Scope;



import static RNA_Scope.RNA_Scope_Main.cal;
import static RNA_Scope.RNA_Scope_Main.singleDotIntGeneRef;
import static RNA_Scope.RNA_Scope_Main.singleDotIntGeneX;
import static RNA_Scope_Utils.Image_Utils.findChannels;
import static RNA_Scope_Utils.Image_Utils.findImageCalib;
import static RNA_Scope_Utils.Image_Utils.findImages;
import static RNA_Scope_Utils.RNA_Scope_Processing.closeImages;
import static RNA_Scope_Utils.RNA_Scope_Processing.findGenePop;
import static RNA_Scope_Utils.RNA_Scope_Processing.find_background;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.RGBStackMerge;
import ij.plugin.Thresholder;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import loci.common.Region;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import loci.plugins.util.ImageProcessorReader;
import mcib3d.geom.Object3D;
import mcib3d.geom.Objects3DPopulation;
import mcib3d.image3d.ImageHandler;
import org.apache.commons.io.FilenameUtils;


/**
 *
 * @author phm
 */
public class RNA_Scope_Roi implements PlugIn {
    

private String imageDir = "";
private String outDirResults = "";
public final ImageIcon icon = new ImageIcon(this.getClass().getResource("/Orion_icon.png"));
private String thMet = "Moments";


    /*
    * Integrated intensity
    */
    public static double find_Integrated(ImagePlus img, Roi roi) {
        roi.setLocation(0, 0);
        ResultsTable rt = new ResultsTable();
        Analyzer ana = new Analyzer(img, Measurements.INTEGRATED_DENSITY, rt);
        double intDen = 0; 
        int index = 0;
        roi.setLocation(0, 0);
        for (int z = 1; z <= img.getNSlices(); z++) {
            img.setSlice(z);
            img.setRoi(roi);
            img.updateAndDraw();
            ana.measure();
            intDen += rt.getValue("RawIntDen", index);
            index++;
        }
        System.out.println(roi.getName()+" Raw Int Den = " + intDen);
        return(intDen);  
    }
    
    /*
    * Integrated intensity
    */
    public static double find_Volume(ImagePlus img, Roi roi) {

        ResultsTable rt = new ResultsTable();
        Analyzer ana = new Analyzer(img, Measurements.AREA, rt);
        double vol = 0; 
        int index = 0;
        roi.setLocation(0, 0);
        for (int z = 1; z <= img.getNSlices(); z++) {
            img.setSlice(z);
            img.setRoi(roi);
            img.updateAndDraw();
            ana.measure();
            vol += rt.getValue("Area", index);
            index++;
        }
        vol = vol * img.getNSlices()*img.getCalibration().pixelHeight;
        System.out.println(roi.getName()+" vol = " + vol);
        return(vol);  
    }
    
    /**
     * Dialog ask for channels order
     * @param channels
     * @return ch
     */
    public ArrayList dialog(List<String> channels) {
        ArrayList ch = new ArrayList();
        String[] thMethods = new Thresholder().methods;
        String[] channel = channels.toArray(new String[channels.size()]);
        GenericDialogPlus gd = new GenericDialogPlus("Parameters");
        gd.setInsets​(0, 80, 0);
        gd.addImage(icon);
        gd.addMessage("Channels", Font.getFont("Monospace"), Color.blue);
        gd.addChoice("DAPI           : ", channel, channel[0]);
        if (channels.size() == 3) {
            gd.addChoice("Gene Reference : ", channel, channel[1]);
            gd.addChoice("Gene X         : ", channel, channel[2]);
        }
        else 
            gd.addChoice("Gene X         : ", channel, channel[1]);
        gd.addMessage("Single dot calibration", Font.getFont("Monospace"), Color.blue);
        if (channels.size() == 3) 
            gd.addNumericField("Gene reference single dot mean intensity : ", singleDotIntGeneRef, 0);
        gd.addNumericField("Gene X single dot mean intensity : ", singleDotIntGeneX, 0);
        gd.addChoice("Dots threshold method : ", thMethods, thMet);
        gd.showDialog();
        ch.add(0, gd.getNextChoice());
        if (channels.size() == 3) {
            ch.add(1, gd.getNextChoice());
            ch.add(2, gd.getNextChoice());
        }
        else
           ch.add(1, gd.getNextChoice()); 
        if (channels.size() == 3)
            singleDotIntGeneRef = gd.getNextNumber();
        singleDotIntGeneX = gd.getNextNumber();
        thMet = gd.getNextChoice();
        if(gd.wasCanceled())
            ch = null;
        return(ch);
    }
    
    /**
     * save images objects population
     * @param img
     * @param geneRefPop
     * @param geneXPop
     * @param outDirResults
     * @param rootName
     */
    public static void saveDotsImage (ImagePlus img, Objects3DPopulation geneRefPop, Objects3DPopulation geneXPop,
            String outDirResults, String rootName) {
        // red dots geneRef , dots green geneX, blue nucDilpop
        ImageHandler imgDotsGeneRef = ImageHandler.wrap(img).createSameDimensions();
        ImageHandler imgDotsGeneX = ImageHandler.wrap(img).createSameDimensions();
        if (geneRefPop != null)
            geneRefPop.draw(imgDotsGeneRef, 255);
        geneXPop.draw(imgDotsGeneX, 255);
        ImagePlus[] imgColors = {imgDotsGeneRef.getImagePlus(), imgDotsGeneX.getImagePlus()};
        ImagePlus imgObjects = new RGBStackMerge().mergeHyperstacks(imgColors, false);
        imgObjects.setCalibration(img.getCalibration());
        IJ.run(imgObjects, "Enhance Contrast", "saturated=0.35");

        // Save images
        FileSaver ImgObjectsFile = new FileSaver(imgObjects);
        ImgObjectsFile.saveAsTiff(outDirResults + rootName + "_DotsObjects.tif");
        imgDotsGeneRef.closeImagePlus();
        imgDotsGeneX.closeImagePlus();
    }
    /**
     * Sort roi manager to have bg first
     * @param rm
     * @return 
     */
    
    private RoiManager roiSort(RoiManager rm) {
        RoiManager rmSorted = new RoiManager(true);
        ArrayList<Roi> rois = new ArrayList<>();
        for (int i = 0; i < rm.getCount(); i++) {
            Roi roi = rm.getRoi(i);
            if (rm.getName(i).contains("bg"))
                rmSorted.add(roi, i);
            else
                rois.add(roi);
        }
        int r = 0;
        for (int i = rmSorted.getCount(); i < rm.getCount(); i++) {
            rmSorted.addRoi(rois.get(r));
            r++;
        }
        return(rmSorted);
    }
    
    
    /**
     * find rois with name = serieName
     */
    public static ArrayList<Roi> findRoi(RoiManager rm, String seriesName) {
        ArrayList<Roi> roi = new ArrayList();
        for (int i = 0; i < rm.getCount(); i++) {
            rm.select(i);
            String name = rm.getName(i);
            if (name.contains(seriesName))
                roi.add(rm.getRoi(i));
        }
        return(roi);
    }
    
    
  @Override
    public void run(String arg) {
        try {
            imageDir = IJ.getDirectory("Choose directory containing nd files...");
            if (imageDir == null) {
                return;
            }
            String fileExt = "nd";
            File inDir = new File(imageDir);
            ArrayList<String> imageFiles = findImages(imageDir, fileExt);
            if (imageFiles == null) {
                return;
            }
            
            // create output folder
            outDirResults = inDir + File.separator+ "Results"+ File.separator;
            File outDir = new File(outDirResults);
            if (!Files.exists(Paths.get(outDirResults))) {
                outDir.mkdir();
            }
            
            FileWriter  fwAnalyze_detail = new FileWriter(outDirResults + "results.xls",false);
            BufferedWriter output_Analyze = new BufferedWriter(fwAnalyze_detail);

            // create OME-XML metadata store of the latest schema version
            ServiceFactory factory;
            factory = new ServiceFactory();
            OMEXMLService service = factory.getInstance(OMEXMLService.class);
            IMetadata meta = service.createOMEXMLMetadata();
            ImageProcessorReader reader = new ImageProcessorReader();
            reader.setMetadataStore(meta);
            int series = 0;
            int imageNum = 0;
            List<String> channels = new ArrayList<>();
            ArrayList<String> ch = new ArrayList<>();
            for (String f : imageFiles) {
                String rootName = FilenameUtils.getBaseName(f);
                reader.setId(f);
                reader.setSeries(series);
                String roiFile = imageDir + File.separator + rootName + ".zip";  
                // find cell rois
                if (!new File(roiFile).exists()) {
                        IJ.showStatus("No roi file found !") ;
                        return;
                }
                else {
                    if (imageNum == 0) {
                        cal = findImageCalib(meta);
                        channels = findChannels(f);
                        ch = dialog(channels);
                        if (ch == null) {
                            IJ.showStatus("Plugin cancelled !!!");
                            return;
                        }
                        // write headers
                        output_Analyze.write("Image Name\tRoi\tGene Vol\tCells Integrated intensity in gene ref. channel\tMean background intensity in ref. channel\t"
                            + "Total dots gene ref. (based on cells intensity)/µm3\tDots ref. volume (pixel3)\tIntegrated intensity of dots ref. channel\t"
                            + "Total dots gene ref (based on dots seg intensity)/µm3\tCells Integrated intensity in gene X channel\tMean background intensity in X channel\t"
                            + "Total dots gene X (based on cells intensity)/µm3\tDots X volume (pixel3)\tIntegrated intensity of dots X channel\tTotal dots gene X (based on dots seg intensity)/µm3\n");
                        output_Analyze.flush();
                    }
                    imageNum++;
                    
                    reader.setSeries(0);
                    ImporterOptions options = new ImporterOptions();
                    options.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
                    options.setId(f);
                    options.setSplitChannels(true);  
                    options.setQuiet(true);
                    options.setCrop(true);

                    RoiManager rm = new RoiManager(false);
                    rm.runCommand("Open", roiFile);

                    ImagePlus imgGeneRef = new ImagePlus(), imgGeneX = new ImagePlus();
                    Objects3DPopulation geneRefPop = new Objects3DPopulation();
                    Objects3DPopulation geneXPop = new Objects3DPopulation();
                        
                    // find background rois
                    ArrayList<Roi> roiBg = findRoi(rm, "bg");
                    if (roiBg.size() == 0) {
                        System.out.println("No background roi found !!!!");
                        return;
                    }
                    double geneRefBgInt = 0, geneXBgInt = 0;
                    int channel = 0;
                    for (Roi roi : roiBg) {
                        String roiName = roi.getName();
                        Rectangle rect = roi.getBounds();
                        Region reg = new Region(rect.x, rect.y, rect.width, rect.height);
                        options.setCropRegion(0, reg);
                        if (roiName.contains("ref"))  {
                            // Open Gene reference channel
                            System.out.println("Opening Ref gene channel for background...");
                            imgGeneRef = BF.openImagePlus(options)[channels.indexOf(ch.get(1))];
                            geneRefBgInt = find_background(imgGeneRef, 1, imgGeneRef.getNSlices());
                            System.out.println("Background reference gene channel "+geneRefBgInt);
                            imgGeneRef.close();
                        }
                        else {
                            // Open Gene X channel
                            System.out.println("Opening X gene channel for background...");
                            if (channels.size() == 3)
                                channel = channels.indexOf(ch.get(2));
                            else
                                channel = channels.indexOf(ch.get(1));
                            imgGeneX = BF.openImagePlus(options)[channel];
                            geneXBgInt = find_background(imgGeneX, 1, imgGeneX.getNSlices());
                            System.out.println("Background X gene channel "+geneXBgInt);
                            imgGeneX.close();
                        }
                    }
                    // read others rois no background
                    for (int r = 0; r < rm.getCount(); r++) {
                        Roi roi = rm.getRoi(r);
                        String roiName = roi.getName();
                        if (!roiName.contains("bg")) {
                            Rectangle rect = roi.getBounds();
                            Region reg = new Region(rect.x, rect.y, rect.width, rect.height);
                            options.setCropRegion(0, reg);
                            double geneRefInt = 0, geneXInt = 0;
                            double geneRefIntCor = 0, geneXIntCor = 0;
                            // for gene Ref
                            if (channels.size() == 3) {
                                imgGeneRef = BF.openImagePlus(options)[channels.indexOf(ch.get(1))];
                                geneRefInt = find_Integrated(imgGeneRef, roi);
                                geneRefPop = findGenePop(imgGeneRef, roi, thMet);
                            }
                            // for gene X
                            if (channels.size() == 3)
                                channel = channels.indexOf(ch.get(2));
                            else
                                channel = channels.indexOf(ch.get(1));
                            imgGeneX = BF.openImagePlus(options)[channel];
                            geneXInt = find_Integrated(imgGeneX, roi);
                            double geneVol = find_Volume(imgGeneX, roi);
                            geneXPop = findGenePop(imgGeneX, roi, thMet);

                            // corrected value    
                            geneXIntCor = geneXInt - geneXBgInt*geneVol;
                            geneRefIntCor = geneRefInt - geneRefBgInt*geneVol;
                            // save dots image
                            saveDotsImage(imgGeneX, geneRefPop, geneXPop, outDirResults, rootName+"_"+roiName);
                            
                            double geneRefDotsInt = 0;
                            double geneRefDotsVol =0;
                            // find dots integrated intensity
                            if (channels.size() == 3) {
                                for (int n = 0; n < geneRefPop.getNbObjects(); n++) {
                                    Object3D dotobj = geneRefPop.getObject(n);
                                    geneRefDotsInt += dotobj.getIntegratedDensity(ImageHandler.wrap(imgGeneRef));
                                    geneRefDotsVol += dotobj.getVolumePixels();
                                }
                            }
                            double geneRefDotsIntCor = geneRefDotsInt - geneRefBgInt*geneRefDotsVol;
                            double geneXDotsInt = 0;
                            double geneXDotsVol = 0;
                            for (int n = 0; n < geneXPop.getNbObjects(); n++) {
                                Object3D dotobj = geneXPop.getObject(n);
                                geneXDotsInt += dotobj.getIntegratedDensity(ImageHandler.wrap(imgGeneX));
                                geneXDotsVol += dotobj.getVolumePixels();
                            }
                            double geneXDotsIntCor = geneXDotsInt - geneXBgInt*geneXDotsVol;
                            double geneRefDotsVol_micron = geneRefDotsVol*cal.pixelWidth*cal.pixelHeight*cal.pixelDepth;
                            double geneXDotsVol_micron = geneXDotsVol*cal.pixelWidth*cal.pixelHeight*cal.pixelDepth;
                            output_Analyze.write(rootName+"\t"+roiName+"\t"+geneVol+"\t"+geneRefInt+"\t"+geneRefBgInt+"\t"+(geneRefIntCor/singleDotIntGeneRef)/geneVol+"\t"+geneRefDotsVol+"\t"+geneRefDotsIntCor+"\t"+
                                    (geneRefDotsIntCor/singleDotIntGeneRef/geneRefDotsVol_micron)+"\t"+geneXInt+"\t"+geneXBgInt+"\t"+geneXDotsVol+"\t"+geneXIntCor+"\t"+
                                    (geneXIntCor/singleDotIntGeneX)/geneVol+"\t"+(geneXDotsIntCor/singleDotIntGeneX)/geneXDotsVol_micron+"\n");
                            output_Analyze.flush();
                        }                            
                    }
                    if (imgGeneRef != null)
                        closeImages(imgGeneRef);
                    closeImages(imgGeneX);
                }
            }
            output_Analyze.close();
            IJ.showStatus("Process done ...");
        } catch (DependencyException | ServiceException | FormatException | IOException ex) {
            Logger.getLogger(RNA_Scope_Roi.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

                    
                              
}
